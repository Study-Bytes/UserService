# Руководство оператора UserService

## 1. Назначение программы

UserService является микросервисом пользователей и аутентификации учебной платформы StudyBytes.
Сервис обеспечивает регистрацию пользователей, вход в систему, выпуск JWT access tokens,
хранение refresh tokens, предоставление профиля пользователя и публикацию JWKS endpoint
для проверки токенов другими микросервисами.

## 2. Область применения

Руководство предназначено для оператора или администратора, который выполняет развертывание,
запуск, остановку, проверку состояния и первичную диагностику UserService на сервере.

## 3. Требования к окружению

| Компонент | Требование |
| --- | --- |
| Операционная система | Linux server или VPS с Docker |
| Java | Java 21 внутри Docker image |
| База данных | PostgreSQL 16 |
| Контейнеризация | Docker и Docker Compose |
| Сеть | External network `studybytes_backend_net` и internal network `user_db_net` |

## 4. Состав сервиса

В Docker Compose запускаются следующие компоненты:

- `user-service` - Spring Boot приложение.
- `postgres` - база данных PostgreSQL.
- `user_postgres_data` - volume для хранения данных PostgreSQL.
- `studybytes_backend_net` - общая сеть для Nginx, BFF и микросервисов.
- `user_db_net` - внутренняя сеть для UserService и PostgreSQL.

## 5. Переменные окружения

| Переменная | Назначение | Пример |
| --- | --- | --- |
| `SERVER_PORT` | Порт приложения внутри контейнера и на localhost хоста | `8081` |
| `POSTGRES_DB` | Имя базы данных | `user_service` |
| `POSTGRES_USER` | Пользователь PostgreSQL | `postgres` |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | сложный пароль |
| `DB_URL` | JDBC URL для подключения к БД | `jdbc:postgresql://postgres:5432/user_service` |
| `JWT_PRIVATE_KEY` | RSA private key для подписи JWT | PEM key в одну строку с `\n` |
| `JWT_KEY_ID` | Идентификатор ключа в JWKS | `user-service-rsa-1` |
| `JWT_ISSUER` | Issuer JWT | `study-platform-user-service` |
| `JWT_AUDIENCE` | Audience JWT | `study-platform` |

## 6. Подготовка RSA private key

Для стабильной работы JWT необходимо задать постоянный RSA private key.

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private-key.pem
```

Преобразовать ключ в формат для `.env`:

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-private-key.pem
```

Полученное значение записывается в переменную `JWT_PRIVATE_KEY`.
Файл ключа нельзя коммитить в репозиторий.

## 7. Первый запуск

1. Перейти в директорию проекта на сервере.
2. Создать общую Docker network, если она еще не создана:

```bash
docker network create studybytes_backend_net
```

3. Проверить наличие файла `.env` и корректность переменных.
4. Запустить сервис:

```bash
docker compose up -d --build
```

## 8. Остановка и перезапуск

Остановить сервис:

```bash
docker compose down
```

Перезапустить сервис:

```bash
docker compose up -d --build
```

Посмотреть состояние контейнеров:

```bash
docker compose ps
```

## 9. Проверка работоспособности

Проверить health endpoint:

```bash
curl -fsS http://127.0.0.1:8081/health
```

Ожидаемый ответ содержит:

```json
{
  "status": "UP",
  "service": "UserService",
  "timestamp": "..."
}
```

## 10. Проверка Swagger/OpenAPI

Swagger UI:

```text
http://127.0.0.1:8081/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://127.0.0.1:8081/v3/api-docs
```

## 11. Проверка JWKS

Локальный endpoint:

```bash
curl http://127.0.0.1:8081/api/v1/auth/.well-known/jwks.json
```

Публичный endpoint через Nginx:

```text
https://study-byte.ru/user-service/api/v1/auth/.well-known/jwks.json
```

Ответ должен содержать публичные поля RSA ключа: `kty`, `use`, `kid`, `alg`, `n`, `e`.
Приватные поля `d`, `p`, `q` отсутствуют.

## 12. Просмотр логов

```bash
docker logs user-service -f
docker logs user-service-postgres -f
```

В логах не должны выводиться приватные ключи, пароли, refresh tokens и значения token hash.

## 13. Типовые ошибки и действия оператора

| Ошибка | Причина | Действие |
| --- | --- | --- |
| `Failed to load RSA JWT private key` | `JWT_PRIVATE_KEY` не является PEM RSA private key | Сгенерировать корректный RSA key и обновить `.env` |
| `Connection refused` к PostgreSQL | БД не запущена или неверный `DB_URL` | Проверить `docker compose ps` и переменные БД |
| `401 Unauthorized` | Нет access token или token истек | Выполнить login или refresh |
| `403 Forbidden` | Недостаточно роли | Проверить `roles` claim и права пользователя |
| JWKS недоступен через домен | Ошибка Nginx proxy или Docker binding | Проверить localhost endpoint и конфиг Nginx |

## 14. Резервное копирование базы данных

Пример создания dump:

```bash
docker exec user-service-postgres pg_dump -U postgres user_service > user_service_backup.sql
```

Восстановление выполняется только после остановки сервиса или в согласованное окно обслуживания.

## 15. Правила безопасности

- Не коммитить `.env`, `*.pem`, `*.key`.
- Не передавать `JWT_PRIVATE_KEY` другим микросервисам.
- Другие сервисы проверяют JWT только через публичный JWKS.
- Регулярно проверять доступность `/health` и JWKS после обновлений.
