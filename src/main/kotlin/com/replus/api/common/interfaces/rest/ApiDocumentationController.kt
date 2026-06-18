package com.replus.api.common.interfaces.rest

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApiDocumentationController {
    @GetMapping("/api-docs/openapi.yaml", produces = [OPENAPI_YAML_MEDIA_TYPE])
    fun openApiYaml(): ResponseEntity<Resource> {
        val resource = ClassPathResource("api/openapi.yaml")
        if (!resource.exists()) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(OPENAPI_YAML_MEDIA_TYPE))
            .body(resource)
    }

    private companion object {
        private const val OPENAPI_YAML_MEDIA_TYPE = "application/yaml"
    }
}
