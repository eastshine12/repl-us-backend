package com.replus.api.mission.infrastructure.storage

import com.replus.api.mission.application.port.VideoStoragePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Instant

class StorageConfigTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(StorageConfig::class.java)

    @Test
    fun `local mode uses configured upload and playback base urls`() {
        contextRunner
            .withPropertyValues(
                "replus.storage.mode=local",
                "replus.storage.local.upload-base-url=https://uploads.example.dev/upload/",
                "replus.storage.local.playback-base-url=https://cdn.example.dev/playback/",
            )
            .run { context ->
                assertThat(context).hasSingleBean(VideoStoragePort::class.java)

                val storage = context.getBean(VideoStoragePort::class.java)
                val objectKey = "rooms/room-id/missions/mission-id/members/member-id.webm"

                val uploadTarget = storage.createUploadTarget(
                    objectKey = objectKey,
                    contentType = "video/webm",
                    expiresAt = Instant.parse("2026-05-24T09:25:00Z"),
                    maxFileSizeBytes = 15_000_000,
                )

                assertThat(uploadTarget.uploadUrl).isEqualTo("https://uploads.example.dev/upload/$objectKey")
                assertThat(storage.playbackUrl(objectKey)).isEqualTo("https://cdn.example.dev/playback/$objectKey")
            }
    }

    @Test
    fun `object storage mode fails fast until an adapter is implemented`() {
        contextRunner
            .withPropertyValues("replus.storage.mode=object-storage")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Object storage adapter is not implemented yet")
            }
    }
}
