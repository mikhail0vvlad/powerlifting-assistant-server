package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toProgramExercise
import com.powerlifting.server.data.repository.mapper.toWorkoutSet
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.ProgramExercisesTable
import com.powerlifting.server.db.tables.ProgramWorkoutsTable
import com.powerlifting.server.db.tables.WorkoutSessionsTable
import com.powerlifting.server.db.tables.WorkoutSetsTable
import com.powerlifting.server.domain.model.WorkoutHistoryItem
import com.powerlifting.server.domain.model.WorkoutHistoryPage
import com.powerlifting.server.domain.model.WorkoutSessionDetail
import com.powerlifting.server.domain.model.WorkoutSet
import com.powerlifting.server.domain.repository.WorkoutRepository
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class WorkoutRepositoryImpl : WorkoutRepository {

    override suspend fun startSession(
        userId: UUID,
        programWorkoutId: UUID?,
        sleepHours: Double?,
        wellbeing: Int?,
        fatigue: Int?,
        soreness: Int?,
        recommendation: String?
    ): UUID = dbQuery {
        WorkoutSessionsTable.insertAndGetId {
            it[WorkoutSessionsTable.userId] = userId
            it[WorkoutSessionsTable.programWorkoutId] = programWorkoutId
            it[startedAt] = Instant.now()
            it[finishedAt] = null
            it[workoutDurationSec] = null
            it[WorkoutSessionsTable.sleepHours] = sleepHours?.toBigDecimal()
            it[WorkoutSessionsTable.wellbeing] = wellbeing
            it[WorkoutSessionsTable.fatigue] = fatigue
            it[WorkoutSessionsTable.soreness] = soreness
            it[WorkoutSessionsTable.recommendation] = recommendation
        }.value
    }

    override suspend fun replaceSets(userId: UUID, sessionId: UUID, sets: List<WorkoutSet>): Unit = dbQuery {
        WorkoutSessionsTable
            .select { (WorkoutSessionsTable.id eq sessionId) and (WorkoutSessionsTable.userId eq userId) }
            .limit(1)
            .singleOrNull() ?: error("Session not found")

        WorkoutSetsTable.deleteWhere { WorkoutSetsTable.sessionId eq sessionId }

        sets.forEach { s ->
            WorkoutSetsTable.insert {
                it[WorkoutSetsTable.sessionId] = sessionId
                it[exerciseName] = s.exerciseName
                it[setNumber] = s.setNumber
                it[weightKg] = s.weightKg.toBigDecimal()
                it[reps] = s.reps
                it[rpe] = s.rpe?.toBigDecimal()
            }
        }
    }

    override suspend fun finishSession(
        userId: UUID,
        sessionId: UUID,
        durationSec: Int,
        wellbeingRating: Int?
    ): UUID? = dbQuery {
        val row = WorkoutSessionsTable
            .select { (WorkoutSessionsTable.id eq sessionId) and (WorkoutSessionsTable.userId eq userId) }
            .limit(1)
            .singleOrNull() ?: error("Session not found")

        WorkoutSessionsTable.update({ WorkoutSessionsTable.id eq sessionId }) {
            it[finishedAt] = Instant.now()
            it[workoutDurationSec] = durationSec
            if (wellbeingRating != null) {
                it[WorkoutSessionsTable.wellbeingRating] = wellbeingRating
            }
        }

        row[WorkoutSessionsTable.programWorkoutId]
    }

    override suspend fun getSessionDetail(userId: UUID, sessionId: UUID): WorkoutSessionDetail? = dbQuery {
        val row = WorkoutSessionsTable
            .select { (WorkoutSessionsTable.id eq sessionId) and (WorkoutSessionsTable.userId eq userId) }
            .limit(1)
            .singleOrNull() ?: return@dbQuery null

        val programWorkoutId = row[WorkoutSessionsTable.programWorkoutId]

        val exercises = if (programWorkoutId != null) {
            ProgramExercisesTable
                .select { ProgramExercisesTable.programWorkoutId eq programWorkoutId }
                .orderBy(ProgramExercisesTable.orderIndex, SortOrder.ASC)
                .map { it.toProgramExercise() }
        } else emptyList()

        val loggedSets = WorkoutSetsTable
            .select { WorkoutSetsTable.sessionId eq sessionId }
            .orderBy(WorkoutSetsTable.setNumber, SortOrder.ASC)
            .map { it.toWorkoutSet() }

        WorkoutSessionDetail(
            sessionId = sessionId,
            programWorkoutId = programWorkoutId,
            recommendation = row[WorkoutSessionsTable.recommendation],
            exercises = exercises,
            loggedSets = loggedSets
        )
    }

    override suspend fun deleteSession(userId: UUID, sessionId: UUID): Boolean = dbQuery {
        WorkoutSessionsTable.deleteWhere {
            (WorkoutSessionsTable.id eq sessionId) and (WorkoutSessionsTable.userId eq userId)
        } > 0
    }

    override suspend fun getHistory(userId: UUID, before: Instant?, limit: Int): WorkoutHistoryPage = dbQuery {
        // Query 1: sessions joined with program_workouts to fetch the title in one go.
        val sessionsQuery = WorkoutSessionsTable
            .join(
                ProgramWorkoutsTable,
                JoinType.LEFT,
                additionalConstraint = { WorkoutSessionsTable.programWorkoutId eq ProgramWorkoutsTable.id }
            )
            .select {
                val base = (WorkoutSessionsTable.userId eq userId) and
                    (WorkoutSessionsTable.finishedAt.isNotNull())
                if (before != null) base and (WorkoutSessionsTable.startedAt less before) else base
            }
            .orderBy(WorkoutSessionsTable.startedAt, SortOrder.DESC)
            .limit(limit)
            .toList()

        if (sessionsQuery.isEmpty()) {
            return@dbQuery WorkoutHistoryPage(items = emptyList(), nextCursor = null)
        }

        val sessionIds = sessionsQuery.map { it[WorkoutSessionsTable.id].value }

        // Query 2: one grouped count for all sessions in the page (replaces the N
        // SELECT-COUNT loop the original implementation had).
        val countAlias = WorkoutSetsTable.id.count().alias("sets_count")
        val setsCounts: Map<UUID, Int> = WorkoutSetsTable
            .slice(WorkoutSetsTable.sessionId, countAlias)
            .select { WorkoutSetsTable.sessionId inList sessionIds }
            .groupBy(WorkoutSetsTable.sessionId)
            .associate { row ->
                row[WorkoutSetsTable.sessionId] to row[countAlias].toInt()
            }

        val items = sessionsQuery.map { row ->
            val sid = row[WorkoutSessionsTable.id].value
            WorkoutHistoryItem(
                sessionId = sid,
                startedAt = row[WorkoutSessionsTable.startedAt],
                durationSec = row[WorkoutSessionsTable.workoutDurationSec],
                workoutTitle = row.getOrNull(ProgramWorkoutsTable.title),
                wellbeingRating = row[WorkoutSessionsTable.wellbeingRating],
                setsCount = setsCounts[sid] ?: 0
            )
        }

        WorkoutHistoryPage(
            items = items,
            // If we returned a full page, the last item's startedAt is the next cursor.
            // If we returned fewer than `limit`, this is the last page.
            nextCursor = if (items.size == limit) items.last().startedAt else null
        )
    }
}
