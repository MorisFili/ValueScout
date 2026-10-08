package derivative.valueScout.productScraper

import derivative.valueScout.utility.Debug
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class PriceNodeFinder {
    fun findPriceNode(source: Document): List<String>? {
        val priceNodeMap = HashMap<String, Metadata>()

        for (priceElement in source.select("[class]")) {

            if (priceElement.allElements.size > 5) continue

            val cleanText = priceElement.text().replace('\u00A0', ' ')
                .replace("\u00C2", "").trim()

            if (isFullPrice(cleanText) || isNakedPrice(cleanText)) {
                val nodeScore = assessNode(priceElement)
                val nodeName = priceElement.className()

                priceNodeMap.compute(nodeName) { _, m ->
                    m?.copy(nodeScore = m.nodeScore + nodeScore, uniquePrices = m.uniquePrices.apply { add(cleanText) })
                        ?: Metadata(
                            nodeScore = nodeScore,
                            uniquePrices = mutableSetOf(cleanText),
                            elementSize = priceElement.allElements.size,
                            element = priceElement
                        )
                }

            }
        }

        return refineResults(priceNodeMap).keys.toList()
    }


    private fun refineResults(priceNodeMap: HashMap<String, Metadata>): LinkedHashMap<String, Metadata> {
        val refinedResults = HashMap<String, Int>()

        for (entry in priceNodeMap.entries) {
            val className = entry.key
            val priceData = entry.value
            val uniqueVariations =
                uniqueEntries(priceData.uniquePrices) // how many different prices the particular className has

            val totalScore = priceData.nodeScore

            val isStaticBanner = (totalScore > 10 && uniqueVariations <= 2)

            val truncatedClassName = className.trim().substringBefore(" ") // keep only first word
            if (!isStaticBanner) refinedResults[truncatedClassName] =
                refinedResults.getOrDefault(truncatedClassName, 0) + totalScore
        }

        val cutOffValue = 12 // 4x Strong | 6x FullPrice | 12x NakedPrice

        val metadata = priceNodeMap.entries.flatMap { entry ->
            entry.key.split(" ").map { it to entry.value }
        }.toMap()

        Debug.log("Debugging refined results")
        refinedResults.forEach { (string, i) -> println("$string: $i - ${metadata[string]!!.elementSize}") }
        Debug.log("End of debugging")

        val foundPriceNodes = refinedResults
            .asSequence()
            .filter { it.value >= cutOffValue }
            .sortedBy { metadata[it.key]?.elementSize ?: Int.MAX_VALUE }
            .filter { a ->
                val aMeta = metadata[a.key]!!
                refinedResults.keys.none { b ->
                    val bMeta = metadata[b]!!
                    a.key != b && aMeta.element.contains(bMeta.element)
                } }
            .take(3)
            .associate { it.key to metadata[it.key]!! }
            .toMap(LinkedHashMap())

        if (foundPriceNodes.isNotEmpty()) println("Found priceNodes: ${foundPriceNodes.keys}")

        return foundPriceNodes
    }

    private fun uniqueEntries(collection: Collection<*>): Int {
        return collection.toSet().size
    }

    private fun assessNode(node: Element): Int {
        val text = node.text().replace('\u00A0', ' ')
            .replace("\u00C2", "").trim()

        if (node.attributes().any { it.value.contains("price", ignoreCase = true) }) return 3
        if (isFullPrice(text)) return 2
        if (isNakedPrice(text)) return 1
        return 0
    }


    private fun isFullPrice(text: String): Boolean {
        // VALIDATION STRATEGY: "Loose" Detection
        // Checks if the text contains a valid Price + Currency combination.
        // 1. Matches "Number followed by Currency" (e.g., "149 kr", "149:-")
        // 2. OR Matches "Currency followed by Number" (e.g., "$100", "EUR 50")
        // 3. Handles flexible number formatting: accepts dots (.), commas (,), and spaces as separators.
        // 4. (?i) makes it case-insensitive (matches "KR", "kr", "Kr").
        return text.matches("(?i).*(\\d[\\d\\s.,]*)\\s?(kr|sek|eur|€|usd|\\$|£|gbp|:-).*".toRegex())
                || text.matches("(?i).*(kr|sek|eur|€|usd|\\$|£|gbp|:-)\\s?(\\d[\\d\\s.,]*).*".toRegex())
    }

    private fun isNakedPrice(text: String): Boolean {
        // VALIDATION STRATEGY: "Strict" Naked Price Detection
        // Used when no currency symbol is present (e.g. "249,00").
        // 1. Enforces specific formatting to avoid false positives (like years or phone numbers).
        // 2. Requires an integer followed by a separator (. or ,) and EXACTLY two decimal digits.
        // 3. \b ensures boundaries so it doesn't match part of a longer number.
        // Matches: "249,00", "19.99" | Ignores: "2024", "100", "3.14159"
        return text.matches(".*\\b\\d+[.,]\\d{2}\\b.*".toRegex())
    }

    data class Metadata(
        val nodeScore: Int, val elementSize: Int, val uniquePrices: MutableSet<String>, val element: Element
    )
}