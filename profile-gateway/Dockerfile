FROM node:22-alpine AS frontend-builder

WORKDIR /src/profile-frontend
COPY profile-frontend/package*.json ./
RUN npm ci
COPY profile-frontend/ ./
RUN npm run build

FROM openresty/openresty:1.27.1.2-0-alpine-fat

RUN apk add --no-cache gettext \
    && opm get fffonion/lua-resty-openssl \
    && mkdir -p /var/log/sparrow/account-gateway \
    && mkdir -p /usr/local/openresty/nginx/lua

COPY --from=frontend-builder /src/profile-frontend/dist /usr/local/openresty/nginx/html
COPY profile-gateway/nginx.conf.template /usr/local/openresty/nginx/conf/nginx.conf.template
COPY profile-gateway/lua /usr/local/openresty/nginx/lua
COPY profile-gateway/entrypoint.sh /usr/local/bin/account-gateway-entrypoint

RUN chmod +x /usr/local/bin/account-gateway-entrypoint

EXPOSE 8089

ENTRYPOINT ["account-gateway-entrypoint"]
