package edu.kit.kastel.mcse.ardoco.metrics.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Maps exceptions to responses.
 *
 * Extending [ResponseEntityExceptionHandler] keeps the status codes that Spring MVC already defines for its own exceptions, so an unknown path stays a
 * 404, an unsupported method a 405 and a malformed request body a 400. Only exceptions that Spring does not know about reach the handlers below.
 */
@ControllerAdvice
class Handler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(Exception::class)
    fun handle(
        ex: Exception?,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ): ResponseEntity<Any> {
        if (ex is NullPointerException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex?.message)
    }

    /**
     * Invalid input, e.g. a beta that is not greater than 0 or a number of weights that does not match the number of requests, is a client error.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)
}
