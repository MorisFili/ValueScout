package derivative.valueScout.exceptions

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class CustomExceptionHandler {

    @ExceptionHandler(HttpRequestTimeout::class)
    fun timeoutHandler(exception: HttpRequestTimeout): ResponseEntity<String> {
        return ResponseEntity.status(408).body("Request timed out")
    }
}