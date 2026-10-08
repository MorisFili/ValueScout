package derivative.valueScout.productScraper

import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.net.URI
import java.text.Normalizer
import java.util.*

@Component
class CardNodeFinder {

    fun findCardNode(source: Document, priceNodeList: List<String>): String? {
        val wrapperMap = HashMap<String, Metadata>()

        var result: String? = null

        for (priceNode in priceNodeList) {
            cardLoop@ for (element in source.select("[class^=\"$priceNode\"]")) {
                var currentElement = element

                repeat(6) {
                    for (hasLink in currentElement.select("a[href]")) {
                        if (hasLink.attr("href").length > 7 &&
                            !hasLink.attr("href").lowercase().contains("javascript:")
                        ) {

                            val textTokens = if (!currentElement.text().isNullOrEmpty())
                                tokenize(currentElement.text()) else break
                            val hrefSlug = getSlug(hasLink.attr("href"))
                            val hrefTokens = tokenize(hrefSlug)

                            val isSimilar = isSimilar(textTokens, hrefTokens)

                            if (isSimilar) {
                                var depth = 0;
                                while (currentElement.className().isNullOrEmpty() && depth < 4) {
                                    currentElement = currentElement.parent() ?: continue@cardLoop
                                    depth++
                                }
                                if (!currentElement.className().isNullOrEmpty()) {
                                    wrapperMap.compute(currentElement.className()) { _, m ->
                                        m?.copy(amountHits = m.amountHits + 1) ?: Metadata(
                                            1,
                                            currentElement.allElements.size
                                        )
                                    }
                                    continue@cardLoop // card extracted, go to next
                                }
                            }
                        }
                    }
                    currentElement = currentElement.parent() ?: continue@cardLoop
                }
            }

            result = wrapperMap
                .asSequence()
                .filter { it.value.amountHits > 10 }
                .sortedWith(compareByDescending<Map.Entry<String, Metadata>> { it.value.amountHits }
                    .thenBy { it.value.elementSize })
                .firstOrNull()
                ?.key
            if (!result.isNullOrEmpty()){
                println("Wrapper class found: $result")
                break // wrapper found, no need to loop additional price nodes
            }
        }
        return result
    }


    private fun tokenize(input: String): List<String> {
        val cleaned = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")      // remove diacritics
            .replace('\u00A0', ' ')
            .replace('\u2009', ' ')
            .replace('\u202F', ' ')
            .lowercase()
            .replace("[^a-z0-9.,]+".toRegex(), " ")

        return cleaned
            .trim()
            .split("\\s+".toRegex())
    }

    private fun isSimilar(textTokens: List<String>, slugTokens: List<String>): Boolean {
        val textSet = textTokens.toSet()
        val slugSet = slugTokens.toSet()

        if (textSet.isEmpty() || slugSet.isEmpty()) return false

        val matchCount = slugSet.count { it in textSet }

        val score = matchCount.toDouble() / slugSet.size

        return score >= 0.5
    }

    private fun getSlug(url: String): String {
        val uri = URI.create(url)
        var path = if (!uri.path.isNullOrEmpty()) uri.path else return ""

        // Trim trailing /
        path = path.removeSuffix("/")

        // Take last segment after /
        val slug = path.substringAfterLast('/')

        // Strip .html or .htm and everything after
        slug.replace("\\.html?.*$".toRegex(), "")
        return slug
    }

    data class Metadata(
        val amountHits: Int, val elementSize: Int
    )

}