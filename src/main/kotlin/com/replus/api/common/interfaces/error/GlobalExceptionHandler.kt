package com.replus.api.common.interfaces.error

import com.replus.api.common.error.CoreException
import com.replus.api.common.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(
        exception: CoreException,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetailsResponse> {
        val status = exception.errorType.toHttpStatus()
        if (status.is5xxServerError) {
            log.error("core_exception path={} code={}", request.requestURI, exception.errorType.code, exception)
        } else {
            log.info("core_exception path={} code={} detail={}", request.requestURI, exception.errorType.code, exception.message)
        }

        return ResponseEntity
            .status(status)
            .body(exception.errorType.toProblem(status, exception.message ?: exception.errorType.defaultMessage))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<ProblemDetailsResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors.map {
            FieldErrorResponse(
                field = it.field,
                message = it.defaultMessage ?: "Invalid value.",
            )
        }

        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity
            .status(status)
            .body(
                ProblemDetailsResponse(
                    title = status.reasonPhrase,
                    status = status.value(),
                    detail = ErrorType.INVALID_REQUEST.defaultMessage,
                    code = ErrorType.INVALID_REQUEST.code.name,
                    fieldErrors = fieldErrors,
                ),
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessageException(): ResponseEntity<ProblemDetailsResponse> {
        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity
            .status(status)
            .body(ErrorType.INVALID_REQUEST.toProblem(status, ErrorType.INVALID_REQUEST.defaultMessage))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ProblemDetailsResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        log.error("unexpected_exception path={}", request.requestURI, exception)
        return ResponseEntity
            .status(status)
            .body(ErrorType.INTERNAL_ERROR.toProblem(status, ErrorType.INTERNAL_ERROR.defaultMessage))
    }

    private fun ErrorType.toProblem(status: HttpStatus, detail: String): ProblemDetailsResponse =
        ProblemDetailsResponse(
            title = status.reasonPhrase,
            status = status.value(),
            detail = detail,
            code = code.name,
            fieldErrors = null,
        )

    private fun ErrorType.toHttpStatus(): HttpStatus =
        when (this) {
            ErrorType.UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED
            ErrorType.ROOM_MEMBER_REQUIRED,
            ErrorType.ROOM_OWNER_REQUIRED,
            -> HttpStatus.FORBIDDEN
            ErrorType.RESOURCE_NOT_FOUND,
            ErrorType.INVITE_LINK_NOT_FOUND,
            -> HttpStatus.NOT_FOUND
            ErrorType.INVALID_REQUEST,
            ErrorType.INVALID_DURATION,
            ErrorType.INVALID_AUDIO_REQUIRED,
            -> HttpStatus.BAD_REQUEST
            ErrorType.ROOM_FULL,
            ErrorType.INVITE_LINK_EXPIRED,
            ErrorType.INVITE_LINK_USAGE_LIMIT_REACHED,
            ErrorType.MISSION_EDIT_LIMIT_REACHED,
            ErrorType.MISSION_ALREADY_HAS_RESPONSE,
            ErrorType.RESPONSE_ALREADY_EXISTS,
            ErrorType.RESPONSE_RELEASE_LOCKED,
            ErrorType.RESPONSE_NOT_VISIBLE,
            ErrorType.CANNOT_REMOVE_OWNER,
            -> HttpStatus.CONFLICT
            ErrorType.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
