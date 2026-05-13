# UserService

UserService - это Spring Boot микросервис для учебной платформы Study Platform. Он отвечает за регистрацию пользователей, аутентификацию, выдачу JWT access tokens, работу с refresh tokens, профили пользователей и публичный JWKS endpoint для проверки токенов другими сервисами.

## Основные возможности

- Регистрация пользователя с ролью `STUDENT` по умолчанию
- Вход по email и паролю
- Выдача access и refresh токенов
- Хранение и отзыв refresh токенов
- JWT access токены с подписью RS256
- Публичный JWKS endpoint для проверки JWT другими сервисами
- Поддержка ролей `STUDENT`, `TEACHER`, `ADMIN`
- Защищенные endpoints профиля пользователя
- OpenAPI контракт
- PostgreSQL для запуска приложения
- H2 база для тестов
- Docker Compose для локального запуска

## Стек

- Java 21
- Spring Boot 3.4
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- H2 для тестов
- JJWT
- Springdoc OpenAPI
- Maven
- Docker Compose

## Порты

По умолчанию:

- UserService запускается на `8081`
- PostgreSQL доступен на host-порту `5432`

Значения можно изменить через переменные окружения.

## Документация API

OpenAPI контракт хранится в файле:

```text
openapi.yml
```

После запуска приложения Swagger UI доступен по адресу:

```text
http://localhost:8081/swagger-ui/index.html
```

OpenAPI JSON от Springdoc доступен по адресу:

```text
http://localhost:8081/v3/api-docs
```

## Health Check

Health endpoint описан в OpenAPI для проверки доступности сервиса:

```text
GET http://127.0.0.1:8081/health
```

## Auth Endpoints

Базовый путь:

```text
/api/v1/auth
```

Доступные endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/.well-known/jwks.json
```

## User Endpoints

Базовый путь:

```text
/api/v1/users
```

Доступные endpoints:

```text
GET /api/v1/users/me
GET /api/v1/users/{id}
```

`GET /api/v1/users/{id}` доступен только пользователям с ролью `ADMIN`.

## JWT Контракт

UserService выпускает access JWT tokens, подписанные алгоритмом RS256. Приватный RSA ключ должен храниться только внутри UserService. Другие сервисы проверяют access tokens через публичный ключ из JWKS.

Пример claims в access token:

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

Access token не должен содержать пароль, refresh token, token hash или другие секретные данные.

## JWKS

JWKS endpoint:

```text
GET http://localhost:8081/api/v1/auth/.well-known/jwks.json
```

Ответ содержит только публичные поля RSA ключа:

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

Приватные поля ключа, например `d`, `p`, `q`, не должны возвращаться наружу.

## Генерация RSA Private Key

Сгенерировать приватный RSA ключ для локальной разработки:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private-key.pem
```

Вывести ключ в формате, удобном для `.env`:

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-private-key.pem
```

Пример значения в `.env`:

```text
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

Нельзя коммитить `.env`, `*.pem`, `*.key` и другие локальные секреты.

## Переменные окружения

| Переменная | Значение по умолчанию | Описание |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP порт UserService |
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_service` | JDBC URL PostgreSQL |
| `DB_USERNAME` | `postgres` | Имя пользователя PostgreSQL |
| `DB_PASSWORD` | `postgres` | Пароль PostgreSQL |
| `POSTGRES_PORT` | `5432` | Host-порт PostgreSQL в Docker Compose |
| `POSTGRES_DB` | `user_service` | Имя базы данных PostgreSQL |
| `POSTGRES_USER` | `postgres` | Пользователь PostgreSQL в Docker |
| `POSTGRES_PASSWORD` | `postgres` | Пароль PostgreSQL в Docker |
| `JWT_PRIVATE_KEY` | пусто | RSA private key в PEM формате |
| `JWT_KEY_ID` | `user-service-rsa-1` | ID публичного ключа в JWKS |
| `JWT_ISSUER` | `study-platform-user-service` | Issuer JWT |
| `JWT_AUDIENCE` | `study-platform` | Audience JWT |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `15` | Время жизни access token |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | Время жизни refresh token |
| `JPA_DDL_AUTO` | `update` | Режим Hibernate schema generation |
| `JPA_SHOW_SQL` | `false` | Включение SQL логов |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |

## Локальный запуск через Maven

Сначала запустите PostgreSQL, затем выполните:

```bash
./mvnw spring-boot:run
```

Сервис будет доступен по адресу:

```text
http://localhost:8081
```

## Локальный запуск через Docker Compose

Запустить PostgreSQL и UserService:

```bash
docker compose up --build
```

PostgreSQL будет доступен на:

```text
localhost:5432
```

UserService будет доступен на:

```text
localhost:8081
```

## Тесты

Тесты используют изолированную H2 базу и не требуют локального PostgreSQL:

```bash
./mvnw clean test
```

## CI/CD

Workflow GitHub Actions находится в:

```text
.github/workflows/user-service-ci-cd.yml
```

Он запускается на `pull_request` в `main` и на `push` в `main`.

Проверки CI:

- сборка и тесты через Maven
- проверка валидности `openapi.yml`
- проверка, что OpenAPI файл не изменился во время сборки
- Docker build

Deploy на VPS запускается только при `push` в `main` после успешного CI.

Для deploy нужны GitHub Secrets:

```text
VPS_HOST
VPS_PORT
VPS_USER
VPS_SSH_KEY
VPS_DEPLOY_BASE_PATH
```

Опционально можно добавить:

```text
VPS_HEALTHCHECK_URL
```

Если `VPS_HEALTHCHECK_URL` не задан, workflow проверит:

```text
http://127.0.0.1:8081/health
```

## Как проверить CI/CD

1. Создайте Pull Request в `main`.
2. Откройте вкладку `Actions` в GitHub.
3. Убедитесь, что workflow `UserService CI/CD` завершился успешно.
4. Проверьте шаги `Run tests`, `Validate OpenAPI contract` и `Build Docker image`.
5. После merge в `main` проверьте deploy job.
6. Workflow деплоит сервис в `$VPS_DEPLOY_BASE_PATH/user-service`.
   При первом деплое он клонирует репозиторий и создает `.env` из `.env.example`, если `.env` еще нет.
7. Проверьте контейнеры командой:

```bash
docker compose ps
```

8. Проверьте доступность сервиса:

```bash
curl -fsS http://127.0.0.1:8081/health
```
