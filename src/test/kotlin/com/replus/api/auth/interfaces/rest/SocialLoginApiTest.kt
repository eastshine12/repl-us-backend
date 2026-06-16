package com.replus.api.auth.interfaces.rest

import com.replus.api.auth.application.SocialLoginCommand
import com.replus.api.auth.application.port.SocialIdentityVerifier
import com.replus.api.auth.application.port.VerifiedSocialIdentity
import com.replus.api.auth.domain.model.AuthProvider
import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocialLoginApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `verified Google identity creates a bearer session for a non guest user`() {
        val session = mockMvc.perform(
            post("/api/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"provider":"GOOGLE","providerToken":"google-id-token"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.displayName").value("Google Friend"))
            .andExpect(jsonPath("$.user.avatarUrl").value("https://cdn.example.test/google.png"))
            .andExpect(jsonPath("$.user.isGuest").value(false))
            .andReturn()

        assertThat(session.response.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store")

        val sessionBody = objectMapper.readTree(session.response.contentAsString)
        val accessToken = sessionBody["accessToken"].asString()
        val userId = sessionBody["user"]["id"].asString()

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(userId))
            .andExpect(jsonPath("$.user.displayName").value("Google Friend"))
            .andExpect(jsonPath("$.user.isGuest").value(false))
            .andExpect(jsonPath("$.rooms").isArray)
            .andExpect(jsonPath("$.rooms").isEmpty)
    }

    @Test
    fun `verified Apple identity creates a bearer session with nullable profile fields`() {
        mockMvc.perform(
            post("/api/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"provider":"APPLE","providerToken":"apple-id-token"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.displayName").value("Member"))
            .andExpect(jsonPath("$.user.avatarUrl").doesNotExist())
            .andExpect(jsonPath("$.user.isGuest").value(false))
    }

    @Test
    fun `failed social identity verification is unauthorized`() {
        mockMvc.perform(
            post("/api/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"provider":"GOOGLE","providerToken":"bad-token"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    @Test
    fun `blank social provider token is rejected`() {
        mockMvc.perform(
            post("/api/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"provider":"GOOGLE","providerToken":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("providerToken"))
    }

    @Test
    fun `unknown social provider is rejected as a bad request`() {
        mockMvc.perform(
            post("/api/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"provider":"UNSUPPORTED","providerToken":"token"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @TestConfiguration
    class FakeSocialIdentityVerifierConfig {
        @Bean
        @Primary
        fun socialIdentityVerifier(): SocialIdentityVerifier = object : SocialIdentityVerifier {
            override fun verify(command: SocialLoginCommand): VerifiedSocialIdentity = when (command.providerToken) {
                "google-id-token" -> VerifiedSocialIdentity(
                    provider = AuthProvider.GOOGLE,
                    providerSubject = "google-123",
                    email = "google.friend@example.test",
                    displayName = "Google Friend",
                    avatarUrl = "https://cdn.example.test/google.png",
                )

                "apple-id-token" -> VerifiedSocialIdentity(
                    provider = AuthProvider.APPLE,
                    providerSubject = "apple-789",
                    email = null,
                    displayName = null,
                    avatarUrl = null,
                )

                else -> throw CoreException(ErrorType.UNAUTHENTICATED)
            }
        }
    }
}
