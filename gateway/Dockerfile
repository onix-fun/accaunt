FROM node:22-alpine AS frontend-builder

WORKDIR /src/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM openresty/openresty:1.27.1.2-0-alpine-fat

RUN apk add --no-cache gettext \
    && opm get fffonion/lua-resty-openssl \
    && mkdir -p /var/log/account/account-gateway \
    && mkdir -p /usr/local/openresty/nginx/lua

COPY --from=frontend-builder /src/frontend/dist /usr/local/openresty/nginx/html
COPY gateway/nginx.conf.template /usr/local/openresty/nginx/conf/nginx.conf.template
COPY gateway/lua /usr/local/openresty/nginx/lua
COPY gateway/entrypoint.sh /usr/local/bin/account-gateway-entrypoint

RUN chmod +x /usr/local/bin/account-gateway-entrypoint

EXPOSE 8089

ENTRYPOINT ["account-gateway-entrypoint"]
