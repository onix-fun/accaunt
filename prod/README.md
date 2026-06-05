# Account production boundary

Mount the RS256 private and public keys under `prod/secrets/`. The private key is
mounted only into `profile`; the public key is mounted into both `profile` and
`account-gateway`.

External ingress terminates TLS and routes `account.<base-domain>` to port `8089`.
Configure `ACCOUNT_TRUSTED_PROXY_CIDRS` with only ingress CIDRs. Enable HSTS after
HTTPS works for every trusted subdomain.

The bundled account OTel collector reads account-gateway and profile logs, accepts
profile OTLP telemetry, and forwards it to `CENTRAL_OTEL_EXPORTER_OTLP_ENDPOINT`.
