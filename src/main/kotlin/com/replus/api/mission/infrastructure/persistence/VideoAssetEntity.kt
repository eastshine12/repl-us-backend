package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.VideoAsset
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "video_assets")
class VideoAssetEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "object_key", nullable = false, length = 512)
    var objectKey: String,

    @Column(name = "content_type", nullable = false, length = 64)
    var contentType: String,

    @Column(name = "file_size_bytes", nullable = false)
    var fileSizeBytes: Long,

    @Column(name = "duration_seconds", nullable = false)
    var durationSeconds: Int,

    @Column(name = "has_audio", nullable = false)
    var hasAudio: Boolean,

    @Column(name = "width")
    var width: Int?,

    @Column(name = "height")
    var height: Int?,

    @Column(name = "thumbnail_object_key")
    var thumbnailObjectKey: String?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): VideoAsset =
        VideoAsset(
            id = id,
            objectKey = objectKey,
            contentType = contentType,
            fileSizeBytes = fileSizeBytes,
            durationSeconds = durationSeconds,
            hasAudio = hasAudio,
            width = width,
            height = height,
            thumbnailObjectKey = thumbnailObjectKey,
            createdAt = createdAt,
        )

    companion object {
        fun from(videoAsset: VideoAsset): VideoAssetEntity =
            VideoAssetEntity(
                id = videoAsset.id,
                objectKey = videoAsset.objectKey,
                contentType = videoAsset.contentType,
                fileSizeBytes = videoAsset.fileSizeBytes,
                durationSeconds = videoAsset.durationSeconds,
                hasAudio = videoAsset.hasAudio,
                width = videoAsset.width,
                height = videoAsset.height,
                thumbnailObjectKey = videoAsset.thumbnailObjectKey,
                createdAt = videoAsset.createdAt,
            )
    }
}
