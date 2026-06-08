package com.replus.api.mission.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.mission.domain.model.MissionResponse
import com.replus.api.mission.domain.model.MissionResponseStatus
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MissionResponseSubmissionPolicyTest {
    private val policy = MissionResponseSubmissionPolicy()

    @Test
    fun `리플 영상 길이가 3초가 아니면 제출할 수 없다`() {
        // given
        val durationSeconds = 2
        val hasAudio = true

        // when
        val action = { policy.validateVideoMetadata(durationSeconds, hasAudio) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_DURATION)
    }

    @Test
    fun `리플 영상에 오디오 메타데이터가 없으면 제출할 수 없다`() {
        // given
        val durationSeconds = 3
        val hasAudio = false

        // when
        val action = { policy.validateVideoMetadata(durationSeconds, hasAudio) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_AUDIO_REQUIRED)
    }

    @Test
    fun `이미 active 리플이 있으면 다시 제출할 수 없다`() {
        // given
        val existingResponse = missionResponse()

        // when
        val action = { policy.ensureCanCreate(existingResponse) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.RESPONSE_ALREADY_EXISTS)
    }

    @Test
    fun `3초이고 오디오가 있으며 active 리플이 없으면 제출할 수 있다`() {
        // given
        val durationSeconds = 3
        val hasAudio = true
        val existingResponse = null

        // when
        val action = {
            policy.validateVideoMetadata(durationSeconds, hasAudio)
            policy.ensureCanCreate(existingResponse)
        }

        // then
        assertThatCode { action() }
            .doesNotThrowAnyException()
    }

    private fun missionResponse(): MissionResponse =
        MissionResponse(
            id = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            missionId = UUID.randomUUID(),
            memberId = UUID.randomUUID(),
            videoAssetId = UUID.randomUUID(),
            status = MissionResponseStatus.ACTIVE,
            createdAt = Instant.parse("2026-05-24T09:15:05Z"),
            deletedAt = null,
        )
}
