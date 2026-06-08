package com.replus.api.mission.interfaces.rest

import com.replus.api.common.security.BearerAuthSupport
import com.replus.api.mission.application.MissionFacade
import com.replus.api.mission.application.port.VideoStoragePort
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class MissionController(
    private val missionFacade: MissionFacade,
    private val bearerAuthSupport: BearerAuthSupport,
    private val videoStoragePort: VideoStoragePort,
) {
    @GetMapping("/api/rooms/{roomId}/today")
    fun getToday(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
    ): TodayResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return missionFacade.getToday(user.userId, roomId).toResponse(videoStoragePort)
    }

    @PatchMapping("/api/rooms/{roomId}/missions/{missionId}")
    fun updateMission(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable missionId: UUID,
        @Valid @RequestBody
        request: UpdateMissionRequest,
    ): MissionResponseDto {
        val user = bearerAuthSupport.requireUser(authorization)
        val mission = missionFacade.updateTodayMission(
            userId = user.userId,
            roomId = roomId,
            missionId = missionId,
            prompt = request.prompt,
            category = request.category,
        )
        return mission.toResponse(canEdit = false)
    }

    @PostMapping("/api/rooms/{roomId}/missions/{missionId}/responses/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    fun createResponseUploadUrl(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable missionId: UUID,
        @Valid @RequestBody
        request: CreateUploadUrlRequest,
    ): UploadUrlResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return missionFacade.createResponseUploadUrl(
            userId = user.userId,
            roomId = roomId,
            missionId = missionId,
            metadata = request.toMetadata(),
        ).toResponse()
    }

    @PostMapping("/api/rooms/{roomId}/missions/{missionId}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    fun createMissionResponse(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable missionId: UUID,
        @Valid @RequestBody
        request: CreateMissionResponseRequest,
    ): MissionResponseCreatedResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return missionFacade.createMissionResponse(
            userId = user.userId,
            roomId = roomId,
            missionId = missionId,
            command = request.toCommand(),
        ).toResponse(videoStoragePort)
    }

    @DeleteMapping("/api/rooms/{roomId}/responses/{responseId}")
    fun deleteMissionResponse(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable responseId: UUID,
    ): DeleteResponseResultResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return missionFacade.deleteMissionResponse(
            userId = user.userId,
            roomId = roomId,
            responseId = responseId,
        ).toResponse()
    }
}
