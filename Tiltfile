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
    live_update=[
        sync('./frontend', '/app'),
    ],
)

docker_compose('services/docker-compose.yml')