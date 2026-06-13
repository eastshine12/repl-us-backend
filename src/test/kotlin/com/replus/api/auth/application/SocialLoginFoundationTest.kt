package com.replus.api.auth.application

import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocialLoginFoundationTest {
    @Autowired
    private lateinit var authFacade: AuthFacade

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `verified social identity creates a non guest user with a provider account and bearer session`() {
        val session = authFacade.loginWithSocialProvider(
            SocialLoginCommand(
                provider = AuthProvider.KAKAO,
                providerToken = "kakao-new-token",
            ),
        )

        assertThat(session.user.isGuest).isFalse()
        assertThat(session.user.displayName).isEqualTo("Kakao Friend")
        assertThat(session.user.avatarUrl).isEqualTo("https://cdn.example.test/kakao-friend.png")
        entityManager.flush()
        assertThat(providerAccountCount(AuthProvider.KAKAO, "kakao-123")).isEqualTo(1)

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(session.user.id.toString()))
            .andExpect(jsonPath("$.user.isGuest").value(false))
    }

    @Test
    fun `verified social identity reuses an existing provider account`() {
        val first = authFacade.loginWithSocialProvider(
            SocialLoginCommand(
                provider = AuthProvider.APPLE,
                providerToken = "apple-returning-token",
            ),
        )
        val second = authFacade.loginWithSocialProvider(
            SocialLoginCommand(
                provider = AuthProvider.APPLE,
                providerToken = "apple-returning-token",
            ),
        )

        assertThat(second.user.id).isEqualTo(first.user.id)
        entityManager.flush()
        assertThat(providerAccountCount(AuthProvider.APPLE, "apple-789")).isEqualTo(1)
        assertThat(sessionCount(first.user.id.toString())).isEqualTo(2)
    }

    private fun providerAccountCount(provider: AuthProvider, providerSubject: String): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from auth_provider_accounts where provider = ? and provider_subject = ?",
            Long::class.java,
            provider.name,
            providerSubject,
        ) ?: 0

    private fun sessionCount(userId: String): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from user_sessions where user_id = ?",
            Long::class.java,
            java.util.UUID.fromString(userId),
        ) ?: 0

    @TestConfiguration
    class FakeSocialIdentityVerifierConfig {
        @Bean
        @Primary
        fun socialIdentityVerifier(): SocialIdentityVerifier = object : SocialIdentityVerifier {
            override fun verify(command: SocialLoginCommand): VerifiedSocialIdentity = when (command.providerToken) {
                "kakao-new-token" -> VerifiedSocialIdentity(
                    provider = AuthProvider.KAKAO,
                    providerSubject = "kakao-123",
                    email = "kakao.friend@example.test",
                    displayName = "Kakao Friend",
                    avatarUrl = "https://cdn.example.test/kakao-friend.png",
                )

                "apple-returning-token" -> VerifiedSocialIdentity(
                    provider = AuthProvider.APPLE,
                    providerSubject = "apple-789",
                    email = "apple.friend@example.test",
                    displayName = "Apple Friend",
                    avatarUrl = null,
                )

                else -> error("Unexpected test provider token: ${command.providerToken}")
            }
        }
    }
}
