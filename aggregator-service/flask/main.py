import logging.config
import os
import sys

import requests
from autologging import traced, logged
from flask import Flask, request
from flask import jsonify, make_response
from flask import url_for
from flask_consulate import Consul as Consulate
from flask_jwt_extended import JWTManager, jwt_required
from flask_prometheus_metrics import register_metrics
from flask_restx import Api
from flask_restx import fields, Resource
from flask_zipkin import Zipkin
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware
from werkzeug.serving import run_simple

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


class CustomApi(Api):
    @property
    def specs_url(self):
        """
        The Swagger specifications absolute url (ie. `swagger.json`)

        :rtype: str
        """
        return url_for(self.endpoint('specs'), _external=False)


authorizations = {
    'apikey': {
        'type': 'apiKey',
        'in': 'header',
        'name': 'Authorization'
    }
}


def initialize_api(app):
    return CustomApi(app=app, catch_all_404s=True, version='1.0', title='API - Products Service',
                     description='Products Management', doc='/swagger-ui.html',
                     default_label='products endpoints', default='products',
                     authorizations=authorizations, security='apikey')


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
@ns.route('/totalCategory')
class DashboardCategoryApi(Resource):
    """Return list of categories"""

    @jwt_required
    @ns.doc(description='List of categories',
            params={'categoryName': 'Category Name', 'personId': 'Person Id', 'plannedEndDate': 'Planned Date',
                    'done': 'Todo done?'},
            responses={
                400: 'Validation Error',
                401: 'Unauthorized',
                403: 'Forbidden',
                500: 'Unexpected Error'
            })
    @api.response(200, 'Success', [categoryModel])
    def get(self):
        token = request.headers.get('Authorization')
        category_name = request.args.get('categoryName')
        person_id = request.args.get('personId')
        planned_end_date = request.args.get('plannedEndDate')
        done = request.args.get('done')
        log.debug('Token: %s', token)
        url = app.config['TODO_URL']
        query_param = '?'
        if category_name is not None:
            query_param += '&categoryName=' + category_name
        if person_id is not None:
            query_param += '&personId=' + person_id
        if planned_end_date is not None:
            query_param += '&plannedEndDate=' + planned_end_date
        if done is not None:
            query_param += '&done=' + done

        if query_param != '?':
            url += query_param
        r = requests.get(url, headers={'Content-Type': 'application/json',
                                                       'Authorization': token})
        json = r.json()
        if r.status_code == 200:
            array = []
            for key in json:
                array.append({'category': key, 'total': len(json[key])})
            json = jsonify(array)

        return make_response(json, r.status_code)


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
    app_name = app.config['APP_NAME']
    # Consul
    # This extension should be the first one if enabled:
    consul = Consulate(app=app)
    # Fetch the conviguration:
    consul.apply_remote_config(namespace='config/'+app_name+'/data')
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

    app.config['JWT_PUBLIC_KEY'] = open(app.config['JWT_PUBLIC_KEY'], "r").read()

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")


if __name__ == "__main__":
    run_simple(hostname="0.0.0.0", port=server_port, application=initialize_dispatcher(app), use_debugger=debug_flag)
