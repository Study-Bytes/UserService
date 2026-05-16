# Программа и методика испытаний UserService

## 1. Объект испытаний

Объектом испытаний является микросервис UserService учебной платформы StudyBytes.
Сервис реализован на Java 21 и Spring Boot 3.4, использует PostgreSQL для хранения данных,
Spring Security для защиты API и RS256 JWT для аутентификации пользователей.

## 2. Цель испытаний

Цель испытаний - подтвердить, что UserService:

- собирается и запускается без ошибок;
- корректно выполняет регистрацию, login, refresh и logout;
- защищает пользовательские endpoints через JWT;
- публикует JWKS без приватных данных;
- работает в Docker Compose окружении;
- имеет доступную документацию OpenAPI и health endpoint.

## 3. Требования к тестовому окружению

| Компонент | Требование |
| --- | --- |
| JDK | Java 21 |
| Build tool | Maven Wrapper или Maven 3.9+ |
| Unit/Integration tests | H2 in-memory database |
| Runtime test | Docker Compose и PostgreSQL 16 |
| HTTP checks | `curl` или аналогичный HTTP client |

## 4. Состав проверяемых функций

- Регистрация пользователя.
- Проверка уникальности email.
- Кодирование пароля.
- Выдача access и refresh tokens.
- Хранение refresh token hash.
- Обновление access token через refresh token.
- Отзыв refresh tokens при logout.
- Проверка JWT и ролей.
- Профиль текущего пользователя.
- Admin lookup пользователя по id.
- JWKS endpoint.
- Health endpoint.
- Swagger/OpenAPI endpoint.

## 5. Методика проведения испытаний

Испытания выполняются в следующем порядке:

1. Проверка сборки проекта.
2. Запуск автоматических тестов.
3. Проверка Docker Compose конфигурации.
4. Запуск сервиса в контейнерах.
5. Ручная проверка ключевых HTTP endpoints.
6. Проверка JWT/JWKS контракта.
7. Фиксация результатов.

## 6. Команды испытаний

Автоматические тесты:

```bash
./mvnw clean test
```

Полная verification фаза:

```bash
./mvnw clean verify
```

Проверка Docker Compose config:

```bash
docker compose config
```

Запуск сервиса:

```bash
docker network create studybytes_backend_net
docker compose up -d --build
```

## 7. Критерии успешности

- Проект компилируется без ошибок.
- Все автоматические тесты проходят успешно.
- Контейнеры `user-service` и `user-service-postgres` находятся в состоянии `running`/`healthy`.
- Health endpoint возвращает HTTP 200.
- JWKS endpoint возвращает публичный RSA key.
- Protected endpoints не доступны без JWT.
- Admin endpoint не доступен пользователю без роли `ADMIN`.

## 8. Таблица тест-кейсов

| № | Проверка | Шаги | Ожидаемый результат |
| --- | --- | --- | --- |
| 1 | Сборка проекта | Выполнить `./mvnw clean test` | `BUILD SUCCESS`, все тесты прошли |
| 2 | Docker Compose config | Выполнить `docker compose config` | Compose файл валиден |
| 3 | Запуск сервиса | Выполнить `docker compose up -d --build` | Контейнеры запущены, PostgreSQL healthy |
| 4 | Health endpoint | `GET /health` | HTTP 200, `status = UP` |
| 5 | Регистрация пользователя | `POST /api/v1/auth/register` с валидным email и password | Пользователь создан, возвращено сообщение об успехе |
| 6 | Дублирующий email | Повторить регистрацию с тем же email | HTTP 409 Conflict |
| 7 | Login | `POST /api/v1/auth/login` с корректными учетными данными | Возвращены `accessToken`, `refreshToken`, `tokenType`, `expiresIn` |
| 8 | Bad credentials | `POST /api/v1/auth/login` с неверным паролем | HTTP 401 Unauthorized |
| 9 | Refresh access token | `POST /api/v1/auth/refresh` с валидным refresh token | Возвращен новый `accessToken` |
| 10 | Logout | `POST /api/v1/auth/logout` с refresh token | Refresh tokens пользователя отозваны |
| 11 | Профиль без JWT | `GET /api/v1/users/me` без Authorization header | HTTP 401 Unauthorized |
| 12 | Профиль с JWT | `GET /api/v1/users/me` с `Authorization: Bearer token` | Возвращен `UserDto` текущего пользователя |
| 13 | Admin endpoint без `ADMIN` | `GET /api/v1/users/{id}` пользователем `STUDENT` | HTTP 403 Forbidden |
| 14 | JWKS endpoint | `GET /api/v1/auth/.well-known/jwks.json` | HTTP 200, есть `kty`, `use`, `kid`, `alg`, `n`, `e` |
| 15 | Отсутствие private key в JWKS | Проверить JSON JWKS | Нет полей `d`, `p`, `q`, `JWT_PRIVATE_KEY` |
| 16 | Swagger UI | Открыть `/swagger-ui/index.html` | Swagger UI доступен |

## 9. Проверка JWT/JWKS

При успешном login access token подписывается private RSA key алгоритмом RS256.
Другие сервисы получают публичный ключ через JWKS endpoint и проверяют подпись локально.

Проверяются следующие claims:

- `iss = study-platform-user-service`;
- `aud` содержит `study-platform`;
- `sub` содержит id пользователя;
- `exp` больше текущего времени;
- `roles` содержит роль пользователя.

## 10. Проверка безопасности

- Пароль пользователя не возвращается в API.
- Refresh token не хранится в базе в открытом виде.
- JWT private key не доступен через API.
- PostgreSQL не открыт наружу через host port.
- UserService опубликован только на `127.0.0.1` для reverse proxy.

## 11. Итоговое заключение

Испытания считаются успешными, если все автоматические тесты проходят,
сервис запускается в Docker Compose окружении, health endpoint отвечает HTTP 200,
а ключевые auth/user/JWKS сценарии соответствуют ожидаемым результатам.
