package com.replus.api.mission.infrastructure.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class LocalVideoStorageAdapterTest {
    private val storage = LocalVideoStorageAdapter(
        LocalStorageProperties(
            uploadBaseUrl = "https://uploads.example.dev/mock-upload/",
            playbackBaseUrl = "https://cdn.example.dev/mock-playback/",
        ),
    )

    @Test
    fun `업로드 대상은 object key와 필수 헤더를 포함한다`() {
        // given
        val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"
        val expiresAt = Instant.parse("2026-05-24T09:25:00Z")

        // when
        val target = storage.createUploadTarget(
            objectKey = objectKey,
            contentType = "video/webm",
            expiresAt = expiresAt,
            maxFileSizeBytes = 15_000_000,
        )

        // then
        assertThat(target.uploadUrl).isEqualTo("https://uploads.example.dev/mock-upload/$objectKey")
        assertThat(target.method).isEqualTo("PUT")
        assertThat(target.objectKey).isEqualTo(objectKey)
        assertThat(target.requiredHeaders).containsEntry("Content-Type", "video/webm")
        assertThat(target.expiresAt).isEqualTo(expiresAt)
        assertThat(target.maxFileSizeBytes).isEqualTo(15_000_000)
    }

    @Test
    fun `재생 URL은 object key 기반으로 만든다`() {
        // given
        val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

        // when
        val playbackUrl = storage.playbackUrl(objectKey)

        // then
        assertThat(playbackUrl).isEqualTo("https://cdn.example.dev/mock-playback/$objectKey")
    }
}
