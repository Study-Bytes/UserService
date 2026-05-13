# UserService

UserService is a Spring Boot microservice for the Study Platform coursework project. It manages user registration, authentication, JWT access tokens, refresh tokens, user profile endpoints, and public JWKS data for other services.

## Main Features

- User registration with default `STUDENT` role
- Login with access and refresh tokens
- Refresh token persistence and revocation
- JWT access tokens signed with RS256
- Public JWKS endpoint for token verification by other services
- Role support: `STUDENT`, `TEACHER`, `ADMIN`
- Protected user profile endpoints
- OpenAPI and Swagger UI documentation
- Health endpoint for deployment checks
- PostgreSQL runtime database and isolated H2 test database

## Technology Stack

- Java 21
- Spring Boot 3.4
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- H2 for tests
- JJWT
- Springdoc OpenAPI
- Maven
- Docker Compose

## Runtime Ports

By default:

- UserService runs on `8089`
- PostgreSQL is exposed on host port `6766`

These values can be overridden with environment variables.

## API Documentation

After starting the application, OpenAPI is available at:

```text
http://localhost:8089/v3/api-docs
```

Swagger UI is available at:

```text
http://localhost:8089/swagger-ui/index.html
```

## Health Endpoint

The health endpoint is public and can be used by local checks, deployment scripts, or monitoring:

```text
GET http://localhost:8089/health
```

Example response:

```json
{
  "status": "UP",
  "service": "UserService",
  "timestamp": "2026-05-13T14:00:00Z"
}
```

## Auth Endpoints

Base path:

```text
/api/v1/auth
```

Available endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/.well-known/jwks.json
```

## User Endpoints

Base path:

```text
/api/v1/users
```

Available endpoints:

```text
GET /api/v1/users/me
GET /api/v1/users/{id}
```

`GET /api/v1/users/{id}` is restricted to users with the `ADMIN` role.

## JWT Contract

UserService issues access JWT tokens signed with the RS256 algorithm. The private RSA key stays only inside UserService. Other services must verify access tokens with the public key from JWKS.

Access token claims:

```json
{
  "iss": "study-platform-user-service",
  "sub": "123",
  "aud": ["study-platform"],
  "iat": 1730000000,
  "exp": 1730000900,
  "jti": "uuid-token-id",
  "email": "user@example.com",
  "username": "user",
  "roles": ["STUDENT"]
}
```

The access token must not contain passwords, refresh token values, token hashes, or other sensitive data.

## JWKS

JWKS endpoint:

```text
GET http://localhost:8089/api/v1/auth/.well-known/jwks.json
```

The response exposes only public RSA key fields:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "user-service-rsa-1",
      "alg": "RS256",
      "n": "public-modulus",
      "e": "AQAB"
    }
  ]
}
```

Private key fields such as `d`, `p`, and `q` must never be returned.

## RSA Private Key Generation

Generate a private RSA key for local development:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private-key.pem
```

Print the key as a single environment variable value:

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-private-key.pem
```

Example `.env` value:

```text
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

Do not commit `.env`, `*.pem`, or `*.key` files.

## Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8089` | HTTP port for UserService |
| `DB_URL` | `jdbc:postgresql://localhost:6766/user_service` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `POSTGRES_PORT` | `6766` | PostgreSQL host port in Docker Compose |
| `POSTGRES_DB` | `user_service` | PostgreSQL database name |
| `POSTGRES_USER` | `postgres` | PostgreSQL Docker user |
| `POSTGRES_PASSWORD` | `postgres` | PostgreSQL Docker password |
| `JWT_PRIVATE_KEY` | empty | RSA private key in PEM format |
| `JWT_KEY_ID` | `user-service-rsa-1` | Public key id exposed in JWKS |
| `JWT_ISSUER` | `study-platform-user-service` | JWT issuer |
| `JWT_AUDIENCE` | `study-platform` | JWT audience |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `15` | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | Refresh token lifetime |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `JPA_SHOW_SQL` | `false` | SQL logging flag |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |

## Local Run With Maven

Start PostgreSQL first, then run:

```bash
mvn spring-boot:run
```

The service will be available at:

```text
http://localhost:8089
```

## Local Run With Docker Compose

Start PostgreSQL and UserService:

```bash
docker compose up --build
```

PostgreSQL will be exposed on:

```text
localhost:6766
```

UserService will be exposed on:

```text
localhost:8089
```

## Run Tests

Tests use an isolated H2 database and do not require local PostgreSQL:

```bash
mvn clean test
```

## Coursework Notes

This service demonstrates a microservice authentication model:

- only UserService owns the RSA private key
- access tokens are signed with RS256
- other services verify tokens through JWKS
- refresh tokens are stored as SHA-256 hashes
- protected endpoints use stateless Spring Security
- OpenAPI documents the HTTP API contract
