package com.replus.api.mission.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.MissionReleaseState

class MissionResponseDeletionPolicy {
    fun validateCanDelete(
        activeMemberCount: Int,
        submittedCount: Int,
        releaseState: MissionReleaseState?,
    ) {
        if (activeMemberCount > 0 && submittedCount >= activeMemberCount) {
            throw CoreException(ErrorType.RESPONSE_RELEASE_LOCKED)
        }
        if (releaseState?.allSubmittedAt != null || releaseState?.releasedAt != null) {
            throw CoreException(ErrorType.RESPONSE_RELEASE_LOCKED)
        }
    }
}
