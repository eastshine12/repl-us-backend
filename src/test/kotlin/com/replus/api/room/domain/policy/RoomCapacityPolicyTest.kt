package com.replus.api.room.domain.policy

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RoomCapacityPolicyTest {
    private val policy = RoomCapacityPolicy()

    @Test
    fun `활성 멤버가 6명이면 초대 참여가 거절된다`() {
        // given
        val activeMemberCount = 6
        val maxMembers = 6

        // when
        val action = { policy.ensureCanJoin(activeMemberCount, maxMembers) }

        // then
        assertThatThrownBy { action() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ROOM_FULL)
    }

    @Test
    fun `활성 멤버가 정원보다 적으면 초대 참여가 허용된다`() {
        // given
        val activeMemberCount = 5
        val maxMembers = 6

        // when
        val action = { policy.ensureCanJoin(activeMemberCount, maxMembers) }

        // then
        assertThatCode { action() }
            .doesNotThrowAnyException()
    }
}
