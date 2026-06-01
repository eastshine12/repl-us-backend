package com.replus.api.mission.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.Mission
import com.replus.api.room.domain.model.RoomMember

class MissionEditPolicy {
    fun validateTodayEdit(
        editor: RoomMember,
        mission: Mission,
        activeResponseCount: Int,
    ) {
        if (!editor.isOwner()) {
            throw CoreException(ErrorType.ROOM_OWNER_REQUIRED)
        }
        if (mission.editCount >= 1) {
            throw CoreException(ErrorType.MISSION_EDIT_LIMIT_REACHED)
        }
        if (activeResponseCount > 0) {
            throw CoreException(ErrorType.MISSION_ALREADY_HAS_RESPONSE)
        }
    }
}
