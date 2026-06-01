package com.replus.api.room.infrastructure.persistence

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.room.domain.model.Room
import com.replus.api.room.domain.repository.RoomRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaRoomRepository(
    private val roomJpaRepository: RoomJpaRepository,
) : RoomRepository {
    override fun getById(roomId: UUID): Room =
        roomJpaRepository.findByIdOrNull(roomId)?.toDomain()
            ?: throw CoreException(ErrorType.RESOURCE_NOT_FOUND)

    override fun save(room: Room): Room =
        roomJpaRepository.save(RoomEntity.from(room)).toDomain()
}
