# API

Base URL: `https://<host>/api/v1`. All authenticated endpoints accept
`Authorization: Bearer <Firebase ID Token>`.

## Auth / Me
- `POST /auth/sync` — upsert user from verified Firebase token
- `GET  /me`        — current principal

## Profile
- `GET /profile` — full profile + summary
- `PUT /profile` — update height / weight / age / sex / experience / goals

## Programs
- `GET  /programs`               — list
- `GET  /programs/{id}`          — detail
- `POST /programs/{id}/assign`   — assign to current user
- `POST /programs/calendar/{id}/reschedule` / `/skip`

## Workouts
- `POST   /workouts/sessions`                 — start
- `POST   /workouts/sessions/{id}/sets`       — append sets
- `POST   /workouts/sessions/{id}/finish`     — finish
- `GET    /workouts/sessions/{id}`            — detail
- `GET    /workouts/history?from=&to=&limit=`
- `DELETE /workouts/sessions/{id}`

## Achievements
- `GET    /achievements`
- `POST   /achievements`
- `DELETE /achievements/{id}`

## Nutrition
- `GET    /nutrition/today`
- `POST   /nutrition/entries`
- `DELETE /nutrition/entries/{id}`
- `PUT    /nutrition/goals`
- `GET    /nutrition/tips`

## Errors
`{ "code": "string", "message": "string" }` with HTTP 400 / 401 / 403 / 404 / 409 / 422 / 500.
