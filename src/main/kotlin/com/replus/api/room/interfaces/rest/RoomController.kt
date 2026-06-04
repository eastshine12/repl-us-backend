package com.replus.api.room.interfaces.rest

import com.replus.api.common.security.BearerAuthSupport
import com.replus.api.room.application.RoomFacade
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
class RoomController(
    private val roomFacade: RoomFacade,
    private val bearerAuthSupport: BearerAuthSupport,
) {
    @PostMapping("/api/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRoom(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @Valid @RequestBody
        request: CreateRoomRequest,
    ): ResponseEntity<RoomDetailResponse> {
        val user = bearerAuthSupport.requireUser(authorization)
        val result = roomFacade.createRoom(user.userId, request.name)
        return ResponseEntity
            .created(URI.create("/api/rooms/${result.room.id}"))
            .body(result.toResponse())
    }

    @GetMapping("/api/rooms/{roomId}")
    fun getRoom(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
    ): RoomDetailResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return roomFacade.getRoomDetail(user.userId, roomId).toResponse()
    }

    @PostMapping("/api/rooms/{roomId}/invite-links")
    @ResponseStatus(HttpStatus.CREATED)
    fun createInviteLink(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @Valid @RequestBody(required = false)
        request: CreateInviteLinkRequest?,
    ): InviteLinkResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return roomFacade
            .createInviteLink(
                userId = user.userId,
                roomId = roomId,
                expiresInHours = request?.expiresInHours ?: 168,
                maxUses = request?.maxUses,
            )
            .toResponse()
    }

    @PostMapping("/api/invite-links/{code}/join")
    fun joinRoomByInviteCode(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable code: String,
    ): ResponseEntity<RoomDetailResponse> {
        val user = bearerAuthSupport.requireUser(authorization)
        val result = roomFacade.joinByInviteCode(user.userId, code)
        return ResponseEntity
            .ok()
            .location(URI.create("/api/rooms/${result.room.id}"))
            .body(result.toResponse())
    }

    @DeleteMapping("/api/rooms/{roomId}/members/me")
    fun leaveRoom(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
    ): RemoveMemberResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return roomFacade.leaveRoom(user.userId, roomId).toResponse()
    }

    @DeleteMapping("/api/rooms/{roomId}/members/{memberId}")
    fun removeRoomMember(
        @RequestHeader(BearerAuthSupport.AUTHORIZATION_HEADER, required = false)
        authorization: String?,
        @PathVariable roomId: UUID,
        @PathVariable memberId: UUID,
    ): RemoveMemberResponse {
        val user = bearerAuthSupport.requireUser(authorization)
        return roomFacade.removeMember(user.userId, roomId, memberId).toResponse()
    }
}
