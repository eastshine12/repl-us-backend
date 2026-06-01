package com.replus.api.common.interfaces.error

data class ProblemDetailsResponse(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String,
    val code: String,
    val fieldErrors: List<FieldErrorResponse>? = null,
)

data class FieldErrorResponse(
    val field: String,
    val message: String,
)
