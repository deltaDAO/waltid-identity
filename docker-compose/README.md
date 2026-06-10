# walt.id Identity Docker Environment

This directory contains the Docker Compose configuration that sets up and runs the services and applications of the
walt.id Identity Stack.
You can either run the latest release using pre-built Docker images or build your images locally.

## Prerequisites

Ensure you have the following tools installed:

- [Docker]()
- [Docker Compose]()

---

## Quick Start with Latest Release Images

If you prefer to run the services using latest release pre-built Docker images, follow these steps:

### Pull the Latest Release Images

Start by pulling the latest release Docker images for the services:

```bash
$ docker compose pull
```

This ensures that you're using the most recent release images from the Docker registry.

### Start the Services

Once the images are pulled, start the services by running:

```bash
$ docker compose up
```

*Note:* If you are facing issues with the containers, try running the following command to remove the existing
containers and then run the
above command again.
```bash
$ docker compose down
```

### Tear down the Services

```bash
$ docker compose down -v
```

*Note:*
The version of the images pulled is controlled by the `VERSION_TAG` in the `.env` file. By default, it is set to latest,
which pulls the most recent release of the Docker images.

## Building and Running Services Locally

## Prerequisites

Ensure you have the following tools installed:

- [Docker]()
- [Docker Compose]()
- [Java 21 SDK]()

### Update the VERSION_TAG

Set `VERSION_TAG` in `docker-compose/.env` to the image tag you want to run.

### Run with Local Source (docker-compose.local.yaml)

Use `docker-compose.local.yaml` to build `wallet-api` directly from the local
Kotlin source. This is required when you have local code changes that are not
yet in a published image.

```bash
# Build and start with local wallet-api
docker compose -f docker-compose-remote.yaml -f docker-compose.local.yaml --profile identity up -d

# Rebuild wallet-api after code changes
docker compose -f docker-compose-remote.yaml -f docker-compose.local.yaml build wallet-api

# Start wallet + verifier only (plus dependencies)
docker compose -f docker-compose-remote.yaml -f docker-compose.local.yaml --profile wallet-verifier up -d
```

### Build the Docker Webapp Images Locally

```bash
$ cd docker-compose
$ docker compose build
```

### Start the Services

```bash
$ docker compose up
```

### Starting services selectively

It is possible to start services selectively, including their dependencies.

#### Start the demo wallet and all dependant services

```console
$ docker compose up waltid-demo-wallet
```

will start automatically:

- caddy
- postgres
- wallet-api
- and waltid-web-wallet

#### Start services using compose profiles

`COMPOSE_PROFILES` environment variable located in the .env file allows the selection of
profiles to start the services for. The services are available with the following profiles:

- **identity** - for all waltid-identity services (includes both `services` and `apps` profiles)
- **wallet-verifier** - starts wallet-api and verifier-api (plus dependencies like postgres and caddy)
- **services** - for API services (wallet-api, issuer-api, verifier-api, vc-repo)
- **apps** - for web applications (waltid-demo-wallet, waltid-dev-wallet, web-portal)
- **valkey** - for the Valkey/Redis service (required when using valkey for session storage in wallet-api)
- **tse** - for the Hashicorp vault service, will be initialized with:
    - a transit secrets engine
    - and authentication methods
        - approle - for my-role, where role-id and secret-id will be output in the console<sup>1</sup>
        - userpass - for myuser with mypassword
        - access-token - with dev-only-token
- **opa** - for the Open Policy Agent service
- **all** - starts all services (equivalent to combining all profiles)

Profiles can be combined, e.g.:
- `COMPOSE_PROFILES=wallet-verifier` - will start wallet-api + verifier-api and required dependencies
- `COMPOSE_PROFILES=identity,tse` - will start the waltid-identity services and the vault
- `COMPOSE_PROFILES=identity,valkey` - will start the waltid-identity services with valkey for session storage
- `COMPOSE_PROFILES=all` - will start all services including vault, valkey, and opa

<sup>1</sup> - example output:

```console
vault-init            | Role ID: 66f3f095-74c9-b270-9d1f-1f842aa6bf3f
vault-init            | Secret ID: 3abf1e00-2dc1-9e77-0705-9a81a95c7c59
```

### Stop the Services

```bash
$ docker-compose down
```

### Tear down the Services

```bash
$ docker-compose down -v
```

## Port mapping

### Services

- Wallet API: [http://localhost:7001](http://localhost:7001)
- Issuer API: [http://localhost:7002](http://localhost:7002)
- Verifier API: [http://localhost:7003](http://localhost:7003)
- Valkey (Redis-compatible): `localhost:6379` (requires `--profile valkey` or `--profile all`)
- Hashicorp vault: [http://localhost:8200](http://localhost:8200)
- Open Policy Agent: [http://localhost:8181](http://localhost:8181)

### Apps

- Demo Web Wallet: [http://localhost:7101](http://localhost:7101)
- Dev Web Wallet: [http://localhost:7104](http://localhost:7104)
- Web Portal: [http://localhost:7102](http://localhost:7102)
- Credential Repo: [http://localhost:7103](http://localhost:7103)

## Configurations

- wallet API:
    - `wallet-api/config` - wallet configuration
- issuer API:
    - `issuer-api/config`
- verifier API:
    - `verifier-api/config`
- ingress:
    - `Caddyfile`

## OIDC / Hanko Setup

Authentication uses **ktor-authnz** with Hanko as the OIDC provider. See [OIDC_DEPLOYMENT_PLAN.md](../../OIDC_DEPLOYMENT_PLAN.md) for the full guide.

**Configured Hanko Cloud env vars in `.env`:**

```env
HANKO_API_URL=https://74b96159-f6f1-44dc-b39b-98380c67660f.hanko.io
HANKO_CLIENT_ID=__not_used_for_token_login__
HANKO_CLIENT_SECRET=__not_used_for_token_login__
```

**Wallet + verifier local startup (Hanko Cloud OIDC):**
```bash
docker compose --profile wallet-verifier up -d
```

**Hanko JWT Token (programmatic / API access):**

In addition to the browser-based OIDC flow, the wallet accepts Hanko session tokens posted directly — no browser redirect required. This is useful for CLI tools, backend services, or automated tests.

```bash
# Obtain a Hanko session token via the Hanko API or SDK, then:
curl -X POST http://localhost:7001/wallet-api/auth/account/jwt \
  -H "Content-Type: text/plain" \
  --data-raw "<hanko-session-jwt>"
```

The wallet verifies the RS256 signature against Hanko's JWKS (`HANKO_API_URL/.well-known/jwks.json`), checks `iss` and `exp`, and creates or reuses the wallet account keyed on the `sub` claim. See [OIDC_DEPLOYMENT_PLAN.md](../../OIDC_DEPLOYMENT_PLAN.md) for the full JWT auth reference.

[//]: # (## Environment)

[//]: # ()

[//]: # (- main:)

[//]: # (    - `.env` - stores the common environment variables, such as port numbers,)

[//]: # (      version-tag, database-engine selection, etc.)

[//]: # (- postgres:)

[//]: # (    - `postgres/postgres.env` - stores postgres specific variables, e.g. admin user, etc.)

[//]: # (    - `pgadmin.env` - stores pgAdmin specific variables, e.g. admin user, etc.)

[//]: # (- microsoft sql-server:)

[//]: # (    - `mssql/mssql.env` - stores mssql specific variables, e.g. sql-server edition, etc.)

[//]: # ()

[//]: # (Variables from `.env` are propagated automatically down to reverse proxy configurations)

[//]: # (&#40;Caddyfile&#41; and also api configurations &#40;wallet, issuer, verifier&#41;.)

## How to

[//]: # (### Select a database engine)

[//]: # ()

[//]: # (- browse `.env` file)

[//]: # (- set `DATABASE_ENGINE` to one of:)

[//]: # (    - sqlite)

[//]: # (    - postgres)

[//]: # (    - mssql)

This value will be used also by compose profile so only the required services are started.

### Update port number

- browse `.env` file
- update the desired port number

This value will be used by reverse proxy (and services configs, if any).

### Select an identity stack version

- browse `.env` file
- update `VERSION_TAG` to a specific image version (e.g. a release version)
    - if not set, `latest` tag is used

## Troubleshooting

#### Updating ports doesn't work

Make sure the ports are also updated in:

- Caddyfile
- issuer-api/config
    - issuer-service.conf
    - web.conf
- verifier-api/config
    - verifier-service.conf
    - web.conf
- wallet-api/config
    - web.conf
    - db.conf

#### Removing the DB volume

```
docker volume rm docker-compose_wallet-api-db
```

#### DB Backup / Restore

```
pg_dump -U your_user_name -h your_host -d your_db_name > backup.sql
psql -U your_user_name -h your_host -d your_db_name < backup.sql
```
