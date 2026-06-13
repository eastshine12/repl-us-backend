package com.replus.api.common.operations

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SmokeRoomCleanupService(
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${replus.operations.smoke-cleanup.token:}")
    private val cleanupToken: String,
    @Value("\${replus.operations.smoke-cleanup.room-name-prefix:Smoke Room }")
    private val roomNamePrefix: String,
) {
    @Transactional
    fun cleanup(roomId: UUID, operationsToken: String?): SmokeRoomCleanupResult {
        requireValidToken(operationsToken)

        val roomName = findRoomName(roomId)
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)
        if (!roomName.startsWith(roomNamePrefix)) {
            throw CoreException(
                ErrorType.INVALID_REQUEST,
                "Only smoke rooms with '$roomNamePrefix' prefix can be cleaned up.",
            )
        }

        val videoAssetIds = findVideoAssetIds(roomId)
        deleteResponseChildren(roomId)
        deleteMissionReleaseStates(roomId)
        val deletedMissionResponses = deleteMissionResponses(roomId)
        val deletedVideoAssets = deleteVideoAssets(videoAssetIds)
        val deletedMissions = deleteMissions(roomId)
        val deletedInviteLinks = deleteInviteLinks(roomId)
        val deletedRoomMembers = deleteRoomMembers(roomId)
        val deletedRooms = deleteRoom(roomId)

        return SmokeRoomCleanupResult(
            roomId = roomId,
            deleted = deletedRooms == 1,
            deletedInviteLinks = deletedInviteLinks,
            deletedRoomMembers = deletedRoomMembers,
            deletedMissions = deletedMissions,
            deletedMissionResponses = deletedMissionResponses,
            deletedVideoAssets = deletedVideoAssets,
        )
    }

    private fun requireValidToken(operationsToken: String?) {
        if (cleanupToken.isBlank() || operationsToken == null || operationsToken != cleanupToken) {
            throw CoreException(ErrorType.UNAUTHENTICATED)
        }
    }

    private fun findRoomName(roomId: UUID): String? =
        jdbcTemplate.query(
            "select name from rooms where id = ?",
            { resultSet, _ -> resultSet.getString("name") },
            roomId,
        ).firstOrNull()

    private fun findVideoAssetIds(roomId: UUID): List<UUID> =
        jdbcTemplate.query(
            "select video_asset_id from mission_responses where room_id = ?",
            { resultSet, _ -> resultSet.getObject("video_asset_id", UUID::class.java) },
            roomId,
        )

    private fun deleteResponseChildren(roomId: UUID) {
        jdbcTemplate.update(
            """
            delete from response_comments
            where response_id in (
                select id from mission_responses where room_id = ?
            )
            """.trimIndent(),
            roomId,
        )
        jdbcTemplate.update(
            """
            delete from response_reactions
            where response_id in (
                select id from mission_responses where room_id = ?
            )
            """.trimIndent(),
            roomId,
        )
    }

    private fun deleteMissionReleaseStates(roomId: UUID): Int =
        jdbcTemplate.update(
            """
            delete from mission_release_states
            where mission_id in (
                select id from missions where room_id = ?
            )
            """.trimIndent(),
            roomId,
        )

    private fun deleteMissionResponses(roomId: UUID): Int =
        jdbcTemplate.update("delete from mission_responses where room_id = ?", roomId)

    private fun deleteVideoAssets(videoAssetIds: List<UUID>): Int {
        if (videoAssetIds.isEmpty()) {
            return 0
        }

        val placeholders = videoAssetIds.joinToString(",") { "?" }
        return jdbcTemplate.update(
            "delete from video_assets where id in ($placeholders)",
            *videoAssetIds.toTypedArray(),
        )
    }

    private fun deleteMissions(roomId: UUID): Int =
        jdbcTemplate.update("delete from missions where room_id = ?", roomId)

    private fun deleteInviteLinks(roomId: UUID): Int =
        jdbcTemplate.update("delete from invite_links where room_id = ?", roomId)

    private fun deleteRoomMembers(roomId: UUID): Int =
        jdbcTemplate.update("delete from room_members where room_id = ?", roomId)

    private fun deleteRoom(roomId: UUID): Int =
        jdbcTemplate.update("delete from rooms where id = ?", roomId)
}

data class SmokeRoomCleanupResult(
    val roomId: UUID,
    val deleted: Boolean,
    val deletedInviteLinks: Int,
    val deletedRoomMembers: Int,
    val deletedMissions: Int,
    val deletedMissionResponses: Int,
    val deletedVideoAssets: Int,
)
