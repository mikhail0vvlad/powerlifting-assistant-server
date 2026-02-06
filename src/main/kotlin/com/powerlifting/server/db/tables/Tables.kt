package com.powerlifting.server.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : UUIDTable("users") {
    val firebaseUid = text("firebase_uid").uniqueIndex()
    val email = text("email").nullable()
    val displayName = text("display_name").nullable()
    val createdAt = timestamp("created_at")
}

/**
 * In DB schema: user_profile.user_id is both PK and FK -> users.id.
 * We model it as the table's primary id column.
 */
object UserProfileTable : UUIDTable("user_profile", "user_id") {
    val heightCm = integer("height_cm").nullable()
    val weightKg = decimal("weight_kg", precision = 5, scale = 2).nullable()
    val bench1rm = decimal("bench_1rm", precision = 6, scale = 2).nullable()
    val squat1rm = decimal("squat_1rm", precision = 6, scale = 2).nullable()
    val deadlift1rm = decimal("deadlift_1rm", precision = 6, scale = 2).nullable()
    val updatedAt = timestamp("updated_at")
}

object NutritionGoalsTable : UUIDTable("nutrition_goals", "user_id") {
    val caloriesGoal = integer("calories_goal")
    val proteinGoalG = integer("protein_goal_g")
}

object NutritionEntriesTable : UUIDTable("nutrition_entries") {
    val userId = uuid("user_id")
    val eatenAt = timestamp("eaten_at")
    val title = text("title")
    val calories = integer("calories")
    val proteinG = integer("protein_g")
}

object TrainingProgramsTable : UUIDTable("training_programs") {
    val userId = uuid("user_id")
    val name = text("name")
    val templateCode = text("template_code")
    val startDate = date("start_date")
    val weeks = integer("weeks")
    val isActive = bool("is_active")
    val createdAt = timestamp("created_at")
    val scheduleJson = text("schedule_json").nullable()
}

object ProgramWorkoutsTable : UUIDTable("program_workouts") {
    val programId = uuid("program_id")
    val workoutDate = date("workout_date")
    val title = text("title")
    val status = text("status")
    val originalWorkoutId = uuid("original_workout_id").nullable()
}

object ProgramExercisesTable : UUIDTable("program_exercises") {
    val programWorkoutId = uuid("program_workout_id")
    val exerciseName = text("exercise_name")
    val orderIndex = integer("order_index")
    val sets = integer("sets")
    val reps = text("reps")
    val percent1rm = decimal("percent_1rm", precision = 5, scale = 2).nullable()
    val liftType = text("lift_type")
}

object WorkoutSessionsTable : UUIDTable("workout_sessions") {
    val userId = uuid("user_id")
    val programWorkoutId = uuid("program_workout_id").nullable()
    val startedAt = timestamp("started_at")
    val finishedAt = timestamp("finished_at").nullable()
    val workoutDurationSec = integer("workout_duration_sec").nullable()

    val sleepHours = decimal("sleep_hours", precision = 3, scale = 1).nullable()
    val wellbeing = integer("wellbeing").nullable()
    val fatigue = integer("fatigue").nullable()
    val soreness = integer("soreness").nullable()
    val recommendation = text("recommendation").nullable()
    val wellbeingRating = integer("wellbeing_rating").nullable()
}

object WorkoutSetsTable : UUIDTable("workout_sets") {
    val sessionId = uuid("session_id")
    val exerciseName = text("exercise_name")
    val setNumber = integer("set_number")
    val weightKg = decimal("weight_kg", precision = 6, scale = 2)
    val reps = integer("reps")
    val rpe = decimal("rpe", precision = 3, scale = 1).nullable()
}

object AchievementsTable : UUIDTable("achievements") {
    val userId = uuid("user_id")
    val createdAt = timestamp("created_at")
    val note = text("note")
    val photoUrl = text("photo_url").nullable()
}
