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
- Настройки текущего пользователя.
- Admin lookup пользователя по id.
- Создание и модерация teacher requests.
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
| 5 | Регистрация пользователя | `POST /api/v1/auth/register` с `fullName`, валидным email, password и ролью `STUDENT` или `TEACHER` | Возвращены `user`, `accessToken`, `refreshToken`, `tokenType`, `expiresIn` |
| 6 | Дублирующий email | Повторить регистрацию с тем же email | HTTP 409 Conflict |
| 7 | Login | `POST /api/v1/auth/login` с корректными учетными данными | Возвращены `user`, `accessToken`, `refreshToken`, `tokenType`, `expiresIn` |
| 8 | Bad credentials | `POST /api/v1/auth/login` с неверным паролем | HTTP 401 Unauthorized |
| 9 | Refresh access token | `POST /api/v1/auth/refresh` с валидным refresh token в body | Возвращен новый `accessToken` и данные `user` |
| 10 | Logout | `POST /api/v1/auth/logout` с `Authorization: Bearer <access-token>` без body | HTTP 204, refresh tokens пользователя отозваны |
| 11 | Профиль без JWT | `GET /api/v1/users/me` без Authorization header | HTTP 401 Unauthorized |
| 12 | Профиль с JWT | `GET /api/v1/users/me` с `Authorization: Bearer token` | Возвращен `CurrentUser` текущего пользователя |
| 13 | Admin endpoint без `ADMIN` | `GET /api/v1/users/{id}` пользователем `STUDENT` | HTTP 403 Forbidden |
| 14 | JWKS endpoint | `GET /api/v1/auth/.well-known/jwks.json` | HTTP 200, есть `kty`, `use`, `kid`, `alg`, `n`, `e` |
| 15 | Отсутствие private key в JWKS | Проверить JSON JWKS | Нет полей `d`, `p`, `q`, `JWT_PRIVATE_KEY` |
| 16 | Swagger UI | Открыть `/swagger-ui/index.html` | Swagger UI доступен |
| 17 | Получение настроек | `GET /api/v1/users/me/settings` с Bearer token | Возвращены `fullName`, `avatarUrl`, `bio`, `preferredLocale` |
| 18 | Обновление настроек | `PUT /api/v1/users/me/settings` с Bearer token и body `fullName`, `avatarUrl`, `bio`, `preferredLocale` | Возвращены обновленные настройки пользователя |
| 19 | Валидация locale | `PUT /api/v1/users/me/settings` с `preferredLocale = de` | HTTP 400 Bad Request |
| 20 | Создание teacher request | `POST /api/v1/teacher-requests` с Bearer token STUDENT | HTTP 201 Created, `status = PENDING` |
| 21 | Дубликат pending teacher request | Повторить `POST /api/v1/teacher-requests` для того же пользователя | HTTP 409 Conflict |
| 22 | Получение своей teacher request | `GET /api/v1/teacher-requests/me` с Bearer token | Возвращен объект заявки или `204 No Content` |
| 23 | Список заявок (ADMIN) | `GET /api/v1/admin/teacher-requests?status=PENDING&page=0&size=20` под ADMIN | Возвращена страница заявок |
| 24 | Approve teacher request (ADMIN) | `POST /api/v1/admin/teacher-requests/{id}/approve` под ADMIN | `status = APPROVED`, роль пользователя изменена на `TEACHER` |
| 25 | Reject teacher request (ADMIN) | `POST /api/v1/admin/teacher-requests/{id}/reject` с `reviewComment` | `status = REJECTED`, сохранен `reviewComment` |
| 26 | Смена пароля | `PUT /api/v1/users/me/password` с Bearer token и body `currentPassword`, `newPassword` | HTTP 204 |
| 27 | Запрет ADMIN регистрации | `POST /api/v1/auth/register` с `role = ADMIN` | HTTP 400 Bad Request |

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
