# Powerlifting Assistant — Server (Ktor + PostgreSQL + Firebase Auth)

Это серверная часть мобильного приложения «Ассистент пауэрлифтера».

## Стек
- Kotlin (JVM)
- Ktor (Netty)
- PostgreSQL (Neon)
- Exposed + HikariCP
- Flyway migrations
- Firebase Admin SDK (проверка Firebase ID Token)

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
