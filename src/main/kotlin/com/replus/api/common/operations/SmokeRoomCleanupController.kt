package com.replus.api.common.operations

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@ConditionalOnProperty(
    name = ["replus.operations.smoke-cleanup.enabled"],
    havingValue = "true",
)
class SmokeRoomCleanupController(
    private val smokeRoomCleanupService: SmokeRoomCleanupService,
) {
    @DeleteMapping("/internal/operations/smoke-rooms/{roomId}")
    fun cleanupSmokeRoom(
        @PathVariable roomId: UUID,
        @RequestHeader(SMOKE_CLEANUP_TOKEN_HEADER, required = false)
        operationsToken: String?,
    ): SmokeRoomCleanupResponse =
        smokeRoomCleanupService.cleanup(roomId, operationsToken).toResponse()

    private fun SmokeRoomCleanupResult.toResponse(): SmokeRoomCleanupResponse =
        SmokeRoomCleanupResponse(
            roomId = roomId,
            deleted = deleted,
            deletedInviteLinks = deletedInviteLinks,
            deletedRoomMembers = deletedRoomMembers,
            deletedMissions = deletedMissions,
            deletedMissionResponses = deletedMissionResponses,
            deletedVideoAssets = deletedVideoAssets,
        )

    private companion object {
        const val SMOKE_CLEANUP_TOKEN_HEADER = "X-Replus-Operations-Token"
    }
}

data class SmokeRoomCleanupResponse(
    val roomId: UUID,
    val deleted: Boolean,
    val deletedInviteLinks: Int,
    val deletedRoomMembers: Int,
    val deletedMissions: Int,
    val deletedMissionResponses: Int,
    val deletedVideoAssets: Int,
)
