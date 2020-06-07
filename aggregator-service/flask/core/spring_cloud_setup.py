import logging.config
import py_eureka_client.eureka_client as eureka_client

from flask_prometheus_metrics import register_metrics
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware

log = logging.getLogger(__name__)


def initialize_dispatcher(app):
    # Plug metrics WSGI app to your main app with dispatcher
    return DispatcherMiddleware(app.wsgi_app, {"/actuator/prometheus": make_wsgi_app()})


def initialize_spring_cloud_client(app):
    server_port = app.config['SERVER_PORT']

    app_name = app.config['APP_NAME']

    # The following code will register server to eureka server and also start to send heartbeat every 30 seconds
    eureka_client.init(eureka_server=app.config['EUREKA_SERVER'],
                       app_name=app_name,
                       instance_port=server_port)

    public_key_location = app.config['JWT_PUBLIC_KEY']

    log.debug('public_key_location: %s', public_key_location)

    log.debug('Config environment: %s', app.config)

    # provide app's version and deploy environment/config name to set a gauge metric
    register_metrics(app, app_version="v0.1.2", app_config="staging")
