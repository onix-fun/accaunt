FROM openresty/openresty:1.27.1.2-0-alpine-fat

RUN apk add --no-cache gettext \
    && opm get fffonion/lua-resty-openssl \
    && mkdir -p /var/log/account/account-gateway \
    && mkdir -p /usr/local/openresty/nginx/lua

COPY nginx.conf.template /usr/local/openresty/nginx/conf/nginx.conf.template
COPY lua /usr/local/openresty/nginx/lua
COPY entrypoint.sh /usr/local/bin/account-gateway-entrypoint

RUN chmod +x /usr/local/bin/account-gateway-entrypoint

EXPOSE 8089

ENTRYPOINT ["account-gateway-entrypoint"]
