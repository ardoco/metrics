package edu.kit.kastel.mcse.ardoco.metrics.rest

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.http.HttpStatus

class HandlerTest {
    private val handler = Handler()

    @Test
    fun illegalArgumentBecomesBadRequestTest() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("Beta must be greater than 0"), null, null)
        assertAll(
            Executable { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            Executable { assertEquals("Beta must be greater than 0", response.body) }
        )
    }

    @Test
    fun nullPointerBecomesBadRequestTest() {
        val response = handler.handle(NullPointerException(), null, null)
        assertAll(
            Executable { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            Executable { assertNull(response.body) }
        )
    }

    @Test
    fun anyOtherExceptionBecomesInternalServerErrorTest() {
        // Only exceptions Spring MVC does not define a status for reach this handler; the ones it does keep their own status because Handler
        // extends ResponseEntityExceptionHandler.
        val response = handler.handle(IllegalStateException("boom"), null, null)
        assertAll(
            Executable { assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode) },
            Executable { assertEquals("boom", response.body) }
        )
    }

    @Test
    fun nullExceptionIsToleratedTest() {
        val response = handler.handle(null, null, null)
        assertAll(
            Executable { assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode) },
            Executable { assertNull(response.body) }
        )
    }
}
