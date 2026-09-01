# CODEMAP — карта кода Powerlifting Assistant Server

> **Назначение этого файла.** Индекс для мгновенного ответа на любой вопрос по серверу
> (включая «что делает эта строчка» + скриншот). Прочитав его, ассистент за секунды
> определяет слой фрагмента, его файл, зависимости и действующие конвенции.
> Источник истины — сам код; при расхождении верь коду, а не этому файлу.

---

## 0. Как пользоваться (рецепт «что делает эта строчка»)

1. **Определи слой по сигнатуре/импортам:**
   - `fun Route.registerXxxRoutes(...)`, `get/post/put/delete`, `call.receive`, `call.respond` → **роут** (`routes/`).
   - `class XxxUseCase(...)`, `suspend operator fun invoke(...)`, `require(...)` → **use case** (`domain/usecase/...`).
   - `class XxxRepositoryImpl : XxxRepository`, `dbQuery { ... }`, обращения к `XxxTable` → **репозиторий-реализация** (`data/repository/`).
   - `class CachedXxxRepository(delegate, cache)` → **кэширующий декоратор** (`data/repository/cached/`).
   - `object XxxTable : UUIDTable(...)` → **схема таблицы Exposed** (`db/tables/Tables.kt`).
   - `@Serializable data class …Dto / …Request / …Response` + `init { require(...) }` → **DTO + валидация** (`dto/Dto.kt`).
   - `fun XxxRequest.toDomain()` / `Xxx.toDto()/toResponse()` в `routes/mapper/` → **DTO↔домен**; в `data/repository/mapper/` → **row Exposed→домен**.
   - `class XxxService` (`domain/service/`) → **доменный сервис** (бизнес-правила без БД).
2. **Найди фичу** в §4 — получишь весь срез: роут → use case → сервис → репозиторий → таблицы.
3. **Сверь конвенцию** в §3 — почти весь код повторяет 5–6 шаблонов.
4. **Проверь нюансы** в §8 — там собрано всё неочевидное (безопасность, кэш, пагинация, генерация программы).

> **Главный файл — [Application.kt](src/main/kotlin/com/powerlifting/server/Application.kt).** Это ручной DI-композитор: install всех плагинов Ktor, создание кэшей/репозиториев/use case и регистрация роутов. Если вопрос «как всё связано» — открывай его первым.

---

## 1. Что это за проект

Бэкенд мобильного приложения для пауэрлифтеров. Ktor (Netty) REST API под `/api/v1/**`,
PostgreSQL (Neon serverless), аутентификация по Firebase ID Token (проверяет Firebase Admin SDK).
Парный клиент — Android-приложение (тот же контракт DTO и те же эндпоинты).

**Технологии:** Kotlin (JVM 21), Ktor 2.3.7 (Netty, ContentNegotiation, StatusPages, CORS,
RateLimit, CallLogging, DefaultHeaders), Exposed 0.50.1 (ORM, не DAO а DSL), HikariCP,
Flyway 11 (миграции), PostgreSQL 42.7, Firebase Admin 9.3, Caffeine 3.1 (in-process кэш),
kotlinx.serialization, Logback. Тесты — JUnit 5 + ktor-server-tests.

**Пакет:** `com.powerlifting.server`. Entry point — `MainKt` ([Main.kt](src/main/kotlin/com/powerlifting/server/Main.kt)) → `embeddedServer(Netty)` → `Application.module(config)`.

---

## 2. Структура пакетов (слои Clean Architecture)

```
com.powerlifting.server
├── Main.kt                  Точка входа: ConfigLoader + embeddedServer(Netty).
├── Application.kt           module(config): плагины Ktor, ручной DI, auth-interceptor, routing.
│                            Здесь же ApplicationCall.principal()/userRow() и routeAuth().
│
├── config/
│   └── AppConfig.kt         AppConfig/DbConfig/FirebaseConfig + ConfigLoader (env → конфиг,
│                            парсинг DATABASE_URL, server-local.properties для локалки).
│
├── auth/
│   └── FirebaseAuth.kt      FirebaseUserPrincipal + FirebaseTokenVerifier (verifyIdToken;
│                            init из service-account path или base64).
│
├── db/
│   ├── DatabaseFactory.kt   Hikari pool + Flyway-миграции (с ретраями; в prod падает при провале).
│   ├── Db.kt                dbQuery { } — newSuspendedTransaction(Dispatchers.IO).
│   └── tables/Tables.kt     Все таблицы Exposed (UUIDTable). 9 таблиц (см. §6).
│
├── domain/
│   ├── model/               Доменные модели + enum WorkoutStatus + sealed ProgramSchedule (§5).
│   ├── repository/          Интерфейсы репозиториев (6 шт).
│   ├── service/RecoveryService.kt   Правила рекомендации по восстановлению.
│   └── usecase/             Use case по доменам: profile, nutrition, program, workout, achievements.
│
├── data/
│   ├── repository/          Реализации на Exposed (паттерн §3.4).
│   │   ├── cached/          Декораторы кэша: CachedUserRepository, CachedProgramRepository.
│   │   └── mapper/          Row Exposed → доменная модель (toUser, toProgramExercise, …).
│   └── cache/
│       ├── Cache.kt         Интерфейс-порт (get/put/invalidate/invalidateAll).
│       └── CaffeineCache.kt Реализация на Caffeine.
│
├── dto/Dto.kt               Все DTO/Request/Response (@Serializable) + валидация в init {}.
│
└── routes/                  Ktor-роуты (register*Routes). По одному файлу на домен.
    └── mapper/              DTO ↔ домен на уровне роутов (toDomain/toDto/toResponse, курсор).

src/main/resources/db/migration/   Flyway: V1__init, V2__add_wellbeing_rating, V3__schedule_and_status.
```

---

## 3. Конвенции и идиомы (знать их = понимать любой файл)

### 3.1 Поток запроса (golden path)
```
HTTP → [intercept: размер тела ≤ 64 KiB] → [routeAuth interceptor: authenticate() →
   principal в attributes → userRepository.getOrCreate() → User в attributes (UserKey)]
→ rateLimit(perUser|expensive) → register*Routes:
   call.userRow()  → req = call.receive<XxxRequest>()  → useCase(user.id, req.toDomain())
   → result.toDto()/toResponse() → call.respond(status, body)
```
- Текущий пользователь достаётся в роуте через `call.userRow(): User` (а Firebase-данные — `call.principal()`).
- Ошибки **не** обрабатываются в роутах — их ловит `StatusPages` (§3.5). Роуты бросают исключения.

### 3.2 Аутентификация ([Application.kt:265](src/main/kotlin/com/powerlifting/server/Application.kt))
- `routeAuth` ставит `intercept(ApplicationCallPipeline.Plugins)` на всю `/api/v1`.
- `authenticate()`: либо **dev-bypass** (`X-DEV-UID`, только при `APP_ENV=development`), либо `Bearer <token>` → `FirebaseTokenVerifier.verify()`.
- При `requireEmailVerified=true` непроверенный email → `EmailNotVerifiedException` (403).
- `userRepository.getOrCreate(firebaseUid, email, displayName)` создаёт строку `users` при первом заходе.

### 3.3 Use case — тонкий слой с валидацией
- Класс с конструктором-внедрением репозиториев/сервисов и `suspend operator fun invoke(userId: UUID, ...)`.
- Бизнес-инварианты через `require(...) { "..." }` → ловится как `IllegalArgumentException` → **400**.
- Сложная логика — только в `GenerateProgramUseCase` (генерация программы) и `RescheduleWorkoutUseCase`; остальные делегируют в репозиторий.

### 3.4 Repository — Exposed DSL + проверка владельца
```kotlin
override suspend fun xxx(userId: UUID, ...) = dbQuery {
    XxxTable.select { (XxxTable.id eq id) and (XxxTable.userId eq userId) }  // всегда фильтр по userId!
        .singleOrNull() ?: throw NotFoundException("...")  // или вернуть null/false
    // insert / update / deleteWhere ...
}
```
- **Всегда** фильтрация по `userId` — изоляция данных пользователей (нельзя достать чужую сессию даже зная её id).
- `dbQuery { }` = suspend-транзакция на `Dispatchers.IO` ([Db.kt](src/main/kotlin/com/powerlifting/server/db/Db.kt)).
- `decimal`-поля БД ↔ `Double` домена: `.toBigDecimal()` при записи, маппер при чтении.
- Образцы: [WorkoutRepositoryImpl.kt](src/main/kotlin/com/powerlifting/server/data/repository/WorkoutRepositoryImpl.kt), [ProgramRepositoryImpl.kt](src/main/kotlin/com/powerlifting/server/data/repository/ProgramRepositoryImpl.kt).

### 3.5 Обработка ошибок (StatusPages, [Application.kt:110](src/main/kotlin/com/powerlifting/server/Application.kt))
| Исключение | HTTP | Тело |
|-----------|------|------|
| `NotFoundException` | 404 | `{"error":"not_found"}` (без деталей — не палит чужие ресурсы) |
| `EmailNotVerifiedException` | 403 | `{"error":"email_not_verified"}` |
| `IllegalArgumentException` | 400 | `{"error":"bad_request","details":"<msg>"}` |
| прочее `Throwable` | 500 | `{"error":"internal_error","details":null}` (детали только в dev) |

### 3.6 Два уровня мапперов
- `routes/mapper/*DtoMapper.kt` — `XxxRequest.toDomain()`, `XxxDomain.toDto()/toResponse()`; здесь же курсор пагинации (`encodeCursor`/`decodeCursor`).
- `data/repository/mapper/*Mapper.kt` — `ResultRow.toXxx()` (Exposed-строка → доменная модель).

### 3.7 Валидация — в двух местах
- **DTO `init {}`** ([Dto.kt](src/main/kotlin/com/powerlifting/server/dto/Dto.kt)): диапазоны полей (`heightCm in 50..250`, `wellbeing in 1..10`, `photoUrl` только `https://`, ≤200 сетов и т.п.). Падает при десериализации → 400.
- **Use case `require`**: дублирует ключевые проверки, чтобы инвариант держался даже в обход DTO.

### 3.8 Кэширование (Caffeine, in-process)
- Интерфейс-порт `Cache<K,V>` ([Cache.kt](src/main/kotlin/com/powerlifting/server/data/cache/Cache.kt)) → можно заменить на Redis не трогая data-слой.
- Декораторы оборачивают impl: `CachedUserRepository(UserRepositoryImpl(), userCache)` (TTL 1ч, 10k),
  `CachedProgramRepository(..., activeProgramCache)` (TTL 5мин). Любая мутация программы → `invalidate`/`invalidateAll`.
- `TrainingProgramHolder(program?)` — обёртка, чтобы кэшировать и «нет активной программы» (null).

### 3.9 Прочее
- Тексты, видимые пользователю (названия дней программы, рекомендации) — на русском.
- Даты на проводе — ISO `YYYY-MM-DD`; время — ISO-8601 Instant. В домене — `LocalDate`/`Instant`, в БД — `date`/`timestamptz`.
- Все таблицы — `UUIDTable` (PK типа UUID, генерится Postgres `gen_random_uuid()`).
- `ProgramSchedule` сериализуется в БД простой строкой (`"weekdays:1,3,5"` / `"dates:2026-05-01,..."`) через `encode()`/`decode()` — без JSON-зависимости в data-слое.

---

## 4. Индекс по фичам (вертикальные срезы)

| Фича | Роут-файл | Use case(ы) | Сервис | Репозиторий | Эндпоинты |
|------|-----------|-------------|--------|-------------|-----------|
| **Профиль** | `ProfileRoutes` | GetProfileSummary, UpdateProfile | — | Profile | `GET/PUT /profile` |
| **Текущий юзер** | `MeRoutes` | — (из `userRow()`) | — | (User, в interceptor) | `GET /me` |
| **Питание** | `NutritionRoutes` | GetTodayNutrition, UpdateNutritionGoals, AddNutritionEntry, DeleteNutritionEntry | — | Nutrition (+ Profile для целей) | `GET /nutrition/today`, `PUT /nutrition/goals`, `POST/DELETE /nutrition/entries` |
| **Программа** | `ProgramRoutes` | GenerateProgram, GetActiveProgram, GetProgramCalendar, RescheduleWorkout, SkipWorkout | — | Program (+ Profile для 1ПМ) | `POST /programs/generate` (rate: expensive), `GET /programs/active`, `GET /calendar`, `POST /programs/workouts/{id}/reschedule`, `POST /programs/workouts/{id}/skip` |
| **Тренировка** | `WorkoutRoutes` | StartWorkoutSession, AddWorkoutSets, FinishWorkoutSession, GetWorkoutSessionDetail, GetWorkoutHistory, DeleteWorkoutSession | RecoveryService (в Start) | Workout (+ Program в Finish) | `POST /workouts/sessions/start`, `POST /workouts/sessions/{id}/sets`, `POST /workouts/sessions/{id}/finish`, `GET/DELETE /workouts/sessions/{id}`, `GET /workouts/history` |
| **Достижения** | `AchievementRoutes` | ListAchievements, CreateAchievement, DeleteAchievement | — | Achievements | `GET/POST /achievements`, `DELETE /achievements/{id}` |
| **Health** | (в Application.kt) | — | — | — | `GET /health` (вне `/api/v1`, без auth) |

---

## 5. Доменный словарь (модели)

**User** ([model/User.kt]): `User(id: UUID, firebaseUid, email?, displayName?, createdAt)`, `UserProfile(heightCm?, weightKg?, bench1rm?, squat1rm?, deadlift1rm?)`, `NutritionGoals(caloriesGoal, proteinGoalG)`, `UserStats`, `ProfileSummary`, `ProfileUpdate`.

**Program** ([model/Program.kt](src/main/kotlin/com/powerlifting/server/domain/model/Program.kt)):
`enum WorkoutStatus(wire)` = PLANNED/COMPLETED/MISSED/RESCHEDULED + `parse()`;
`sealed ProgramSchedule` = `Weekdays(Set<DayOfWeek>)` | `Dates(List<LocalDate>)` + `encode()`/`decode()`;
`TrainingProgram`, `ProgramExercise(percent1rm?, liftType)`, `ProgramWorkout(status, originalWorkoutId?)`, `ActiveProgram`, `CalendarDay(workoutId: UUID)`, `TrainingCalendar`, `GenerateProgramSpec(startDate?, weeks?, schedule?)`, `NewProgramExercise`.

**Workout** ([model/Workout.kt](src/main/kotlin/com/powerlifting/server/domain/model/Workout.kt)):
`WorkoutSet(...)` (с `require` в init), `RecoveryInputs(sleepHours?, wellbeing?, fatigue?, soreness?)` + `isEmpty`, `StartSessionInput(programWorkoutId?, recovery)`, `WorkoutSessionStart`, `WorkoutSessionDetail`, `WorkoutHistoryItem(startedAt: Instant, …)`, `WorkoutHistoryPage(items, nextCursor: Instant?)`, `FinishSessionResult(programWorkoutId?)`.

**Прочее:** `Achievement`, `Nutrition*` (NutritionEntry/Totals/Day).

> DTO в `dto/Dto.kt` зеркалят домен, но: даты/UUID как `String`, добавлены `init`-валидаторы, `WorkoutHistoryResponse.nextCursor` — base64url-курсор. `CalendarDayDto.workoutId` — **не-nullable** на сервере (на клиенте nullable ради старых ответов).

---

## 6. Схема БД (PostgreSQL, миграции Flyway)

Таблицы ([Tables.kt](src/main/kotlin/com/powerlifting/server/db/tables/Tables.kt), DDL — `resources/db/migration/`):

| Таблица | Ключевые колонки | Связи / заметки |
|---------|------------------|-----------------|
| `users` | `firebase_uid` UNIQUE, email?, display_name? | корень; всё каскадно от него |
| `user_profile` | PK=user_id→users, height/weight/bench/squat/deadlift (NUMERIC) | 1:1 с user |
| `nutrition_goals` | PK=user_id→users, calories_goal=2500, protein_goal_g=150 | дефолты в DDL |
| `nutrition_entries` | user_id, eaten_at, title, calories, protein_g | idx(user_id, eaten_at) |
| `training_programs` | user_id, template_code, start_date, weeks, is_active, **schedule_json** | idx(user_id, is_active); schedule_json (V3) |
| `program_workouts` | program_id, workout_date, title, status='planned', **original_workout_id**(self-FK) | статусы + self-FK (V3) |
| `program_exercises` | program_workout_id, exercise_name, order_index, sets, reps(TEXT), percent_1rm?, lift_type | reps хранится строкой («5» или «8-10») |
| `workout_sessions` | user_id, program_workout_id?(SET NULL), started_at, finished_at?, duration?, sleep/wellbeing/fatigue/soreness/recommendation, **wellbeing_rating**(V2) | idx(user_id, started_at) |
| `workout_sets` | session_id, exercise_name, set_number, weight_kg, reps, rpe? | idx(session_id) |
| `achievements` | user_id, created_at, note, photo_url? | idx(user_id, created_at) |

Миграции: **V1** — вся базовая схема; **V2** — `wellbeing_rating` в сессии; **V3** — `schedule_json` + `original_workout_id` + индексы статуса.

---

## 7. Размеры файлов (где много логики)

- `GenerateProgramUseCase.kt` — 205 стр. (**самая сложная бизнес-логика**: шаблоны A/B/C, недельные пирамиды %1ПМ, подсобка, прогрессия).
- `ProgramRepositoryImpl.kt` — 189, `WorkoutRepositoryImpl.kt` — 185 (Exposed-запросы, JOIN, групповой count, пагинация).
- `ProfileRepositoryImpl.kt` — 113, `WorkoutRoutes.kt` — 104, `ProgramRoutes.kt` — 81.
- `Application.kt` — ~313 (вся проводка). `Dto.kt` — 313 (все контракты).
- Остальные use case — 15–58 строк (тонкие).

---

## 8. Нюансы и неочевидные места (частые источники вопросов)

- **Изоляция пользователей**: каждый репозиторный запрос фильтрует по `userId`; 404 намеренно не отличает «нет ресурса» от «чужой ресурс» (см. комментарий в `Exceptions.kt`).
- **Dev-bypass auth** ([Application.kt:81](src/main/kotlin/com/powerlifting/server/Application.kt)): `DEV_BYPASS_AUTH=true` разрешён **только** при `APP_ENV=development`, иначе сервер отказывается стартовать (защита от продакшн-мисконфига). Заголовок `X-DEV-UID` задаёт фейкового юзера.
- **Rate limiting**: `perUser` = 120 запросов/мин на всё API; `expensive` = 5/мин, навешен только на `POST /programs/generate`. Ключ — доменный `user.id`.
- **Лимит тела запроса** 64 KiB — режется в `intercept` до десериализации (`MAX_BODY_BYTES`).
- **Безопасные заголовки**: `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options: DENY`. CORS выключен по умолчанию (мобильный клиент его не требует), включается `CORS_ALLOW_ALL=true`.
- **Генерация программы** ([GenerateProgramUseCase.kt](src/main/kotlin/com/powerlifting/server/domain/usecase/program/GenerateProgramUseCase.kt)): требует заполненных bench/squat/deadlift 1ПМ (иначе 400). Расписание → список дат (`Weekdays`×weeks, либо явные `Dates`, либо дефолт Пн/Ср/Пт). Шаблоны **A/B/C** чередуются `idx % 3`; неделя прогрессии `idx/3` (макс индекс 3). Веса заданы как `percent1rm` (клиент сам умножает на 1ПМ). weeks ограничены 1..12, дефолт 4. Перед созданием — `deactivatePrograms(userId)` (активна одна программа).
- **Рекомендация по восстановлению** ([RecoveryService.kt](src/main/kotlin/com/powerlifting/server/domain/service/RecoveryService.kt)): детерминированные правила. Сон<4ч или самочувствие≤3 → «перенести»; сон<6ч или усталость≥8 или болезненность≥8 → «снизить нагрузку»; иначе «по плану». Пустой ввод → `null`.
- **Завершение тренировки** ([FinishWorkoutSessionUseCase.kt](src/main/kotlin/com/powerlifting/server/domain/usecase/workout/FinishWorkoutSessionUseCase.kt)): если сессия привязана к `program_workout`, он помечается `COMPLETED` (и инвалидируется кэш активной программы). `wellbeingRating` валиден 1..5.
- **Подходы**: метод репозитория `replaceSets` (DELETE+INSERT) — отправка сетов идемпотентно заменяет прежние. `AddWorkoutSets` ограничен 200 сетами (DTO init).
- **История — keyset-пагинация** ([WorkoutRepositoryImpl.kt:132](src/main/kotlin/com/powerlifting/server/data/repository/WorkoutRepositoryImpl.kt)): сортировка по `started_at DESC`, курсор = `Instant` последнего элемента (base64url через `routes/mapper`), только завершённые сессии (`finishedAt NOT NULL`). `setsCount` берётся одним groupBy-count, не N запросами. Достижения пока на `offset/limit` (есть TODO мигрировать на курсор).
- **БД-старт** ([DatabaseFactory.kt](src/main/kotlin/com/powerlifting/server/db/DatabaseFactory.kt)): Hikari pool (≤5, `REPEATABLE_READ`, autoCommit off). Flyway мигрирует с 5 ретраями (Neon serverless «просыпается»); в prod при неудаче — **fail-fast** (отказ старта), в dev — продолжает без миграций.
- **Конфиг** ([AppConfig.kt](src/main/kotlin/com/powerlifting/server/config/AppConfig.kt)): сначала env, потом `server-local.properties`. `DATABASE_URL` принимается в форматах `postgresql://user:pass@host/db`, `postgres://...`, `jdbc:postgresql://...` (для jdbc нужны отдельно `DB_USER`/`DB_PASSWORD`). Firebase — `FIREBASE_SERVICE_ACCOUNT_PATH` или `_BASE64` (path в приоритете).
- **`/health`** — единственный публичный роут (вне auth и вне `/api/v1`), исключён из CallLogging.

---

## 9. Файлы-якоря (куда смотреть первым делом)

| Вопрос про… | Открыть |
|-------------|---------|
| Как всё связано / DI / плагины / pipeline | `Application.kt` |
| Контракт API (DTO, валидация) | `dto/Dto.kt` |
| Список эндпоинтов | `routes/*.kt` (по доменам) |
| Аутентификацию / токен / dev-bypass | `Application.kt` (`authenticate`/`routeAuth`), `auth/FirebaseAuth.kt` |
| Схему БД | `db/tables/Tables.kt`, `resources/db/migration/*.sql` |
| Подключение БД / миграции / пул | `db/DatabaseFactory.kt`, `db/Db.kt` |
| Бизнес-логику программы | `domain/usecase/program/GenerateProgramUseCase.kt` |
| Логику восстановления | `domain/service/RecoveryService.kt` |
| Кэш / инвалидацию | `data/repository/cached/*.kt`, `data/cache/*.kt` |
| Exposed-запросы / пагинацию | `data/repository/WorkoutRepositoryImpl.kt`, `ProgramRepositoryImpl.kt` |
| Конфиг / переменные окружения | `config/AppConfig.kt`, `README.md`, `DEPLOYMENT.md` |
| Деплой / Docker | `Dockerfile`, `docker-compose.yml`, `DEPLOYMENT.md` |

---

> **Парность с клиентом.** DTO здесь (`dto/Dto.kt`) и DTO в Android (`data/api/ApiModels.kt`) — один контракт. Эндпоинты из §4/§6 совпадают с `PowerliftingApi.kt` клиента. Расхождение между этими двумя — почти всегда баг совместимости.
