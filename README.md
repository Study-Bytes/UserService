# UserService

UserService - микросервис пользователей и аутентификации для учебной платформы StudyBytes.

Он владеет регистрацией пользователей, входом, выдачей JWT access tokens, хранением refresh tokens, профилями пользователей и публичным JWKS endpoint для проверки токенов другими backend-сервисами.

## Технологический стек

- Java 21
- Spring Boot 3.4
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Security
- JJWT
- springdoc-openapi
- Docker Compose
- GitHub Actions

## Документы для курсовой работы

Формальные документы для сдачи курсовой находятся в отдельной директории:

- [Руководство оператора](coursework-docs/operator-manual.md)
- [Программа и методика испытаний](coursework-docs/test-program-and-methodology.md)

## Ответственность

UserService отвечает на вопрос:

```text
Кто этот пользователь и какие токены подтверждают его сессию?
```

Он хранит:

- учетные записи пользователей;
- email, хеш пароля, полное имя и роль пользователя;
- refresh tokens в виде хешей;
- состояние отзыва refresh tokens;
- настройки выпуска JWT access и refresh tokens.

Он не хранит:

- курсы и структуру учебного контента;
- записи на курсы;
- прогресс обучения;
- попытки решения задач;
- результаты выполнения кода;
- комментарии и обсуждения.

Эти данные должны принадлежать другим сервисам платформы, например CourseService, LearningService или CodeExecutorService.

## Доменная модель

```text
User
 └── RefreshToken
```

`User` представляет аккаунт пользователя платформы.

Поддерживаемые роли:

```text
STUDENT
TEACHER
ADMIN
```

`RefreshToken` хранит не исходный token, а его hash. При logout UserService отзывает refresh tokens пользователя. Access tokens остаются действительными до истечения короткого срока жизни.

## Публичная Auth API

Базовый путь:

```text
/api/v1/auth
```

Endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/.well-known/jwks.json
```

`POST /api/v1/auth/register` регистрирует нового пользователя. По умолчанию новый пользователь получает роль `STUDENT`.

Пример запроса:

```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "fullName": "Иван Иванов"
}
```

`POST /api/v1/auth/login` проверяет email и пароль, отзывает предыдущие refresh tokens пользователя и возвращает новую пару токенов.

Пример ответа:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

`POST /api/v1/auth/refresh` принимает refresh token и выдает новый access token, если refresh token валиден, не истек, не отозван и найден в базе.

`POST /api/v1/auth/logout` принимает refresh token и отзывает refresh tokens пользователя. Это разлогинивает пользователя на устройствах, где требуется refresh.

`GET /api/v1/auth/.well-known/jwks.json` возвращает публичный JWKS для проверки JWT другими сервисами.

## User API

Базовый путь:

```text
/api/v1/users
```

Endpoints:

```text
GET /api/v1/users/me
GET /api/v1/users/{id}
```

`GET /api/v1/users/me` возвращает профиль текущего пользователя. Требуется заголовок:

```text
Authorization: Bearer <access-token>
```

`GET /api/v1/users/{id}` возвращает пользователя по ID. Endpoint доступен только пользователю с ролью `ADMIN`.

Пример ответа профиля:

```json
{
  "id": 2,
  "email": "test2@mail.com",
  "fullName": "Тест Тестов",
  "role": "STUDENT"
}
```

## Health Check

Endpoint:

```text
GET /health
```

Пример ответа:

```json
{
  "status": "UP",
  "service": "UserService",
  "timestamp": "2026-05-13T12:00:00Z"
}
```

## Модель безопасности

UserService разделяет публичные и защищенные endpoints:

```text
/health                              -> public
/swagger-ui.html, /swagger-ui/**     -> public в текущей конфигурации
/v3/api-docs/**                      -> public в текущей конфигурации
/api/v1/auth/**                      -> public
/api/v1/users/me                     -> Bearer JWT
/api/v1/users/{id}                   -> Bearer JWT + ADMIN
```

Spring Security работает в stateless-режиме:

- CSRF отключен;
- HTTP session не используется;
- каждый защищенный запрос проходит через JWT filter;
- пароль хранится через BCrypt hash;
- access token передается в `Authorization: Bearer <token>`.

## JWT и JWKS

UserService выпускает JWT tokens, подписанные алгоритмом RS256.

Access token содержит публичные claims, которые нужны другим сервисам:

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

Refresh token также является JWT, но он дополнительно хранится в базе по hash и может быть отозван.

Access token не должен содержать:

- пароль;
- refresh token;
- hash refresh token;
- приватный RSA key;
- внутренние секреты сервиса.

JWKS endpoint:

```text
GET /api/v1/auth/.well-known/jwks.json
```

Пример ответа:

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

JWKS возвращает только публичные поля RSA ключа. Приватные поля `d`, `p`, `q` и сам `JWT_PRIVATE_KEY` никогда не должны попадать в API-ответы или логи.

## Интеграция с другими сервисами

UserService является источником истины для JWT.

Другие сервисы платформы должны:

- принимать access token от клиента;
- проверять подпись через JWKS endpoint UserService;
- проверять `iss`, `aud`, `exp`;
- читать роли из claim `roles`;
- не хранить private key UserService;
- не выпускать access tokens самостоятельно.

Пример конфигурации resource server в соседнем сервисе:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://user-service:8081
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://user-service:8081/api/v1/auth/.well-known/jwks.json
spring.security.oauth2.resourceserver.jwt.audiences=study-platform
```

## Swagger/OpenAPI

Локальный Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

Локальный OpenAPI JSON:

```text
http://localhost:8081/v3/api-docs
```

Версионируемый OpenAPI контракт:

```text
openapi.yml
```

После изменения controllers, DTO или response-моделей нужно проверить, что `openapi.yml` остается актуальным.

CI дополнительно проверяет:

```bash
ruby -e "require 'yaml'; YAML.load_file('openapi.yml')"
git diff --exit-code openapi.yml
```

## Переменные окружения

| Переменная | Значение по умолчанию | Описание |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP порт UserService |
| `DB_URL` | `jdbc:postgresql://localhost:5432/user_service` | JDBC URL PostgreSQL |
| `DB_USERNAME` | `postgres` | Пользователь PostgreSQL |
| `DB_PASSWORD` | `postgres` | Пароль PostgreSQL |
| `POSTGRES_DB` | `user_service` | Имя базы PostgreSQL в Docker Compose |
| `POSTGRES_USER` | `postgres` | Пользователь PostgreSQL в Docker Compose |
| `POSTGRES_PASSWORD` | `postgres` | Пароль PostgreSQL в Docker Compose |
| `USER_BACKEND_NETWORK` | `studybytes_backend_net` | Общая Docker network для Nginx/BFF/микросервисов |
| `USER_DB_NETWORK` | `user_db_net` | Внутренняя Docker network для UserService и PostgreSQL |
| `JWT_PRIVATE_KEY` | пусто | RSA private key в PEM формате |
| `JWT_KEY_ID` | `user-service-rsa-1` | `kid` публичного ключа в JWKS |
| `JWT_ISSUER` | `study-platform-user-service` | JWT issuer |
| `JWT_AUDIENCE` | `study-platform` | JWT audience |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `15` | Время жизни access token |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` | Время жизни refresh token |
| `JPA_DDL_AUTO` | `update` | Режим Hibernate schema generation |
| `JPA_SHOW_SQL` | `false` | Печать SQL запросов |
| `HIBERNATE_FORMAT_SQL` | `false` | Форматирование SQL логов |
| `FLYWAY_ENABLED` | `false` | Включение Flyway |
| `LOG_LEVEL_ROOT` | `INFO` | Root log level |
| `LOG_LEVEL_WEB` | `INFO` | Spring Web log level |
| `LOG_LEVEL_SECURITY` | `INFO` | Spring Security log level |
| `LOG_LEVEL_USERSERVICE` | `DEBUG` | Log level пакета UserService |

## Генерация RSA Private Key

Для production и shared dev окружений нужно задать стабильный `JWT_PRIVATE_KEY`. Если ключ не задан, сервис генерирует in-memory RSA key на старте, что подходит только для локальной разработки.

Сгенерировать private key:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private-key.pem
```

Вывести key в формате для `.env`:

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-private-key.pem
```

Пример значения:

```text
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
```

Нельзя коммитить:

- `.env`;
- `*.pem`;
- `*.key`;
- приватные ключи;
- реальные пароли и токены.

## Локальный запуск

Скопировать пример окружения:

```bash
cp .env.example .env
```

Запустить PostgreSQL и UserService через Docker Compose:

```bash
docker network create studybytes_backend_net
docker compose up --build
```

UserService будет доступен:

```text
http://127.0.0.1:8081
```

PostgreSQL не публикуется наружу на host-порт. Он доступен только внутри Docker Compose:

```text
jdbc:postgresql://postgres:5432/user_service
```

Запуск приложения через Maven требует запущенный PostgreSQL:

```bash
./mvnw spring-boot:run
```

## Проверка API вручную

Health:

```bash
curl -fsS http://127.0.0.1:8081/health
```

Регистрация:

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"securePassword123","fullName":"Иван Иванов"}'
```

Логин:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"securePassword123"}'
```

Профиль:

```bash
curl http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer <access-token>"
```

JWKS:

```bash
curl http://localhost:8081/api/v1/auth/.well-known/jwks.json
```

## Тесты

Тесты используют H2 и не требуют локального PostgreSQL:

```bash
./mvnw clean test
```

Полная Maven verification фаза:

```bash
./mvnw clean verify
```

CI запускает:

```bash
./mvnw -B clean verify
```

## Docker Compose

`docker-compose.yml` запускает:

- `user-service`;
- `postgres`;
- volume `user_postgres_data`;
- external network `studybytes_backend_net`;
- internal network `user_db_net`.

UserService получает переменные из `.env`:

```yaml
env_file:
  - .env
```

Внутри Docker Compose приложение подключается к PostgreSQL по service name:

```text
jdbc:postgresql://postgres:5432/user_service
```

UserService публикуется только на localhost хоста:

```yaml
ports:
  - "127.0.0.1:${SERVER_PORT:-8081}:8081"
```

Это нужно, чтобы VPS Nginx мог безопасно проксировать запросы:

```text
https://study-byte.ru/user-service/api/v1/auth/.well-known/jwks.json
```

PostgreSQL находится только во внутренней Docker network и не открыт напрямую в интернет.

Локальный reset базы с потерей данных:

```bash
docker compose down -v
docker compose up --build
```

## Deployment

Deploy выполняется GitHub Actions workflow:

```text
.github/workflows/user-service-ci-cd.yml
```

CI запускается на:

```text
pull_request -> main
push         -> любые ветки
```

Deploy запускается только после успешного CI при:

```text
push -> main
```

Для deploy нужны GitHub Secrets:

```text
VPS_HOST
VPS_PORT
VPS_USER
VPS_SSH_KEY
VPS_DEPLOY_BASE_PATH
```

Опциональный secret:

```text
VPS_HEALTHCHECK_URL
```

Если `VPS_HEALTHCHECK_URL` не задан, workflow проверяет:

```text
http://127.0.0.1:8081/health
```

Workflow деплоит сервис в:

```text
$VPS_DEPLOY_BASE_PATH/user-service
```

При первом deploy workflow:

- создает директорию `user-service`;
- клонирует репозиторий;
- переключается на `main`;
- создает `.env` из `.env.example`, если `.env` еще нет;
- запускает `docker compose up -d --build`;
- проверяет health endpoint.

Важно: `.env.example` подходит только как стартовый шаблон. Для production нужно заменить значения в `.env` на реальные, особенно `JWT_PRIVATE_KEY`, пароли БД и сетевые настройки.

## Как проверить CI/CD

1. Создайте Pull Request в `main`.
2. Откройте вкладку `Actions` в GitHub.
3. Убедитесь, что workflow `UserService CI/CD` завершился успешно.
4. Проверьте job `Build, test, OpenAPI and Docker check`.
5. После merge в `main` проверьте job `Deploy UserService to VPS`.
6. На VPS проверьте контейнеры:

```bash
docker compose ps
```

7. Проверьте health:

```bash
curl -fsS http://127.0.0.1:8081/health
```

## Ошибки и ожидаемое поведение

`401 Unauthorized` означает, что access token отсутствует, истек или не прошел JWT validation.

`403 Forbidden` на `GET /api/v1/users/{id}` означает, что пользователь не имеет роли `ADMIN`.

`409 Conflict` при регистрации означает, что email уже занят.

`Invalid or expired refresh token` означает, что refresh token невалиден, истек, отозван или не найден в базе.

`JWT_PRIVATE_KEY` без стабильного значения приводит к генерации нового ключа при каждом старте. После рестарта ранее выпущенные tokens могут перестать проходить проверку подписи.

## Правила дальнейшей разработки

- Не добавлять в access token секретные данные.
- Не возвращать password hash через API.
- Не хранить refresh token в базе в открытом виде.
- Не коммитить `.env` и приватные ключи.
- После изменения публичного API обновлять `openapi.yml`.
- После изменения security logic запускать тесты контроллеров и security tests.
- Другие сервисы должны проверять JWT через JWKS, а не через общий secret.
