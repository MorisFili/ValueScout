package derivative.valueScout.service;

import derivative.valueScout.dataModels.DomainCategories
import derivative.valueScout.dataModels.Product
import derivative.valueScout.productScraper.Scraper
import derivative.valueScout.repository.DomainRepo
import derivative.valueScout.utility.Debug
import derivative.valueScout.utility.Vocabulary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient
import java.util.concurrent.ConcurrentHashMap

@Service
class EndpointService(
    private val domainRepo: DomainRepo,
    private val scraper: Scraper
) {

    fun search(searchParam: String): List<Product> {
        /*1. Classify the category of the search parameter
        * 2. Fetch all domains for said category
        * 3. For-each async each domain search request
        * 4. Collect products in concurrent list and return */

        val category = classifyParams(searchParam)
        Debug.log("Category: $category")

        val categorySites = domainRepo.fetchAllByCategory(category) // List<Domain>
        Debug.log("Category sites size: ${categorySites.size}")

//        val products = runBlocking {
//            val semaphore = Semaphore(8)
//            coroutineScope {
//                categorySites.map { site ->
//                    async(Dispatchers.IO) {
//                        semaphore.withPermit {
//                            Debug.log("Searching: ${site.domain}")
//                            scraper.collectProducts(site, searchParam)
//                        }
//                    }
//                }.awaitAll().flatten().toSet()
//            }
//        }

        val products = mutableSetOf<Product>()

        categorySites.forEach {
            Debug.log("Searching: ${it.domain}")
            val results = scraper.collectProducts(it, searchParam)
            products.addAll(results)
        }




        Debug.log("Async function complete! Product size: ${products.size}")
        return products.sortedBy { it.price }
    }

    private fun classifyParams(searchParams: String): DomainCategories {

        val tokenized = searchParams.trim()
            .split("\\s+".toRegex()).distinct() // unique keywords only

        val scores = DomainCategories.entries.associateWith { 0 }.toMutableMap()

        for (token in tokenized) {
            if (token in Vocabulary.ELECTRONICS)
                scores.merge(DomainCategories.ELECTRONICS, 1, Int::plus)
            if (token in Vocabulary.SUPPLEMENTS)
                scores.merge(DomainCategories.SUPPLEMENTS, 1, Int::plus)
            if (token in Vocabulary.CLOTHES)
                scores.merge(DomainCategories.CLOTHES, 1, Int::plus)
            if (token in Vocabulary.GENERAL)
                scores.merge(DomainCategories.GENERAL, 1, Int::plus)
        }

        val best = scores.maxByOrNull { it.value } ?: return DomainCategories.UNCLASSIFIED
        return if (best.value == 0) DomainCategories.UNCLASSIFIED else best.key

    }


}
