package com.replus.api.common.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class DevSessionStoreTest {
    private val contextRunner = ApplicationContextRunner()
        .withBean(DevSessionStore::class.java)

    @Test
    fun `fixed dev tokens are resolved by default for local development`() {
        contextRunner.run { context ->
            val store = context.getBean(DevSessionStore::class.java)

            assertThat(store.resolve("dev-token-mina")).isEqualTo(DevSessionStore.MINA_USER_ID)
        }
    }

    @Test
    fun `fixed dev tokens can be disabled while guest sessions still resolve`() {
        contextRunner
            .withPropertyValues("replus.auth.dev-fixed-tokens-enabled=false")
            .run { context ->
                val store = context.getBean(DevSessionStore::class.java)
                val guestUserId = UUID.randomUUID()

                val guestToken = store.register(guestUserId)

                assertThat(store.resolve("dev-token-mina")).isNull()
                assertThat(store.resolve(guestToken)).isEqualTo(guestUserId)
            }
    }
}
