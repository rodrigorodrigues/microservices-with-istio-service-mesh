import logging.config
import os
import sys

import py_eureka_client.eureka_client as eureka_client
import requests
from autologging import traced, logged
from core.api_setup import initialize_api
from flask import Flask, Response
from flask import jsonify, make_response
from flask_jwt_extended import JWTManager, get_jwt_identity, jwt_required
from flask_restx import fields, Resource
from flask_zipkin import Zipkin
from werkzeug.serving import run_simple
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware
from flask_consulate import Consul
from flask_prometheus_metrics import register_metrics


app = Flask(__name__)
app.config.from_envvar('ENV_FILE_LOCATION')
app.debug = app.config['DEBUG']

for v in os.environ:
    env = os.getenv(v)
    if v == 'SERVER_PORT':
        env = int(env)
    app.config[v] = env

jwt = JWTManager(app)

log = logging.getLogger(__name__)

logging.basicConfig(
    format="%(levelname)s [%(name)s %(funcName)s] %(message)s",
    level=app.config['LOG_LEVEL'],
    stream=sys.stdout
)

api = initialize_api(app)

ns = api.namespace('api/dashboards', description='Dashboard operations')

categoryModel = api.model('DashboardTotalByCategory', {
    'category': fields.String(required=True, description='Category Name'),
    'total': fields.Integer(required=True, description='Total')
})

todoModel = api.model('Todo', {
    'name': fields.String(required=True, description='Name'),
    'createdDate': fields.DateTime(required=True, description='Created Date'),
    'plannedEndDate': fields.DateTime(required=True, description='Planned Date'),
    'done': fields.Boolean(required=True, description='Done?')
})

@traced(log)
@logged(log)
@ns.route('')
class DashboardApi(Resource):
    """Return list of categories"""

    @jwt_required
    @ns.doc(description='List of categories',
        params={'categoryName': 'Category Name', 'personId': 'Person Id', 'plannedDate': 'Planned Date', 'done': 'Todo done?'},
        responses={
        400: 'Validation Error',
        401: 'Unauthorized',
        403: 'Forbidden',
        500: 'Unexpected Error'
    })
    @api.response(200, 'Success', [categoryModel])
    def get(self, category_name=None, person_id=None, planned_date=None, done=None):
        token = get_jwt_identity()
        log.debug('Token: %s', token)
        res = eureka_client.do_service("PERSON-SERVICE", app.config['TODO_URL'])
        log.debug('res: %s', res)
        r = requests.get(app.config['TODO_URL'], headers={'Content-Type': 'application/json', 'Authorization': token})
        return Response(r.text, status=r.status_code, headers=r.headers)

@app.errorhandler(Exception)
def handle_root_exception(error):
    """Return a custom message and 400 or 500 status code"""
    log.exception(error)
    if hasattr(error, 'errors'):
        return make_response(jsonify(error=str(error.errors)), 400)
    return make_response(jsonify(error=str(error)), 500)


@app.route('/actuator/health')
def health():
    return jsonify({'status': 'OK'})


server_port = app.config['SERVER_PORT']


@app.route('/actuator/info')
def actuator_info():
    return jsonify({})


@app.route('/actuator')
def actuator_index():
    port = server_port
    actuator = {
        "_links": {
            "self": {
                "href": "http://localhost:" + str(port) + "/actuator",
                "templated": False
            },
            "health": {
                "href": "http://localhost:" + str(port) + "/actuator/health",
                "templated": False
            },
            "info": {
                "href": "http://localhost:" + str(port) + "/actuator/info",
                "templated": False
            },
            "prometheus": {
                "href": "http://localhost:" + str(port) + "/actuator/prometheus",
                "templated": False
            },
            "metrics": {
                "href": "http://localhost:" + str(port) + "/actuator/metrics",
                "templated": False
            }
        }
    }
    return jsonify(actuator)


zipkin = Zipkin(sample_rate=int(app.config['ZIPKIN_RATIO']))
zipkin.init_app(app)


api.add_namespace(ns)
debug_flag = app.config['DEBUG']

def initialize_dispatcher(app):
	initialize_consul(app)

	# Plug metrics WSGI app to your main app with dispatcher
	return DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})


def initialize_consul(app):
	server_port = app.config['SERVER_PORT']

	app_name = app.config['APP_NAME']
	# Consul
	# This extension should be the first one if enabled:
	consul = Consul(app=app)
	# Fetch the conviguration:
	consul.apply_remote_config(namespace='mynamespace/')
	# Register Consul service:
	consul.register_service(
		name=app_name,
		interval='10s',
		tags=['webserver', ],
		port=server_port,
		httpcheck='http://localhost:' + str(server_port) + '/actuator/health'
	)

	public_key_location = app.config['JWT_PUBLIC_KEY']

	log.debug('public_key_location: %s', public_key_location)

	log.debug('Config environment: %s', app.config)

	# provide app's version and deploy environment/config name to set a gauge metric
	register_metrics(app, app_version="v0.1.2", app_config="staging")


if __name__ == "__main__":
    run_simple(hostname="0.0.0.0", port=server_port, application=initialize_dispatcher(app), use_debugger=debug_flag)
