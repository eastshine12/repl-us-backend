package com.replus.api.room.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.room.domain.model.RoomMember

class RoomAccessPolicy {
    fun requireActiveMember(member: RoomMember?) {
        if (member == null || !member.isActive()) {
            throw CoreException(ErrorType.ROOM_MEMBER_REQUIRED)
        }
    }
}
