package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class S3ObjectStorageUploadVerifierTest {
    private val headObjectClient = RecordingS3HeadObjectClient()
    private val verifier = S3ObjectStorageUploadVerifier(headObjectClient)

    @Test
    fun `head object metadata is returned as uploaded object verification`() {
        // given
        headObjectClient.nextResult = S3HeadObjectResult(
            contentType = "video/webm",
            fileSizeBytes = 842120,
        )

        // when
        val verification = verifier.verifyUploadedObject(
            VerifyUploadedObjectCommand(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                expectedContentType = "video/webm",
                expectedFileSizeBytes = 842120,
            ),
        )

        // then
        assertThat(headObjectClient.recordedRequest).isEqualTo(
            S3HeadObjectRequest(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
            ),
        )
        assertThat(verification.exists).isTrue
        assertThat(verification.contentType).isEqualTo("video/webm")
        assertThat(verification.fileSizeBytes).isEqualTo(842120)
    }

    @Test
    fun `not found head object result is returned as missing upload`() {
        // given
        headObjectClient.nextFailure = S3ObjectNotFoundException()

        // when
        val verification = verifier.verifyUploadedObject(
            VerifyUploadedObjectCommand(
                bucket = "replus-dev-videos",
                objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                expectedContentType = "video/webm",
                expectedFileSizeBytes = 842120,
            ),
        )

        // then
        assertThat(verification.exists).isFalse
        assertThat(verification.contentType).isNull()
        assertThat(verification.fileSizeBytes).isNull()
    }

    @Test
    fun `unexpected head object failure is propagated`() {
        // given
        val failure = IllegalStateException("storage unavailable")
        headObjectClient.nextFailure = failure

        // expect
        assertThatThrownBy {
            verifier.verifyUploadedObject(
                VerifyUploadedObjectCommand(
                    bucket = "replus-dev-videos",
                    objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm",
                    expectedContentType = "video/webm",
                    expectedFileSizeBytes = 842120,
                ),
            )
        }.isSameAs(failure)
    }

    private class RecordingS3HeadObjectClient : S3HeadObjectClient {
        var recordedRequest: S3HeadObjectRequest? = null
        var nextResult: S3HeadObjectResult? = null
        var nextFailure: RuntimeException? = null

        override fun headObject(request: S3HeadObjectRequest): S3HeadObjectResult {
            recordedRequest = request
            nextFailure?.let { throw it }
            return requireNotNull(nextResult)
        }
    }
}
