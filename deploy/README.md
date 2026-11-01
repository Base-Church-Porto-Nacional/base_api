# Deploy (Docker Swarm)

A API roda em `https://api.veigagustavo.com.br`, atrás do Traefik (rede `traefik-public`), junto com um Postgres na rede interna da stack.

```bash
cp deploy/.env.example deploy/.env   # preencha DB_PASSWORD, JWT_SECRET e ADMIN_*
set -a; . deploy/.env; set +a
docker stack deploy --with-registry-auth -c deploy/stack.yml base-api
```

O CORS libera só o Flutter Web (`https://base.veigagustavo.com.br`). Os apps Android/iOS não mandam `Origin` e não passam pelo CORS (coberto por `CorsConfigTest`); basta gerá-los com
`--dart-define=API_URL=https://api.veigagustavo.com.br/api`.

A imagem é publicada no GHCR pelo workflow `.github/workflows/docker.yml`.
O guia completo (DNS, Traefik, front) está em `base_interface/deploy/README.md`.
