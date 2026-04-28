FROM nextcloud:28-apache

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY hooks/ /docker-entrypoint-hooks.d/
RUN find /docker-entrypoint-hooks.d -type f -name "*.sh" -exec sed -i 's/\r$//' {} \; \
  && chmod -R +x /docker-entrypoint-hooks.d

