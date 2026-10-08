package derivative.valueScout

import derivative.valueScout.dataModels.DomainCategories
import derivative.valueScout.productScraper.CardNodeFinder
import derivative.valueScout.repository.DomainRepo
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals


@SpringBootTest
class CardWrapperFinderTest(
    @Autowired private val cardNodeFinder: CardNodeFinder
) {

    @Test
    fun cardWrapperFinderTest() {

        val url = "https://www.gymgrossisten.com/search?q=whey&lang=sv_SE"

        val doc = Jsoup.connect(url).get()

        val result = cardNodeFinder.findCardNode(doc, listOf(
            "price__value", "price-adjusted", "price-sales"
        ))

        println(result)
    }

}