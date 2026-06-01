package com.replus.api.mission.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Mission(
    val id: UUID,
    val roomId: UUID,
    val missionDate: LocalDate,
    val prompt: String,
    val category: MissionCategory,
    val editCount: Int,
    val editedByMemberId: UUID?,
    val editedAt: Instant?,
    val createdAt: Instant,
) {
    init {
        require(prompt.isNotBlank()) { "Mission prompt must not be blank." }
        require(prompt.length <= 80) { "Mission prompt must be 80 characters or fewer." }
        require(editCount in 0..1) { "MVP mission edit count must be 0 or 1." }
    }

    fun edit(prompt: String, category: MissionCategory, editorMemberId: UUID, editedAt: Instant): Mission =
        copy(
            prompt = prompt,
            category = category,
            editCount = editCount + 1,
            editedByMemberId = editorMemberId,
            editedAt = editedAt,
        )
}

enum class MissionCategory {
    OBSERVATION,
    MOOD,
    OUTING_FOOD,
    FULL_PARTICIPATION,
}
