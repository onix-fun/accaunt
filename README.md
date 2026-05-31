# Account

Sparrow account boundary monorepo:

- `profile/`: Kotlin identity and profile backend
- `profile-frontend/`: Vue account UI
- `profile-gateway/`: OpenResty public gateway for `account.<base-domain>`

Generate local RS256 keys and start the standalone account stack:

```sh
./scripts/generate-dev-keys.sh
docker compose up --build
```

The gateway listens on `http://localhost:8089`. For Vite development run the
profile frontend separately on `http://localhost:5174`; its `/api` proxy targets
the account gateway.
