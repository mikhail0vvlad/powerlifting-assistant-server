package com.powerlifting.server

import com.powerlifting.server.auth.FirebaseTokenVerifier
import com.powerlifting.server.auth.FirebaseUserPrincipal
import com.powerlifting.server.config.AppConfig
import com.powerlifting.server.config.ConfigLoader
import com.powerlifting.server.data.cache.CaffeineCache
import com.powerlifting.server.domain.error.EmailNotVerifiedException
import com.powerlifting.server.domain.error.NotFoundException
import com.powerlifting.server.domain.error.UnauthorizedException
import com.powerlifting.server.dto.ApiErrorResponse
import com.powerlifting.server.data.repository.AchievementsRepositoryImpl
import com.powerlifting.server.data.repository.NutritionRepositoryImpl
import com.powerlifting.server.data.repository.ProfileRepositoryImpl
import com.powerlifting.server.data.repository.ProgramRepositoryImpl
import com.powerlifting.server.data.repository.UserRepositoryImpl
import com.powerlifting.server.data.repository.WorkoutRepositoryImpl
import com.powerlifting.server.data.repository.cached.CachedProgramRepository
import com.powerlifting.server.data.repository.cached.CachedUserRepository
import com.powerlifting.server.data.repository.cached.TrainingProgramHolder
import com.powerlifting.server.db.DatabaseFactory
import com.powerlifting.server.domain.model.User
import com.powerlifting.server.domain.repository.ProgramRepository
import com.powerlifting.server.domain.repository.UserRepository
import com.powerlifting.server.domain.service.RecoveryService
import com.powerlifting.server.domain.usecase.achievements.CreateAchievementUseCase
import com.powerlifting.server.domain.usecase.achievements.DeleteAchievementUseCase
import com.powerlifting.server.domain.usecase.achievements.ListAchievementsUseCase
import com.powerlifting.server.domain.usecase.nutrition.AddNutritionEntryUseCase
import com.powerlifting.server.domain.usecase.nutrition.DeleteNutritionEntryUseCase
import com.powerlifting.server.domain.usecase.nutrition.GetTodayNutritionUseCase
import com.powerlifting.server.domain.usecase.nutrition.UpdateNutritionGoalsUseCase
import com.powerlifting.server.domain.usecase.profile.GetProfileSummaryUseCase
import com.powerlifting.server.domain.usecase.profile.UpdateProfileUseCase
import com.powerlifting.server.domain.usecase.program.GenerateProgramUseCase
import com.powerlifting.server.domain.usecase.program.GetActiveProgramUseCase
import com.powerlifting.server.domain.usecase.program.GetProgramCalendarUseCase
import com.powerlifting.server.domain.usecase.program.RescheduleWorkoutUseCase
import com.powerlifting.server.domain.usecase.program.SkipWorkoutUseCase
import com.powerlifting.server.domain.usecase.workout.AddWorkoutSetsUseCase
import com.powerlifting.server.domain.usecase.workout.DeleteWorkoutSessionUseCase
import com.powerlifting.server.domain.usecase.workout.FinishWorkoutSessionUseCase
import com.powerlifting.server.domain.usecase.workout.GetWorkoutHistoryUseCase
import com.powerlifting.server.domain.usecase.workout.GetWorkoutSessionDetailUseCase
import com.powerlifting.server.domain.usecase.workout.StartWorkoutSessionUseCase
import com.powerlifting.server.routes.registerAchievementRoutes
import com.powerlifting.server.routes.registerMeRoutes
import com.powerlifting.server.routes.registerNutritionRoutes
import com.powerlifting.server.routes.registerProfileRoutes
import com.powerlifting.server.routes.registerProgramRoutes
import com.powerlifting.server.routes.registerWorkoutRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import java.time.Duration
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

private val PrincipalKey = AttributeKey<FirebaseUserPrincipal>("firebasePrincipal")
private val UserKey = AttributeKey<User>("domainUser")

// Per-request body size cap. JSON requests in this app are small — 64 KiB leaves
// plenty of headroom for the bulkiest AddWorkoutSets payloads while killing
// 1 MB+ junk bodies before the JSON parser tries to allocate them.
private const val MAX_BODY_BYTES = 64L * 1024L

fun Application.module(config: AppConfig = ConfigLoader.loadFromEnv()) {
    // Refuse to start in production with the dev auth bypass enabled. The bypass
    // accepts any user via the X-DEV-UID header without Firebase verification,
    // so a misconfigured prod env would let anyone impersonate any account.
    if (config.devBypassAuth && config.appEnv != "development") {
        error(
            "DEV_BYPASS_AUTH=true is only allowed when APP_ENV=development (current APP_ENV='${config.appEnv}'). " +
                "Set APP_ENV=development for local dev, or DEV_BYPASS_AUTH=false everywhere else."
        )
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/health") }
    }

    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("Referrer-Policy", "no-referrer")
        header("X-Frame-Options", "DENY")
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        )
    }

    installErrorHandling(config.appEnv)

    // Reject oversized bodies BEFORE ContentNegotiation tries to deserialize them.
    intercept(ApplicationCallPipeline.Plugins) {
        val len = call.request.contentLength() ?: 0L
        if (len > MAX_BODY_BYTES) {
            call.respond(HttpStatusCode.PayloadTooLarge, ApiErrorResponse("payload_too_large"))
            finish()
        }
    }

    if (config.corsAllowAll) {
        // Opt-in only. Mobile-only API does not need CORS at all; this branch
        // exists for the occasional browser-based tooling during development.
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            if (config.devBypassAuth) allowHeader("X-DEV-UID")
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
        }
    }

    // Per-user rate limit on every authenticated API call. The key is the
    // domain user id set by the auth interceptor below; falls back to the
    // remote host on the rare paths that bypass auth (none today, but defensive).
    install(RateLimit) {
        register(RateLimitName("perUser")) {
            rateLimiter(limit = 120, refillPeriod = 1.minutes)
            requestKey { call ->
                call.attributes.getOrNull(UserKey)?.id?.toString() ?: call.request.local.remoteHost
            }
        }
        register(RateLimitName("expensive")) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call ->
                call.attributes.getOrNull(UserKey)?.id?.toString() ?: call.request.local.remoteHost
            }
        }
    }

    DatabaseFactory.init(config.db, config.appEnv)

    val tokenVerifier: FirebaseTokenVerifier? = if (config.devBypassAuth) {
        log.warn("DEV_BYPASS_AUTH enabled (APP_ENV=${config.appEnv}): Firebase verification is skipped")
        null
    } else {
        FirebaseTokenVerifier.init(config.firebase)
        FirebaseTokenVerifier.createVerifier()
    }

    // Caches (in-process Caffeine, no Redis needed for single-instance VPS deployment)
    val userCache = CaffeineCache<String, User>(maxSize = 10_000, ttl = Duration.ofHours(1))
    val activeProgramCache = CaffeineCache<UUID, TrainingProgramHolder>(
        maxSize = 10_000, ttl = Duration.ofMinutes(5)
    )

    // Data layer (concrete impls wrapped with caching decorators where appropriate)
    val userRepository: UserRepository = CachedUserRepository(UserRepositoryImpl(), userCache)
    val profileRepository = ProfileRepositoryImpl()
    val nutritionRepository = NutritionRepositoryImpl()
    val programRepository: ProgramRepository = CachedProgramRepository(ProgramRepositoryImpl(), activeProgramCache)
    val workoutRepository = WorkoutRepositoryImpl()
    val achievementsRepository = AchievementsRepositoryImpl()

    // Domain services
    val recoveryService = RecoveryService()

    // Use cases
    val getProfileSummary = GetProfileSummaryUseCase(profileRepository)
    val updateProfile = UpdateProfileUseCase(profileRepository)

    val getTodayNutrition = GetTodayNutritionUseCase(nutritionRepository, profileRepository)
    val updateNutritionGoals = UpdateNutritionGoalsUseCase(profileRepository)
    val addNutritionEntry = AddNutritionEntryUseCase(nutritionRepository)
    val deleteNutritionEntry = DeleteNutritionEntryUseCase(nutritionRepository)

    val generateProgram = GenerateProgramUseCase(profileRepository, programRepository)
    val getActiveProgram = GetActiveProgramUseCase(programRepository)
    val getProgramCalendar = GetProgramCalendarUseCase(programRepository)
    val rescheduleWorkout = RescheduleWorkoutUseCase(programRepository)
    val skipWorkout = SkipWorkoutUseCase(programRepository)

    val startWorkoutSession = StartWorkoutSessionUseCase(workoutRepository, recoveryService)
    val addWorkoutSets = AddWorkoutSetsUseCase(workoutRepository)
    val finishWorkoutSession = FinishWorkoutSessionUseCase(workoutRepository, programRepository)
    val getWorkoutSessionDetail = GetWorkoutSessionDetailUseCase(workoutRepository)
    val getWorkoutHistory = GetWorkoutHistoryUseCase(workoutRepository)
    val deleteWorkoutSession = DeleteWorkoutSessionUseCase(workoutRepository)

    val listAchievements = ListAchievementsUseCase(achievementsRepository)
    val createAchievement = CreateAchievementUseCase(achievementsRepository)
    val deleteAchievement = DeleteAchievementUseCase(achievementsRepository)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/api/v1") {
            routeAuth(tokenVerifier, config, userRepository) {
                rateLimit(RateLimitName("perUser")) {
                    registerMeRoutes()
                    registerProfileRoutes(getProfileSummary, updateProfile)
                    registerNutritionRoutes(
                        getTodayNutrition,
                        updateNutritionGoals,
                        addNutritionEntry,
                        deleteNutritionEntry
                    )
                    registerProgramRoutes(
                        generateProgram, getActiveProgram, getProgramCalendar, rescheduleWorkout, skipWorkout
                    )
                    registerWorkoutRoutes(
                        startWorkoutSession,
                        addWorkoutSets,
                        finishWorkoutSession,
                        getWorkoutSessionDetail,
                        getWorkoutHistory,
                        deleteWorkoutSession
                    )
                    registerAchievementRoutes(listAchievements, createAchievement, deleteAchievement)
                }
            }
        }
    }
}

fun Application.installErrorHandling(appEnv: String) {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            // Причина — только в лог: клиенту незачем знать, отсутствовал заголовок
            // или Firebase отверг подпись.
            call.application.log.debug("Unauthorized: {}", cause.message)
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse(error = "unauthorized"))
        }
        exception<NotFoundException> { call, cause ->
            // Use a generic body — do NOT confirm whether the resource doesn't
            // exist at all vs. belongs to another user. cause.message is for
            // logging only.
            call.application.log.debug("Not found: {}", cause.message)
            call.respond(HttpStatusCode.NotFound, ApiErrorResponse(error = "not_found"))
        }
        exception<EmailNotVerifiedException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden, ApiErrorResponse(error = "email_not_verified"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("bad_request", cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            // Do NOT echo cause.message in production — it leaks internal state
            // (SQL errors, stack messages, JDBC paths) to clients.
            val details = if (appEnv == "development") cause.message else null
            call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse("internal_error", details))
        }
    }
}

internal fun ApplicationCall.principal(): FirebaseUserPrincipal = attributes[PrincipalKey]
internal fun ApplicationCall.userRow(): User = attributes[UserKey]

private fun ApplicationCall.authenticate(
    tokenVerifier: FirebaseTokenVerifier?,
    config: AppConfig
): FirebaseUserPrincipal {
    if (config.devBypassAuth) {
        val uid = request.headers["X-DEV-UID"]?.takeIf { it.isNotBlank() } ?: "dev-user"
        // emailVerified=true here so the dev bypass remains usable end-to-end.
        return FirebaseUserPrincipal(
            uid = uid, email = "dev@example.com", name = "Dev", emailVerified = true
        )
    }

    val header = request.headers[HttpHeaders.Authorization]
        ?: throw UnauthorizedException("Missing Authorization header")

    val parts = header.trim().split(" ", limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) {
        throw UnauthorizedException("Invalid Authorization header (expected: Bearer <token>)")
    }

    val principal = try {
        requireNotNull(tokenVerifier).verify(parts[1])
    } catch (e: Exception) {
        // FirebaseAuthException (истёкший/подделанный токен) — это 401, а не 500.
        throw UnauthorizedException("Token verification failed: ${e.message}")
    }
    if (config.requireEmailVerified && !principal.emailVerified) {
        throw EmailNotVerifiedException()
    }
    return principal
}

private fun Route.routeAuth(
    tokenVerifier: FirebaseTokenVerifier?,
    config: AppConfig,
    userRepository: UserRepository,
    build: Route.() -> Unit
) {
    route("/") {
        intercept(ApplicationCallPipeline.Plugins) {
            val principal = call.authenticate(tokenVerifier, config)
            call.attributes.put(PrincipalKey, principal)

            val user = userRepository.getOrCreate(
                firebaseUid = principal.uid,
                email = principal.email,
                displayName = principal.name
            )
            call.attributes.put(UserKey, user)
        }
        build()
    }
}
