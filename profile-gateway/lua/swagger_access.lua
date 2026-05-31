local enabled = (os.getenv("ACCOUNT_SWAGGER_UI_ENABLED") or ""):lower()
if enabled ~= "true" then
    return ngx.exit(ngx.HTTP_NOT_FOUND)
end
