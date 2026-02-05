package com.powerlifting.server

import com.powerlifting.server.auth.FirebaseTokenVerifier
import com.powerlifting.server.auth.FirebaseUserPrincipal
import com.powerlifting.server.config.AppConfig
import com.powerlifting.server.config.ConfigLoader
import com.powerlifting.server.data.cache.CaffeineCache
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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import java.time.Duration
import java.util.UUID
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

private val PrincipalKey = AttributeKey<FirebaseUserPrincipal>("firebasePrincipal")
private val UserKey = AttributeKey<User>("domainUser")

fun Application.module(config: AppConfig = ConfigLoader.loadFromEnv()) {
    install(CallLogging) {
        level = Level.INFO
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

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "bad_request", "details" to (cause.message ?: "")))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal_error", "details" to (cause.message ?: "")))
        }
    }

    if (config.corsAllowAll) {
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader("X-DEV-UID")
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
        }
    }

    DatabaseFactory.init(config.db)

    val tokenVerifier: FirebaseTokenVerifier? = if (config.devBypassAuth) {
        log.warn("DEV_BYPASS_AUTH enabled: Firebase verification is skipped")
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
                registerMeRoutes()
                registerProfileRoutes(getProfileSummary, updateProfile)
                registerNutritionRoutes(
                    getTodayNutrition,
                    updateNutritionGoals,
                    addNutritionEntry,
                    deleteNutritionEntry
                )
                registerProgramRoutes(generateProgram, getActiveProgram, getProgramCalendar, rescheduleWorkout, skipWorkout)
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

internal fun ApplicationCall.principal(): FirebaseUserPrincipal = attributes[PrincipalKey]
internal fun ApplicationCall.userRow(): User = attributes[UserKey]

private fun ApplicationCall.authenticate(
    tokenVerifier: FirebaseTokenVerifier?,
    config: AppConfig
): FirebaseUserPrincipal {
    if (config.devBypassAuth) {
        val uid = request.headers["X-DEV-UID"]?.takeIf { it.isNotBlank() } ?: "dev-user"
        return FirebaseUserPrincipal(uid = uid, email = "dev@example.com", name = "Dev")
    }

    val header = request.headers[HttpHeaders.Authorization]
        ?: throw IllegalArgumentException("Missing Authorization header")

    val parts = header.trim().split(" ", limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) {
        throw IllegalArgumentException("Invalid Authorization header (expected: Bearer <token>)")
    }

    return requireNotNull(tokenVerifier).verify(parts[1])
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
