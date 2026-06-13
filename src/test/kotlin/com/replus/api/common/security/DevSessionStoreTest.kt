package com.replus.api.common.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Instant
import java.util.UUID

class DevSessionStoreTest {
    private val contextRunner = ApplicationContextRunner()
        .withPropertyValues("replus.auth.session-store=memory")
        .withBean(DevSessionStore::class.java)

    @Test
    fun `fixed dev tokens are resolved by default for local development`() {
        contextRunner.run { context ->
            val store = context.getBean(DevSessionStore::class.java)

            assertThat(store.resolve("dev-token-mina", Instant.now())).isEqualTo(DevSessionStore.MINA_USER_ID)
        }
    }

    @Test
    fun `fixed dev tokens can be disabled while guest sessions still resolve`() {
        contextRunner
            .withPropertyValues("replus.auth.dev-fixed-tokens-enabled=false")
            .run { context ->
                val store = context.getBean(DevSessionStore::class.java)
                val guestUserId = UUID.randomUUID()

                val guestToken = store.register(guestUserId, Instant.now().plusSeconds(60))

                assertThat(store.resolve("dev-token-mina", Instant.now())).isNull()
                assertThat(store.resolve(guestToken, Instant.now())).isEqualTo(guestUserId)
            }
    }

    @Test
    fun `expired guest sessions are rejected`() {
        contextRunner.run { context ->
            val store = context.getBean(DevSessionStore::class.java)
            val guestUserId = UUID.randomUUID()

            val guestToken = store.register(guestUserId, Instant.parse("2026-06-14T00:00:00Z"))

            assertThat(store.resolve(guestToken, Instant.parse("2026-06-14T00:00:01Z"))).isNull()
        }
    }
}
