package com.powerlifting.server.domain.repository

import com.powerlifting.server.domain.model.NewNutritionEntry
import com.powerlifting.server.domain.model.NutritionEntry
import java.time.LocalDate
import java.util.UUID

interface NutritionRepository {
    suspend fun getEntriesForDate(userId: UUID, date: LocalDate): List<NutritionEntry>
    suspend fun createEntry(userId: UUID, entry: NewNutritionEntry): NutritionEntry
    suspend fun deleteEntry(userId: UUID, entryId: UUID): Boolean
}
