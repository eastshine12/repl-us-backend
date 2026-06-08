package com.replus.api.room.interfaces.rest

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InviteCodeValidationApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `형식이 잘못된 초대 코드는 조회 전에 잘못된 요청으로 처리한다`() {
        mockMvc.perform(
            post("/api/invite-links/abc-12/join")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("Invite code format is invalid."))
    }
}
