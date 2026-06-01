package com.replus.api.common.error

class CoreException(
    val errorType: ErrorType,
    override val message: String = errorType.defaultMessage,
    val details: Map<String, Any?> = emptyMap(),
) : RuntimeException(message)
