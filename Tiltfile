config.define_bool("cdc", args=True)
config.define_bool("sms_inbox")
cfg = config.parse()
cdc = cfg.get("cdc")
sms_inbox = cfg.get("sms_inbox")

if cdc:
    docker_compose(['infra/docker-compose.yml', 'infra/docker-compose.cdc.yml'])
else:
    docker_compose('infra/docker-compose.yml')

docker_build(
    'timeeconomy/auth-service',
    context='./backend/auth-service',
    dockerfile='./backend/auth-service/Dockerfile',
    live_update=[
        sync('./backend/auth-service/src', '/app/src'),
        run('cd /app && ./gradlew classes --no-daemon',
            trigger='./backend/auth-service/src'),
    ],
)

docker_build(
    'timeeconomy/user-service',
    context='./backend/user-service',
    dockerfile='./backend/user-service/Dockerfile',
    live_update=[
        sync('./backend/user-service/src', '/app/src'),
        run('cd /app && ./gradlew classes --no-daemon',
            trigger='./backend/user-service/src'),
    ],
)

docker_build(
    'timeeconomy/notification-service',
    context='./backend/notification-service',
    dockerfile='./backend/notification-service/Dockerfile',
    live_update=[
        sync('./backend/notification-service/src', '/app/src'),
        run('cd /app && ./gradlew classes --no-daemon',
            trigger='./backend/notification-service/src'),
    ],
)

docker_build(
    'timeeconomy/gateway-service',
    context='./backend/gateway-service',
    dockerfile='./backend/gateway-service/Dockerfile',
    live_update=[
        sync('./backend/gateway-service/src', '/app/src'),
        run('cd /app && ./gradlew classes --no-daemon',
            trigger='./backend/gateway-service/src'),
    ],
)

docker_build(
    'timeeconomy/frontend',
    context='./frontend',
    dockerfile='./frontend/Dockerfile',
    live_update=[sync('./frontend', '/app')],
)

if sms_inbox:
    docker_compose([
        'services/docker-compose.yml',
        'services/docker-compose.sms-inbox.yml',
    ])
else:
    docker_compose('services/docker-compose.yml')

dc_resource('auth-db')
dc_resource('user-db')
dc_resource('notification-db')
dc_resource('redis')
dc_resource('redisinsight', resource_deps=['redis'])


dc_resource('kafka')
dc_resource('schema-registry', resource_deps=['kafka'])

if cdc:
    dc_resource('connect', resource_deps=[
                'kafka', 'schema-registry', 'auth-db'])
    dc_resource('connector-init', resource_deps=['connect', 'schema-registry'])

dc_resource('auth-service',
            resource_deps=['auth-db', 'redis', 'kafka', 'schema-registry'])
dc_resource('user-service', resource_deps=['user-db', 'kafka', 'auth-service'])
dc_resource('notification-service', resource_deps=['kafka'])

if cdc:
    dc_resource(
        'kafka-ui', resource_deps=['kafka', 'connect', 'schema-registry'])
else:
    dc_resource('kafka-ui', resource_deps=['kafka', 'schema-registry'])

dc_resource('gateway-service', resource_deps=['auth-service', 'user-service'])
dc_resource('frontend', resource_deps=['gateway-service'])
