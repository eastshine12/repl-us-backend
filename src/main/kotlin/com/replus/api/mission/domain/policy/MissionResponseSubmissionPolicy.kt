package com.replus.api.mission.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.MissionResponse

class MissionResponseSubmissionPolicy {
    fun validateVideoMetadata(durationSeconds: Int, hasAudio: Boolean) {
        if (durationSeconds != MVP_DURATION_SECONDS) {
            throw CoreException(ErrorType.INVALID_DURATION)
        }
        if (!hasAudio) {
            throw CoreException(ErrorType.INVALID_AUDIO_REQUIRED)
        }
    }

    fun ensureCanCreate(existingActiveResponse: MissionResponse?) {
        if (existingActiveResponse != null) {
            throw CoreException(ErrorType.RESPONSE_ALREADY_EXISTS)
        }
    }

    companion object {
        const val MVP_DURATION_SECONDS = 3
    }
}
