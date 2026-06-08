package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.MissionResponseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface MissionJpaRepository : JpaRepository<MissionEntity, UUID> {
    fun findByRoomIdAndMissionDate(roomId: UUID, missionDate: LocalDate): MissionEntity?

    fun findAllByRoomIdOrderByMissionDateDesc(roomId: UUID): List<MissionEntity>

    fun findAllByRoomIdAndMissionDateBetweenOrderByMissionDateDesc(
        roomId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<MissionEntity>

    fun findFirstByRoomIdOrderByMissionDateDesc(roomId: UUID): MissionEntity?

    fun findByIdAndRoomId(id: UUID, roomId: UUID): MissionEntity?
}

interface MissionResponseJpaRepository : JpaRepository<MissionResponseEntity, UUID> {
    fun countByRoomIdAndStatus(roomId: UUID, status: MissionResponseStatus): Int

    fun countByMissionIdAndStatus(missionId: UUID, status: MissionResponseStatus): Int

    fun findAllByMissionIdAndStatus(
        missionId: UUID,
        status: MissionResponseStatus,
    ): List<MissionResponseEntity>

    fun findAllByMissionIdInAndStatus(
        missionIds: Collection<UUID>,
        status: MissionResponseStatus,
    ): List<MissionResponseEntity>

    fun findByMissionIdAndMemberIdAndStatus(
        missionId: UUID,
        memberId: UUID,
        status: MissionResponseStatus,
    ): MissionResponseEntity?

    fun findByMissionIdAndMemberId(missionId: UUID, memberId: UUID): MissionResponseEntity?

    fun findByIdAndRoomIdAndStatus(
        id: UUID,
        roomId: UUID,
        status: MissionResponseStatus,
    ): MissionResponseEntity?
}

interface VideoAssetJpaRepository : JpaRepository<VideoAssetEntity, UUID> {
    fun findByObjectKey(objectKey: String): VideoAssetEntity?
}

interface MissionReleaseStateJpaRepository : JpaRepository<MissionReleaseStateEntity, UUID>

interface ResponseReactionJpaRepository : JpaRepository<ResponseReactionEntity, UUID> {
    fun findAllByResponseIdIn(responseIds: Collection<UUID>): List<ResponseReactionEntity>

    fun deleteAllByResponseId(responseId: UUID)
}

interface ResponseCommentJpaRepository : JpaRepository<ResponseCommentEntity, UUID> {
    fun findAllByResponseIdAndDeletedAtIsNullOrderByCreatedAtAsc(responseId: UUID): List<ResponseCommentEntity>

    @Modifying
    @Query("update ResponseCommentEntity c set c.deletedAt = ?2 where c.responseId = ?1 and c.deletedAt is null")
    fun softDeleteByResponseId(responseId: UUID, deletedAt: Instant)
}
