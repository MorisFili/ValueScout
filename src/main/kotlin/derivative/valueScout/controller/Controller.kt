package derivative.valueScout.controller

import derivative.valueScout.dataModels.Product
import derivative.valueScout.service.EndpointService
import derivative.valueScout.utility.Debug
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(
    private val service: EndpointService
) {

    @PostMapping("/s")
    fun search(@RequestBody jsonBody: SearchRequest) : List<Product> {
        return service.search(jsonBody.query)
    }

    data class SearchRequest(val query: String)
}