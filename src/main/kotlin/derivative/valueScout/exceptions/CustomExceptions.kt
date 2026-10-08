package derivative.valueScout.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
class HttpRequestTimeout(message: String): RuntimeException(message)