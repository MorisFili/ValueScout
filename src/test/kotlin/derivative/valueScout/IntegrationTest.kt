package derivative.valueScout

import derivative.valueScout.dataModels.Domain
import derivative.valueScout.dataModels.Product
import derivative.valueScout.productScraper.Scraper
import derivative.valueScout.repository.DomainRepo
import derivative.valueScout.service.EndpointService
import derivative.valueScout.utility.Debug
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class IntegrationTest(
    @Autowired private val service: EndpointService,
    @Autowired private val scraper: Scraper,
    @Autowired private val domainRepo: DomainRepo
) {

    @Test
    fun integrationTest() {
        val queryParam  = "whey"

        //val results = service.search(queryParam)
        val result = mutableListOf<Product>()
        val domain = domainRepo.fetch("tillskottsbolaget.se") ?: Domain("tillskottsbolaget.se")

        val result1 = scraper.collectProducts(domain, queryParam)
        result.addAll(result1)

        result1.forEach { println(it) }

    }
}