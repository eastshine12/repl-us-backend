package com.replus.api.room.domain.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class OpaqueInviteCodeGeneratorTest {
    @Test
    fun `초대 코드는 방 식별자를 노출하지 않는 opaque 값이다`() {
        // given
        val roomId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa")
        val generator = OpaqueInviteCodeGenerator()

        // when
        val code = generator.generate(roomId)

        // then
        assertThat(code).containsPattern("^[A-Z0-9]{6,12}$")
        assertThat(code.contains("-")).isFalse()
        assertThat(roomId.toString().contains(code)).isFalse()
    }
}
