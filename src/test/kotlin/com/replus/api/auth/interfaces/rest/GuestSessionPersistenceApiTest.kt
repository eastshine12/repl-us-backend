package com.replus.api.auth.interfaces.rest

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuestSessionPersistenceApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `guest sessions are persisted with a hashed token`() {
        val session = createGuestSession()
        val accessToken = session.accessToken
        entityManager.flush()

        val tokenHash = jdbcTemplate.queryForObject(
            "select token_hash from user_sessions where user_id = ?",
            String::class.java,
            session.userId,
        )

        assertThat(tokenHash).hasSize(64)
        assertThat(tokenHash).isNotEqualTo(accessToken)
        assertThat(tokenHash).doesNotContain(accessToken.takeLast(12))

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(session.userId.toString()))
    }

    @Test
    fun `expired persisted sessions are rejected`() {
        val session = createGuestSession()
        entityManager.flush()

        jdbcTemplate.update(
            "update user_sessions set expires_at = ? where user_id = ?",
            Timestamp.from(Instant.now().minusSeconds(60)),
            session.userId,
        )
        entityManager.clear()

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
    }

    private fun createGuestSession(): CreatedSession {
        val result = mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"Persistent Guest"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val body = objectMapper.readTree(result.response.contentAsString)
        return CreatedSession(
            accessToken = body["accessToken"].asString(),
            userId = UUID.fromString(body["user"]["id"].asString()),
        )
    }

    private data class CreatedSession(
        val accessToken: String,
        val userId: UUID,
    )
}
