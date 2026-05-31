# Account Gateway

OpenResty gateway for `account.<base-domain>`. It serves the profile SPA and proxies
identity API routes to `profile-service`. The main application gateway must not proxy
these routes.

The gateway validates RS256 access tokens using the mounted public PEM. Browser
requests use the shared parent-domain `access_token` cookie; external clients can use
`Authorization: Bearer`. Bearer auth takes priority.

Generate local keys from the account repository root:

```sh
./scripts/generate-dev-keys.sh
```

Production ingress must terminate TLS and send `X-Forwarded-For`. Configure
`ACCOUNT_TRUSTED_PROXY_CIDRS` with only ingress CIDRs. Set `ACCOUNT_HSTS_HEADER` only
after HTTPS is enabled across all trusted subdomains.
