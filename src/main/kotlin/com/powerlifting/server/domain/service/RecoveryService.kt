package com.powerlifting.server.domain.service

import com.powerlifting.server.domain.model.RecoveryInputs

class RecoveryService {
    /**
     * Simple deterministic rules, easy to justify on defense.
     * Inputs are nullable; if missing, recommendation may be null.
     */
    fun makeRecommendation(inputs: RecoveryInputs): String? {
        if (inputs.isEmpty) return null

        val sh = inputs.sleepHours ?: 8.0
        val wb = inputs.wellbeing ?: 7
        val ft = inputs.fatigue ?: 5
        val sr = inputs.soreness ?: 5

        return when {
            sh < 4.0 || wb <= 3 -> "Настоятельно рекомендуется перенести тренировку. Восстановись и попробуй завтра."
            sh < 6.0 || ft >= 8 || sr >= 8 -> "Рекомендуется снизить нагрузку: уменьшить вес/объём, увеличить отдых, следить за техникой."
            else -> "Можно тренироваться по плану. Сохраняй технику и контролируй самочувствие."
        }
    }
}
