package com.replus.api.room.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import com.replus.api.room.domain.model.RoomMember
import com.replus.api.room.domain.model.RoomMemberStatus
import com.replus.api.room.domain.model.RoomRole
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RoomAccessPolicyTest {
    private val policy = RoomAccessPolicy()

    @Test
    fun `활성 멤버가 아니면 방 범위 접근이 거절된다`() {
        // given
        val member = roomMember(status = RoomMemberStatus.REMOVED)

        // when
        val action = { policy.requireActiveMember(member) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ROOM_MEMBER_REQUIRED)
    }

    @Test
    fun `활성 멤버이면 방 범위 접근이 허용된다`() {
        // given
        val member = roomMember(status = RoomMemberStatus.ACTIVE)

        // when
        val action = { policy.requireActiveMember(member) }

        // then
        assertThatCode { action() }
            .doesNotThrowAnyException()
    }

    @Test
    fun `방장이 아니면 방장 권한이 거절된다`() {
        // given
        val member = roomMember(role = RoomRole.MEMBER)

        // when
        val action = { policy.requireOwner(member) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ROOM_OWNER_REQUIRED)
    }

    @Test
    fun `방장이면 방장 권한이 허용된다`() {
        // given
        val member = roomMember(role = RoomRole.OWNER)

        // when
        val action = { policy.requireOwner(member) }

        // then
        assertThatCode { action() }
            .doesNotThrowAnyException()
    }

    private fun roomMember(
        status: RoomMemberStatus = RoomMemberStatus.ACTIVE,
        role: RoomRole = RoomRole.MEMBER,
    ): RoomMember =
        RoomMember(
            id = UUID.randomUUID(),
            roomId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            role = role,
            status = status,
            slotIndex = 0,
            joinedAt = Instant.parse("2026-05-24T00:01:00Z"),
            removedAt = if (status == RoomMemberStatus.REMOVED) {
                Instant.parse("2026-05-24T01:00:00Z")
            } else {
                null
            },
        )
}
