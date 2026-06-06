FROM nginx:alpine
# Copy custom Nginx configuration if needed, or use default and just copy static files
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
