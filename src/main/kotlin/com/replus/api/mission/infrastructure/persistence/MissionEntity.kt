package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.Mission
import com.replus.api.mission.domain.model.MissionCategory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "missions")
class MissionEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "room_id", nullable = false)
    var roomId: UUID,

    @Column(name = "mission_date", nullable = false)
    var missionDate: LocalDate,

    @Column(name = "prompt", nullable = false, length = 80)
    var prompt: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    var category: MissionCategory,

    @Column(name = "edit_count", nullable = false)
    var editCount: Int,

    @Column(name = "edited_by_member_id")
    var editedByMemberId: UUID?,

    @Column(name = "edited_at")
    var editedAt: Instant?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Mission =
        Mission(
            id = id,
            roomId = roomId,
            missionDate = missionDate,
            prompt = prompt,
            category = category,
            editCount = editCount,
            editedByMemberId = editedByMemberId,
            editedAt = editedAt,
            createdAt = createdAt,
        )

    companion object {
        fun from(mission: Mission): MissionEntity =
            MissionEntity(
                id = mission.id,
                roomId = mission.roomId,
                missionDate = mission.missionDate,
                prompt = mission.prompt,
                category = mission.category,
                editCount = mission.editCount,
                editedByMemberId = mission.editedByMemberId,
                editedAt = mission.editedAt,
                createdAt = mission.createdAt,
            )
    }
}
