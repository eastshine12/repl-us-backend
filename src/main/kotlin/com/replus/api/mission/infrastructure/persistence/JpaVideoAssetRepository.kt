package com.replus.api.mission.infrastructure.persistence

import com.replus.api.mission.domain.model.VideoAsset
import com.replus.api.mission.domain.repository.VideoAssetRepository
import org.springframework.stereotype.Repository

@Repository
class JpaVideoAssetRepository(
    private val videoAssetJpaRepository: VideoAssetJpaRepository,
) : VideoAssetRepository {
    override fun save(videoAsset: VideoAsset): VideoAsset =
        videoAssetJpaRepository.save(VideoAssetEntity.from(videoAsset)).toDomain()
}
