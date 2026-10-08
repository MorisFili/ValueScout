package derivative.valueScout

import com.microsoft.playwright.Page
import derivative.valueScout.dataModels.Domain
import derivative.valueScout.dataModels.Product
import derivative.valueScout.productScraper.Scraper
import derivative.valueScout.repository.DomainRepo
import derivative.valueScout.utility.Debug
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@SpringBootTest
class CollectProductTest(
    @Autowired private val scraper: Scraper,
    @Autowired private val domainRepo: DomainRepo
) {

    @Test
    fun collectProductTest() {
        val domain = domainRepo.fetch("gymgrossisten.com")!!

        val debug = """

        <!-- dwMarker="product" dwContentID="29a64015e7ea167d6f25b6f995" -->






    
    
    <div class="product-tile" data-pid="5751R" data-sibling-color="" data-tracking-click-event="productClick" data-tracking-event-payload="{&quot;currencyCode&quot;:&quot;SEK&quot;,&quot;click&quot;:{&quot;actionField&quot;:{},&quot;products&quot;:[{&quot;id&quot;:&quot;5751R&quot;,&quot;name&quot;:&quot;Whey-100 Vassleprotein 4 kg&quot;,&quot;brand&quot;:&quot;Star Nutrition&quot;,&quot;category&quot;:&quot;Vassleprotein&quot;,&quot;price&quot;:&quot;1399.00&quot;,&quot;variant&quot;:&quot;master&quot;}]}}">

        
            <div class="show-for-large promotion-badge">
                <div class="product-tile-promotions-right">
                    <div class="promotion-wrapper">
    <div class="promotions-right">
        
            
                
                    <div class="promotion custom">
                        <span>Toppsäljare</span>
                    </div>
                
            
                
                    <div class="promotion sale">
                        <span>10%</span>
                    </div>
                
            
        
    </div>
</div>

                </div>
            </div>
            
                <div class="wishlist-wrapper hidden" data-wishlist-tile-item="" data-add-to-wish-wrapper="" data-add-to-wish-product-id="5751R">
    <a href="/on/demandware.store/Sites-Gymgrossisten-Site/sv_SE/Wishlist-AddProduct" title="Lägg till produkten i din önskelista" class="wishlist-icon" aria-label="Önskelista" data-wishlist-product-id="5751R" data-add-to-wish-list="" data-wishlist-inlist="false" data-wishlist-addtowishlisturl="/on/demandware.store/Sites-Gymgrossisten-Site/sv_SE/Wishlist-AddProduct" data-wishlist-removefromwishlisturl="/on/demandware.store/Sites-Gymgrossisten-Site/sv_SE/Wishlist-RemoveProduct">
        <i class="icon-wishlist"></i>
    </a>
</div>

<script>
    window.setWishlistState && window.setWishlistState('5751R');
</script>

            
        

        <div class="product-tile-image__container">
    <div class="product-tile-image__container-item" role="presentation">
        <a class="product-tile-image-link " href="/whey-100-vassleprotein-4-kg/5751R.html">
            
            
                
                    <img class="product-tile-image lazy-loaded" src="https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.jpg?sw=266&amp;sh=266&amp;sm=fit&amp;sfrm=jpg" data-src="https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.jpg?sw=266&amp;sh=266&amp;sm=fit&amp;sfrm=jpg" data-srcset="https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=82&amp;sh=82&amp;sm=fit&amp;sfrm=jpg 82w,https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=123&amp;sh=123&amp;sm=fit&amp;sfrm=jpg 123w,https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=266&amp;sh=266&amp;sm=fit&amp;sfrm=jpg 266w" srcset="https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=82&amp;sh=82&amp;sm=fit&amp;sfrm=jpg 82w,https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=123&amp;sh=123&amp;sm=fit&amp;sfrm=jpg 123w,https://www.gymgrossisten.com/dw/image/v2/BDJH_PRD/on/demandware.static/-/Sites-hsng-master-catalog/default/dw682b7bfb/media/GG-Produktbilder/Star-Nutrition/test/whey100_4kg_1.png?sw=266&amp;sh=266&amp;sm=fit&amp;sfrm=jpg 266w" sizes="(min-width: 1024px) 20vw, (min-width: 768px) 123px, 82px" alt="Whey-100 Vassleprotein 4 kg Choklad" title="Star Nutrition Whey 100 4 kg Choklad " loading="lazy">
                
            
        </a>
    </div>
    
    
        
            
            <div class="product-tile-num-of-variants-wrapper-mobile hide-for-large-inline">
    
        <span class="product-tile-num-of-variants">+ 9 varianter</span>
    
</div>

        
    
</div>

        <div class="product-tile-description">
            <div class="hide-for-large product-tile-promotions-right">
                <div class="promotion-wrapper">
    <div class="promotions-right">
        
            
                
                    <div class="promotion custom">
                        <span>Toppsäljare</span>
                    </div>
                
            
                
                    <div class="promotion sale">
                        <span>10%</span>
                    </div>
                
            
        
    </div>
</div>

            </div>
            <div class="product-tile-description-top">
                



    <a class="link " href="/whey-100-vassleprotein-4-kg/5751R.html">
        <p class="product-tile-name">Whey-100 Vassleprotein 4 kg</p>
    </a>


            </div>
            
                <div class="product-tile-top-rating-variant">
                    
                    
                        
                            
                            <div class="product-tile-num-of-variants-wrapper">
    
        <span class="product-tile-num-of-variants">+ 9 varianter</span>
    
</div>

                        
                    

                    
                    <div class="product-tile-rating">
                        
                            
    <div class="rating-wrapper ">
        <div class="star-rating">
            <span style="--rating: 4.5"></span>
        </div>
        <span class="review-count">
            173 recensioner
        </span>
    </div>

                        
                    </div>
                </div>
                <div>
                    
                        
                            <a role="button" class="product-tile-brand" href="https://www.gymgrossisten.com/allavarumarken/star-nutrition">Star Nutrition</a>
                        
                    
                </div>
                
            
        </div>
        
            <div class="product-tile-CTA  ">
                
                    
                    
                        <div class="price">
    
    
        





    
    <div class="price-adjusted">
        
        
            1.399 kr
        
    </div>

    




    
    
    
    <div class="price-ref">
        
        <div class="price-ref__stmt-container  price-ref__stmt-container--visible-discount">

            
                <span class="price-ref__stmt price-ref__stmt--lowest">
                    <span class="price__prefix">Lägsta pris</span>
                    <span class="price__value">
                        
                        1.549 kr
                    </span>
                    
                        <span class="price__discount price__discount--percentage">(-10%)</span>
                    
                </span>
            

            
        </div>
    </div>




    
</div>

    




                    
                    
                        <a role="button" href="#" class="button product-tile-buy button--small show quickview show-for-large" data-href="/on/demandware.store/Sites-Gymgrossisten-Site/sv_SE/Product-ShowQuickView?pid=5751R">
                            
                                Köp
                            
                        </a>
                        <div class="hidden">
                            <div class="product-usps">
                                <div class="product-usps">
	<div class="product-usps-item"> <i class="icon-delivery"></i>

		<div class="product-usps-text">Fri frakt över 499 kr</div>
	</div>
	<div class="product-usps-item"> <i class="icon-return"></i>

		<div class="product-usps-text">Fri retur</div>
	</div>
	<div class="product-usps-item"> <i class="icon-price-tag"></i>

		<div class="product-usps-text">Prisgaranti</div>
	</div>
</div>
                            </div>
                            <div class="PDP-out-of-stock-popup-consent">
                                <div class="product-outofstock-popup-consent">Vi kontaktar dig när varan finns åter i lager. Information om hur vi hanterar din personliga data hittar du i vår <a class="product-outofstock-popup-consent-link" target="_blank" href="https://www.gymgrossisten.com/data-protection-notice.html">Dataskyddsinformation</a>.</div>
                            </div>
                            <div class="PDP-out-of-stock-disclaimer">
                                <div class="product-outofstock">
<div class="product-outofstock-circle">!</div>

<div class="product-outofstock-text">Denna produkt är tillfälligt slut i lager. Få en&nbsp;notifikation när produkter åter finns i lager.</div>
</div>
                            </div>
                        </div>
                    
                    <div class="price-ref-wrapper">
                        
                            
                            
                            
    <div class="price-ref">
        
        <div class="price-ref__stmt-container  price-ref__stmt-container--visible-discount">

            
                <span class="price-ref__stmt price-ref__stmt--lowest">
                    <span class="price__prefix">Lägsta pris</span>
                    <span class="price__value">
                        
                        1.549 kr
                    </span>
                    
                        <span class="price__discount price__discount--percentage">(-10%)</span>
                    
                </span>
            

            
        </div>
    </div>


                        
                        
                    </div>
                    
                        <a role="button" class="button product-tile-buy button--small show hide-for-large " href="/whey-100-vassleprotein-4-kg/5751R.html">
                            
                                Köp
                            
                        </a>
                    
                
            </div>
            <div>
                
            </div>
        
    </div>

<!-- END_dwmarker -->

    
        """.trimIndent()

        //val document = Jsoup.connect("https://www.gymgrossisten.com/kosttillskott").get()

        val document = Jsoup.parse(debug, "https://www.gymgrossisten.com")

        var counter = 0;
        val productList = mutableListOf<Product>()

        for (product in document.select("[class=\"${domain.cardElement}\"]")) {
            counter++
            val item = Product()
            extractPriceAndCurrency(item, product, domain)
            //extractLinkAndName(item, product)
            productList.add(item)
        }

        println("Products found: $counter")

        productList.forEach { println(it) }


    }

    fun extractPriceAndCurrency(product: Product, element: Element, domain: Domain) {
        var cheapestPrice = Double.MAX_VALUE


        for (priceNode in domain.priceElements!!) {
            val price = element.selectFirst("[class^=\"${priceNode}\"]")
            if (price != null) {
                var text = price.text()
                    .replace('\u00A0', ' ')
                    .replace('\u2009', ' ')
                    .replace('\u202F', ' ')
                text = text.replace("(?i)(kr|sek|eur|€|usd|\\$|£|gbp|:-)([0-9])".toRegex(), "$1 $2")
                text = text.replace("(?i)([0-9])(kr|sek|eur|€|usd|\\$|£|gbp|:-)".toRegex(), "$1 $2")

                // removes space between digit following three digits. fix for blank used as a thousand separator
                // example: 1 500 -> 1500
                text = text.replace("([0-9])\\s+(?=[0-9]{3}(?!\\d))".toRegex(), "$1")

                val matcher = PRICE_PATTERN.matcher(text)

                if (matcher.find()) {
                    val tokens = matcher.group().split("\\s+".toRegex())
                    for (token in tokens) {
                        if (token.matches("^[0-9]+([.,][0-9]+)?$".toRegex())) { // Is value?
                            val productPrice = token.replace(',', '.').toDouble()
                            if (productPrice < cheapestPrice) cheapestPrice = productPrice
                        } else if (token.matches("(?i)(kr|sek|eur|€|usd|\\$|£|gbp|:-)".toRegex())) { // Is currency?
                            val currency = if (token.contains(":-")) "SEK" else token.uppercase()
                            if (product.currency.isNullOrEmpty()) product.currency = currency
                        }
                    }

                }

            }
        }
        // Step 2 logic if it fails

        product.price = cheapestPrice

    }

    fun extractLinkAndName(product: Product, element: Element) {
        val links = element.select("a[href]")

        // Replace all non-digit/letter with space


        var bestLink: Element? = null
        var bestScore = -1.0
        var bestName = ""

        for (link in links) {
            val temp = link.absUrl("href")

            if (tokenize(getSlug(temp)).size < 2) continue

            for (childElement in element.select("[class]")) {
                if (!childElement.text().isNullOrEmpty()) {
                    val cleanText = childElement.text().trim().replace("[^\\p{L}0-9-]+".toRegex(), " ")
                    val childNodeScore = tokenizeAndCalculateSimilarityScore(
                        cleanText, getSlug(temp)
                    )
                    if (childNodeScore >= bestScore) {
                        bestScore = childNodeScore
                        bestName = cleanText // either this or raw childElement.text()
                        bestLink = link
                    }
                }
            }
        }

        product.name = bestName.ifEmpty { null }
        product.linkToProduct = bestLink?.absUrl("href")

    }

    private fun linkFromDomain(domain: Domain): String {
        return "https://${domain.domain}"
    }

    private fun sameDomain(targetURL: String, page: Page): Boolean {
        val currentHost = URI(page.url()).host
        val targetHost = URI(targetURL).host

        return targetHost == currentHost || targetHost.endsWith(".$currentHost")
    }

    private fun tokenize(input: String): List<String> {
        val cleaned = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .replace("\u00A0", " ") // remove weird blanks
            .replace('\u2009', ' ')
            .replace('\u202F', ' ')
            .lowercase()
            .replace("[^a-z0-9.,]+".toRegex(), " ")
        return cleaned.split("\\s+".toRegex())
    }

    private fun getSlug(input: String): String {
        var uri: URI
        try {
            uri = URI.create(input)
        } catch (e: IllegalArgumentException) {
            Debug.error("Error parsing: $input. Cause: ${e.message ?: "Unknown"}")
            return ""
        }
        var path = uri.path ?: return ""

        if (path.isBlank()) return ""

        path = path.replace("\\.html?.*$".toRegex(), "")

        val segments = path.split("/")
            .filter { it.isNotBlank() }

        if (segments.isEmpty()) return ""

        val last = segments.last()
        val prev = segments.getOrNull(segments.size - 2)

        val lastScore = slugScore(last.split("-"))
        val prevScore = slugScore(prev?.split("-") ?: listOf())

        return if (prev != null && prevScore > lastScore) prev else last
    }

    private fun slugScore(tokens: List<String>): Int {
        var score = 0;

        tokens.forEach { token ->
            val letters = token.count { it.isLetter() }
            val digits = token.count { it.isDigit() }
            val length = token.length

            val isWordLike = letters >= 3 && digits == 0
            val isMostlyNumeric = digits > length
            val isMixed = digits > 0 && length > 0 && !isMostlyNumeric

            if (isWordLike) score += 3
            if (isMixed) score += 1
            if (isMostlyNumeric) score -= 1
        }

        return score
    }

    private fun tokenizeAndCalculateSimilarityScore(words1: String, words2: String): Double {

        fun score(a: String, b: String): Double {
            val set1 = tokenize(a).toSet()
            val set2 = tokenize(b).toSet()

            if (set1.isEmpty() || set2.isEmpty()) return 0.0

            val intersectionSize = set1.intersect(set2).size.toDouble()
            val biggerSetSize = maxOf(set1.size, set2.size)
            val smallerSetSize = minOf(set1.size, set2.size)

            return (intersectionSize / biggerSetSize) * smallerSetSize
        }

        val quantityRegex = "(?i)\\b\\d+([.,]\\d+)?\\s*(KG|G|MG|L|ML|LBS?)\\b".toRegex()

        val score1 = score(words1, words2)
        val score2 = score(
            words1.replace(quantityRegex, "").trim(),
            words2.replace(quantityRegex, "").trim()
        )

        return maxOf(score1, score2)
    }

    private val PRICE_PATTERN = Pattern.compile(
        "(?i)(?:kr|sek|eur|€|usd|\\$|£|gbp)\\s*\\d+(?:[.,]\\d+)*|\\d+(?:[.,]\\d+)*\\s*(?:kr|sek|eur|€|usd|\\$|£|gbp|:-)",
        Pattern.CASE_INSENSITIVE
    )

    private val PRICE_PATTERN_ALT = Pattern.compile(
        "(?:kr|sek|eur|€|usd|\\$|£|gbp|:-)?\\s*\\d+(?:[.,]\\d+)*\\s*(?:kr|sek|eur|€|usd|\\$|£|gbp|:-)",
        Pattern.CASE_INSENSITIVE
    )
}