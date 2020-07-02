#!/bin/sh

set -xeuo pipefail

if ! curl -k --retry 5 --retry-connrefused --retry-delay 5 -H "Content-Type: application/json" -sf http://consul-service-discovery:8500/v1/kv/config; then
    curl --request PUT --data-binary @config/application.yml http://consul-service-discovery:8500/v1/kv/config/application/data
    curl --request PUT --data-binary @config/zuul-server.yml http://consul-service-discovery:8500/v1/kv/config/zuul-server/data
    curl --request PUT --data-binary @config/oauth-service.yml http://consul-service-discovery:8500/v1/kv/config/oauth-service/data
fi
