FROM openresty/openresty:1.27.1.2-0-alpine-fat

RUN apk add --no-cache gettext \
    && opm get fffonion/lua-resty-openssl \
    && mkdir -p /var/log/account/account-gateway \
    && mkdir -p /usr/local/openresty/nginx/lua

COPY nginx.conf.template /usr/local/openresty/nginx/conf/nginx.conf.template
COPY nginx/conf/trusted-proxies.conf /usr/local/openresty/nginx/conf/trusted-proxies.conf
COPY lua /usr/local/openresty/nginx/lua
COPY entrypoint.sh /usr/local/bin/account-gateway-entrypoint

RUN addgroup -S nginx 2>/dev/null || true && \
    adduser -S -D -H -h /var/cache/nginx -s /sbin/nologin -G nginx nginx 2>/dev/null || true && \
    chmod +x /usr/local/bin/account-gateway-entrypoint && \
    mkdir -p /usr/local/openresty/nginx/client_body_temp && \
    chown -R nginx:nginx /var/log/account /usr/local/openresty/nginx/conf /usr/local/openresty/nginx/client_body_temp

USER nginx

EXPOSE 8089

ENTRYPOINT ["account-gateway-entrypoint"]
