package com.replus.api.common.error

enum class ErrorType(
    val code: ErrorCode,
    val defaultMessage: String,
) {
    UNAUTHENTICATED(
        ErrorCode.UNAUTHENTICATED,
        "Missing or invalid bearer session.",
    ),
    INVALID_REQUEST(
        ErrorCode.INVALID_REQUEST,
        "The request is invalid.",
    ),
    RESOURCE_NOT_FOUND(
        ErrorCode.RESOURCE_NOT_FOUND,
        "The requested resource was not found.",
    ),
    ROOM_MEMBER_REQUIRED(
        ErrorCode.ROOM_MEMBER_REQUIRED,
        "Active room membership is required.",
    ),
    ROOM_OWNER_REQUIRED(
        ErrorCode.ROOM_OWNER_REQUIRED,
        "Room owner permission is required.",
    ),
    ROOM_FULL(
        ErrorCode.ROOM_FULL,
        "The room is already full.",
    ),
    INVITE_LINK_NOT_FOUND(
        ErrorCode.INVITE_LINK_NOT_FOUND,
        "The invite link was not found or is no longer usable.",
    ),
    INVITE_LINK_EXPIRED(
        ErrorCode.INVITE_LINK_EXPIRED,
        "The invite link has expired.",
    ),
    MISSION_EDIT_LIMIT_REACHED(
        ErrorCode.MISSION_EDIT_LIMIT_REACHED,
        "Today's mission has already been edited.",
    ),
    MISSION_ALREADY_HAS_RESPONSE(
        ErrorCode.MISSION_ALREADY_HAS_RESPONSE,
        "Today's mission already has an active response.",
    ),
    INVALID_DURATION(
        ErrorCode.INVALID_DURATION,
        "Mission response videos must be exactly 3 seconds.",
    ),
    INVALID_AUDIO_REQUIRED(
        ErrorCode.INVALID_AUDIO_REQUIRED,
        "Mission response videos must include audio metadata.",
    ),
    RESPONSE_ALREADY_EXISTS(
        ErrorCode.RESPONSE_ALREADY_EXISTS,
        "You already submitted an active response for this mission.",
    ),
    RESPONSE_RELEASE_LOCKED(
        ErrorCode.RESPONSE_RELEASE_LOCKED,
        "Responses are locked after all members submit or the shared release opens.",
    ),
    RESPONSE_NOT_VISIBLE(
        ErrorCode.RESPONSE_NOT_VISIBLE,
        "The response is not visible to the viewer.",
    ),
    CANNOT_REMOVE_OWNER(
        ErrorCode.CANNOT_REMOVE_OWNER,
        "The room owner cannot be removed.",
    ),
    INTERNAL_ERROR(
        ErrorCode.INTERNAL_ERROR,
        "An unexpected server error occurred.",
    ),
}
