package com.replus.api.common.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.mock.env.MockEnvironment

class DatabaseUrlEnvironmentPostProcessorTest {
    private val postProcessor = DatabaseUrlEnvironmentPostProcessor()

    @Test
    fun `converts render postgres database url into spring datasource properties`() {
        val environment = MockEnvironment()
            .withProperty(
                "DATABASE_URL",
                "postgresql://render_user:p%40ssword@repl-us-postgres:5432/replus?sslmode=require",
            )

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertThat(environment.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://repl-us-postgres:5432/replus?sslmode=require")
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("render_user")
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("p@ssword")
    }

    @Test
    fun `does not override explicit spring datasource url`() {
        val environment = MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://manual-db:5432/replus")
            .withProperty("spring.datasource.username", "manual_user")
            .withProperty("spring.datasource.password", "manual_password")
            .withProperty("DATABASE_URL", "postgresql://render_user:secret@render-db:5432/replus")

        postProcessor.postProcessEnvironment(environment, SpringApplication())

        assertThat(environment.getProperty("spring.datasource.url"))
            .isEqualTo("jdbc:postgresql://manual-db:5432/replus")
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("manual_user")
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("manual_password")
    }

    @Test
    fun `rejects unsupported database url scheme`() {
        val environment = MockEnvironment()
            .withProperty("DATABASE_URL", "mysql://user:secret@db:3306/replus")

        assertThatThrownBy {
            postProcessor.postProcessEnvironment(environment, SpringApplication())
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("DATABASE_URL must use postgresql://")
    }
}
