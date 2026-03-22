package com.powerlifting.server.data.repository

import com.powerlifting.server.data.repository.mapper.toNutritionEntry
import com.powerlifting.server.db.dbQuery
import com.powerlifting.server.db.tables.NutritionEntriesTable
import com.powerlifting.server.domain.model.NewNutritionEntry
import com.powerlifting.server.domain.model.NutritionEntry
import com.powerlifting.server.domain.repository.NutritionRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class NutritionRepositoryImpl : NutritionRepository {

    override suspend fun getEntriesForDate(userId: UUID, date: LocalDate): List<NutritionEntry> = dbQuery {
        val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        NutritionEntriesTable
            .select {
                (NutritionEntriesTable.userId eq userId) and
                    (NutritionEntriesTable.eatenAt greaterEq start) and
                    (NutritionEntriesTable.eatenAt less end)
            }
            .orderBy(NutritionEntriesTable.eatenAt, SortOrder.ASC)
            .map { it.toNutritionEntry() }
    }

    override suspend fun createEntry(userId: UUID, entry: NewNutritionEntry): NutritionEntry = dbQuery {
        val eatenAt = entry.eatenAt ?: Instant.now()

        val id = NutritionEntriesTable.insertAndGetId {
            it[NutritionEntriesTable.userId] = userId
            it[NutritionEntriesTable.eatenAt] = eatenAt
            it[title] = entry.title
            it[calories] = entry.calories
            it[proteinG] = entry.proteinG
        }.value

        NutritionEntry(
            id = id,
            title = entry.title,
            eatenAt = eatenAt,
            calories = entry.calories,
            proteinG = entry.proteinG
        )
    }

    override suspend fun deleteEntry(userId: UUID, entryId: UUID): Boolean = dbQuery {
        NutritionEntriesTable.deleteWhere {
            (NutritionEntriesTable.id eq entryId) and (NutritionEntriesTable.userId eq userId)
        } > 0
    }
}
