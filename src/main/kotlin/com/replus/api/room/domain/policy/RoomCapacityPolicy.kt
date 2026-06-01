package com.replus.api.room.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType

class RoomCapacityPolicy {
    fun ensureCanJoin(activeMemberCount: Int, maxMembers: Int) {
        if (activeMemberCount >= maxMembers) {
            throw CoreException(ErrorType.ROOM_FULL)
        }
    }
}
