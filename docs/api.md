# API

Base URL: `https://<host>/api/v1`.

Все эндпоинты под `/api/v1` требуют заголовок
`Authorization: Bearer <Firebase ID Token>`. Пользователь заводится в БД
автоматически при первом валидном запросе (`getOrCreate` по `firebase_uid`),
отдельного эндпоинта регистрации нет.

Вне `/api/v1`: `GET /health` → `{"status":"ok"}` (без авторизации).

## Me
| Метод | Путь | Ответ |
|---|---|---|
| GET | `/me` | `MeResponse` — `userId`, `firebaseUid`, `email`, `displayName` |

## Profile
| Метод | Путь | Тело / ответ |
|---|---|---|
| GET | `/profile` | `ProfileSummaryResponse` (профиль + статистика на сегодня) |
| PUT | `/profile` | `UpdateProfileRequest` → обновлённый профиль |

## Nutrition
| Метод | Путь | Тело / ответ |
|---|---|---|
| GET | `/nutrition/today?date=YYYY-MM-DD` | `NutritionDayResponse` — итоги, цели, записи |
| PUT | `/nutrition/goals` | `NutritionGoalsDto` → обновлённые цели |
| POST | `/nutrition/entries` | `CreateNutritionEntryRequest` → созданная запись |
| DELETE | `/nutrition/entries/{id}` | 204 |

## Programs
| Метод | Путь | Тело / ответ |
|---|---|---|
| POST | `/programs/generate` | `GenerateProgramRequest` → активная программа. Rate limit: 5/мин |
| GET | `/programs/active` | активная программа или 404 |
| GET | `/calendar?from=YYYY-MM-DD&to=YYYY-MM-DD` | дни календаря со статусами |
| POST | `/programs/workouts/{id}/reschedule` | `{ "newDate": "YYYY-MM-DD" }` |
| POST | `/programs/workouts/{id}/skip` | 204 |

## Workouts
| Метод | Путь | Тело / ответ |
|---|---|---|
| POST | `/workouts/sessions/start` | опросник восстановления → сессия + рекомендация |
| POST | `/workouts/sessions/{id}/sets` | список подходов (вес, повторы, RPE) |
| POST | `/workouts/sessions/{id}/finish` | оценка 1..5 + длительность |
| GET | `/workouts/sessions/{id}` | детали сессии |
| DELETE | `/workouts/sessions/{id}` | 204 |
| GET | `/workouts/history?limit=&cursor=` | список прошедших сессий (курсорная пагинация) |

## Achievements
| Метод | Путь | Тело / ответ |
|---|---|---|
| GET | `/achievements` | список |
| POST | `/achievements` | `CreateAchievementRequest` → созданная запись |
| DELETE | `/achievements/{id}` | 204 |

## Формат ошибок

Все ошибки — `ApiErrorResponse` (`dto/Dto.kt`):

```json
{ "error": "unauthorized", "details": null }
```

| HTTP | `error` | Когда |
|---|---|---|
| 400 | `bad_request` | невалидное тело/параметры (`IllegalArgumentException`) |
| 401 | `unauthorized` | нет заголовка `Authorization`, не `Bearer`, или токен не прошёл проверку Firebase |
| 403 | `email_not_verified` | включён `REQUIRE_EMAIL_VERIFIED`, а почта не подтверждена |
| 404 | `not_found` | объект не найден **или** принадлежит другому пользователю (намеренно неразличимо) |
| 413 | `payload_too_large` | тело больше 64 KiB |
| 429 | — | превышен rate limit (120 запросов/мин на пользователя) |
| 500 | `internal_error` | необработанная ошибка; `details` непусто только при `APP_ENV=development` |
