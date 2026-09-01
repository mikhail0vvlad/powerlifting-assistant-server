# Powerlifting Assistant — Server (Ktor + PostgreSQL + Firebase Auth)

[![server](https://github.com/mikhail0vvlad/powerlifting-assistant-server/actions/workflows/server.yml/badge.svg)](https://github.com/mikhail0vvlad/powerlifting-assistant-server/actions/workflows/server.yml)

Это серверная часть мобильного приложения «Ассистент пауэрлифтера».

## Стек
- Kotlin (JVM)
- Ktor (Netty)
- PostgreSQL (Neon)
- Exposed + HikariCP
- Flyway migrations
- Firebase Admin SDK (проверка Firebase ID Token)

> Клиент: **[powerlifting-assistant-android](https://github.com/mikhail0vvlad/powerlifting-assistant-android)** — Kotlin + Jetpack Compose.

## Быстрый старт

### 1) Переменные окружения

Обязательные:
- `DATABASE_URL` — строка подключения Neon формата `postgresql://user:pass@host/db?sslmode=require`
- `FIREBASE_SERVICE_ACCOUNT_PATH` **или** `FIREBASE_SERVICE_ACCOUNT_BASE64`
  - `PATH`: путь до json service account (Firebase Admin)
  - `BASE64`: base64 от содержимого service account json

Необязательные:
- `PORT` (по умолчанию 8080)
- `FIREBASE_PROJECT_ID` (обычно можно не задавать)

Для локальной разработки без Firebase (НЕ для защиты):
- `DEV_BYPASS_AUTH=true`
- опционально заголовок `X-DEV-UID: any` в запросах

### 2) Запуск

Проект Gradle.

Требуется **JDK 21** (проект использует `jvmToolchain(21)`).

```bash
./gradlew run
```

### 3) Проверка
- `GET /health` → `{ "status": "ok" }`

## API
Все запросы `/api/v1/**` требуют:
`Authorization: Bearer <firebase_id_token>`

Основные роуты:
- `GET /api/v1/profile`
- `PUT /api/v1/profile`
- `GET /api/v1/nutrition/today?date=YYYY-MM-DD`
- `POST /api/v1/nutrition/entries`
- `PUT /api/v1/nutrition/goals`
- `POST /api/v1/programs/generate`
- `GET /api/v1/programs/active`
- `GET /api/v1/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /api/v1/workouts/sessions/start`
- `POST /api/v1/workouts/sessions/{id}/finish`
- `GET/POST/DELETE /api/v1/achievements`

## Миграции
Flyway автоматически применяет миграции из `src/main/resources/db/migration`.

## Лицензия

MIT — см. [LICENSE](LICENSE).
