FROM nginx:alpine
# Copy custom Nginx configuration if needed, or use default and just copy static files
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/run
EXPOSE 80
USER nginx
CMD ["nginx", "-g", "daemon off;"]
