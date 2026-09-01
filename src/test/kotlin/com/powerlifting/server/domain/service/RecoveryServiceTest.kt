package com.powerlifting.server.domain.service

import com.powerlifting.server.domain.model.RecoveryInputs
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecoveryServiceTest {

    private val service = RecoveryService()

    @Test
    fun `no inputs means no recommendation`() {
        assertNull(service.makeRecommendation(RecoveryInputs(null, null, null, null)))
    }

    @Test
    fun `critical sleep deprivation recommends postponing`() {
        val result = service.makeRecommendation(RecoveryInputs(sleepHours = 3.0, wellbeing = 7, fatigue = 4, soreness = 4))
        assertTrue(result!!.contains("перенести"))
    }

    @Test
    fun `low wellbeing recommends postponing even with normal sleep`() {
        val result = service.makeRecommendation(RecoveryInputs(sleepHours = 8.0, wellbeing = 2, fatigue = 4, soreness = 4))
        assertTrue(result!!.contains("перенести"))
    }

    @Test
    fun `high fatigue recommends reducing load`() {
        val result = service.makeRecommendation(RecoveryInputs(sleepHours = 8.0, wellbeing = 7, fatigue = 9, soreness = 4))
        assertTrue(result!!.contains("снизить нагрузку"))
    }

    @Test
    fun `good recovery allows training as planned`() {
        val result = service.makeRecommendation(RecoveryInputs(sleepHours = 8.0, wellbeing = 8, fatigue = 3, soreness = 2))
        assertTrue(result!!.contains("по плану"))
    }
}
