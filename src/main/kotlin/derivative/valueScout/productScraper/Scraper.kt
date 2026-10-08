package derivative.valueScout.productScraper

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Request
import com.microsoft.playwright.options.WaitUntilState
import derivative.valueScout.dataModels.Domain
import derivative.valueScout.dataModels.Product
import derivative.valueScout.repository.DomainRepo
import derivative.valueScout.utility.Debug
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.regex.Pattern


@Service
class Scraper(
    private val domainRepo: DomainRepo,
    private val priceNodeFinder: PriceNodeFinder,
    private val cardNodeFinder: CardNodeFinder,
    private val httpClient: RestClient
) {

    fun collectProducts(domain: Domain, searchQuery: String): List<Product> {
        val queue = ArrayDeque<String>()

        val result = mutableListOf<Product>()

        val queryCode = domain.queryCode ?: run {
            val qC = findQueryCode(domain, searchQuery)
            if (qC != null) {
                domain.setQueryCode(qC)
                domainRepo.save(domain)
                qC
            } else {
                Debug.error("Missing query code for ${domain.domain}")
                return result
            }
        }

        var htmlBody: String

        if (!queryCode.startsWith("LAZY")) {
            val search = URLEncoder.encode(searchQuery, "UTF-8")
            val query = queryCode.replace("Lorem-Ipsum", search)

            htmlBody = httpClient.get().uri(query).retrieve().body<String>() ?: run {
                Debug.error("Error fetching htmlBody for request $query")
                return result
            }
        } else {
            htmlBody = usePlaywright(domain, queryCode, searchQuery)
        }

        File("rawHtml.html").writeText(htmlBody)

        val source = Jsoup.parse(htmlBody, "https://${domain.domain}")

        result.addAll(collectProductsFromPage(source, domain))

        while (true) { // Pagination solver
            val link = queue.removeFirstOrNull() ?: break
            val htmlBody = httpClient.get().uri(link).retrieve().body<String>() ?: continue
            result.addAll(collectProductsFromPage(Jsoup.parse(htmlBody), domain))
        }

        return result
    }

    private fun collectProductsFromPage(source: Document, activeDomain: Domain): List<Product> {

        val result = mutableListOf<Product>()

        if (activeDomain.priceElements.isNullOrEmpty()) {
            activeDomain.priceElements = priceNodeFinder.findPriceNode(source)
            if (!activeDomain.priceElements.isNullOrEmpty()) domainRepo.save(activeDomain)
        }

        if (activeDomain.cardElement.isNullOrEmpty() && !activeDomain.priceElements.isNullOrEmpty()) {
            val priceElements =
                activeDomain.priceElements ?: run { Debug.error("Strange bug at collector"); return result }
            activeDomain.cardElement = cardNodeFinder.findCardNode(source, priceElements)
            if (!activeDomain.cardElement.isNullOrEmpty()) domainRepo.save(activeDomain)
        }

        if (activeDomain.cardElement.isNullOrEmpty() || activeDomain.priceElements.isNullOrEmpty()) {
            if (activeDomain.cardElement.isNullOrEmpty()) Debug.error("Missing cardElement for ${activeDomain.domain}")
            if (activeDomain.priceElements.isNullOrEmpty()) Debug.error("Missing priceElement for ${activeDomain.domain}")
            return result
        }


        val cards = source.select("[class=\"${activeDomain.cardElement}\"]")
        val amtCards = cards.count()

        for (card in cards) {

            val product = Product()
            extractPriceAndCurrency(product, card, activeDomain)
            extractLinkAndName(product, card)
            extractImgLink(product, card)
            result.add(product)
        }

        // Collection code append to result

        return result
    }

    private fun extractImgLink(product: Product, element: Element) {
        val productName = product.name ?: product.linkToProduct ?: return

        for (element in element.select("img, source, image")) {
            for (attribute in element.attributes()) {
                val value = attribute.value.substringBefore(",").trim().substringBefore(" ")
                if (value.isBlank()) continue
                val cleanLink = validUriOrNull(value) ?: continue
                if (charSimilarity(productName, getSlug(cleanLink), 3) > 0.1) {
                    product.linkToImage = value
                    return
                }
            }
        }

    }

    private fun charSimilarity(s1: String, s2: String, n: Int = 2): Double {
        fun buildNgrams(s: String): Set<String> {
            val cleaned = s.filter { !it.isWhitespace() }
            if (cleaned.length < n) return emptySet()

            return (0..cleaned.length - n)
                .map { cleaned.substring(it, it + n) }
                .toSet()
        }

        val g1 = buildNgrams(s1)
        val g2 = buildNgrams(s2)

        if (g1.isEmpty() && g2.isEmpty()) return 0.0

        val intersection = g1.intersect(g2).size
        val union = g1.size + g2.size - intersection

        return intersection.toDouble() / union
    }

    private fun extractPriceAndCurrency(product: Product, element: Element, activeDomain: Domain) {
        var cheapestPrice = Double.MAX_VALUE

        for (priceNode in activeDomain.priceElements!!) {
            val price = element.selectFirst("[class^=\"$priceNode\"]") // ^ = starts with
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
                            product.currency = currency
                        }
                    }

                }

            }
            // Step 2 logic if it fails
        }
        product.price = cheapestPrice
    }

    fun extractLinkAndName(product: Product, element: Element) {
        val links = element.select("a[href]")
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

    private fun usePlaywright(domain: Domain, queryCode: String, query: String): String {
        Playwright.create().use { playwright ->
            val chromium = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(false)
            )

            val context = chromium.newContext()
            val page = context.newPage()

            page.onConsoleMessage { msg ->
                println("BROWSER: ${msg.text()}")
            }

            page.navigate(
                linkFromDomain(domain),
                Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(5_000.0)
            )

            var inputs = page.locator("input[class][placeholder]")
            if (inputs.count() == 0) inputs = page.locator("input[id][placeholder]")
            val count = inputs.count()

            for (i in 0 until count) {
                val input = inputs.nth(i)

                if (!input.isVisible || !input.isEnabled) continue

                input.fill(query)
                input.press("Enter")

            }

            page.waitForTimeout(1500.0) // dont decrease, js content generation is slow

            val selector = queryCode.substringAfter(":")
            if (selector.isNotEmpty()) scrollLazyToBottom(page, ".$selector")

            return page.content()
        }
    }

    private fun findQueryCode(domain: Domain, searchQuery: String): String? {

        Playwright.create().use { playwright ->
            val chromium = playwright.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(false)
            )

            val context = chromium.newContext()

            val page = context.newPage()

            page.navigate(
                linkFromDomain(domain),
                Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(5_000.0)
            )

            var inputs = page.locator("input[class][placeholder]")
            if (inputs.count() == 0) inputs = page.locator("input[id][placeholder]")
            val count = inputs.count()

            var browserHydratedDOMSize = 0

            for (i in 0 until count) {

                val input = inputs.nth(i)

                if (!input.isVisible || !input.isEnabled) continue

                val requests = mutableListOf<Request>()

                page.onRequest { request ->
                    if (request.url().contains('?') &&
                        sameDomain(request.url(), page) &&
                        request.url().contains("Lorem-Ipsum", ignoreCase = true)
                    ) {
                        requests.add(request)
                        Debug.log("Added: ${request.url()}")
                    }
                }

                page.waitForTimeout(1000.0)

                browserHydratedDOMSize = (page.evaluate(
                    "document.documentElement.outerHTML.length"
                ) as Number).toInt()
                Debug.log("Checking browser DOM size. Pre: $browserHydratedDOMSize")

                input.fill("Lorem-Ipsum")
                input.press("Enter")
                page.waitForTimeout(1000.0)

                val match = requests.firstOrNull {
                    it.url().contains("Lorem-Ipsum", ignoreCase = true)
                } ?: continue

                // check here if query actually produces usable DOM
                val initialDOMSize = Jsoup.parse(URI(match.url().replace
                    ("Lorem-Ipsum", searchQuery)).toURL(), 2000).html().length
                Debug.log("Checking raw request DOM size. Size: $initialDOMSize")

                if (initialDOMSize > browserHydratedDOMSize * 0.9) return match.url()
            }

            // No URL query could be found, page probably uses dynamic loading via response-injection
            // or something similar. Best solution: playwright emulation. Check if page responds to
            // emulation and mark query code with "LAZY"

            val lazySelector = detectBestContainer(page)

            if (lazySelector != null) return "LAZY:$lazySelector"


            Debug.error("${domain.domain} uses neither url querying nor responds to emulation.")
            return null

        }
    }

    private fun extractDomain(url: String): String? {
        return URI.create(url).host?.replace("www.", "")?.lowercase()
    }

    private fun linkFromDomain(domain: Domain): String {
        return "https://${domain.domain}"
    }

    private fun sameDomain(targetURL: String, page: Page): Boolean {
        return try {
            val currentHost = URI(page.url()).host
            val targetHost = URI(targetURL).host

            targetHost == currentHost || targetHost.endsWith(".$currentHost")
        } catch (_: Exception) { //
            false
        }
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

    private fun validUriOrNull(input: String?): String? {
        if (input == null) return null

        val raw = input.trim()
        if (raw.isEmpty()) return null

        if (raw.startsWith("(")) return null                  // e.g. (min-width: ...)
        if (raw.contains(" ")) return null                    // plain text / invalid URL
        if (raw.endsWith(":") && !raw.startsWith("http")) return null
        if (raw == "#" || raw.startsWith("#")) return null    // page anchors
        if (raw.startsWith("javascript:", ignoreCase = true)) return null
        if (raw.startsWith("data:", ignoreCase = true)) return null
        if (raw.startsWith("mailto:", ignoreCase = true)) return null
        if (raw.startsWith("tel:", ignoreCase = true)) return null

        val firstCandidate = raw.substringBefore(",").trim()

        return firstCandidate.split("\\s+".toRegex()).firstOrNull() ?: return null

    }

    private fun detectBestContainer(page: Page): String? {
        return page.evaluate(
            """
() => (async () => {
    const delay = ms => new Promise(r => setTimeout(r, ms));

    for (const el of [document.body, document.documentElement]) {
        el.style.setProperty('overflow', 'auto', 'important');
        el.style.setProperty('position', 'static', 'important');
        el.style.setProperty('height', 'auto', 'important');
    }

    const isScrollable = el => {
        const s = getComputedStyle(el);
        return (s.overflowY === 'auto' || s.overflowY === 'scroll') &&
               el.scrollHeight > el.clientHeight + 50;
    };

    const candidates = [...document.querySelectorAll('*')].filter(isScrollable);

    let bestIndex = -1;
    let bestScore = -1;
    
    const debug = [];

    for (let i = 0; i < candidates.length; i++) {
        const el = candidates[i];
        
        const before = el.querySelectorAll('*').length
        
        const step = Math.max(300, Math.floor(el.clientHeight * 0.8));

        for (let j = 0; j < 10; j++) {
            el.scrollTop += step;
            el.dispatchEvent(new Event('scroll', { bubbles: true }));
            await delay(200);
        }
        
        const after = el.querySelectorAll('*').length
        
        const diff = after - before;
        const score = diff / (before + 1);
        
        if (score > bestScore) {
            bestDiff = diff;
            bestIndex = i;
        }
        
        console.log("element:", el.className, "before:", before, "after:", after);
        
        el.scrollTop = 0;
        await delay(100);
        
    }

    const best = candidates[bestIndex];
    if (!best) return null;

    return best.className || best.tagName.toLowerCase();
})()
    """.trimIndent()
        ) as String?
    }

    private fun scrollLazyToBottom(page: Page, selector: String) {
        page.evaluate(
            """
(selector) => (async () => {
    const delay = ms => new Promise(r => setTimeout(r, ms));
    const str = selector.trim().split(/\s+/).join(".");
    const el =
        selector === 'html'
            ? document.documentElement
            : selector === 'body'
                ? document.body
                : document.querySelector(str);

    if (!el) return;
    
    for (const el of [document.body, document.documentElement]) {
        el.style.setProperty('overflow', 'auto', 'important');
        el.style.setProperty('position', 'static', 'important');
        el.style.setProperty('height', 'auto', 'important');
    }

    const isRoot = el === document.documentElement || el === document.body;

    const getTop = () => isRoot ? window.scrollY : el.scrollTop;
    const getHeight = () => isRoot
        ? Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)
        : el.scrollHeight;
    const getClient = () => isRoot ? window.innerHeight : el.clientHeight;

    const scrollStep = () => {
        const step = Math.max(300, Math.floor(getClient() * 0.8));
        if (isRoot) {
            window.scrollBy(0, step);
            window.dispatchEvent(new Event('scroll', { bubbles: true }));
        } else {
            el.scrollTop += step;
            el.dispatchEvent(new Event('scroll', { bubbles: true }));
        }
    };

    let stagnant = 0;

    while (stagnant < 4) {
        const beforeTop = getTop();
        const beforeHeight = getHeight();

        scrollStep();
        await delay(300);

        const afterTop = getTop();
        const afterHeight = getHeight();

        const moved = afterTop > beforeTop;
        const grew = afterHeight > beforeHeight;

        stagnant = (moved || grew) ? 0 : stagnant + 1;
    }

    await delay(200);
})(selector)
    """.trimIndent(), selector
        )
    }
}