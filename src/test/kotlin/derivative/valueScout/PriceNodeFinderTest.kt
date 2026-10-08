package derivative.valueScout

import derivative.valueScout.productScraper.PriceNodeFinder
import derivative.valueScout.utility.Debug
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.fail

@SpringBootTest
class PriceNodeFinderTest(
    @Autowired private final val priceNodeFinder: PriceNodeFinder,
    @Autowired private val httpClient: RestClient
) {

    @Test
    fun priceNodeFinder() {

        val query = "https://www.tillskottsbolaget.se/cgi-bin/ibutik/AIR_ibutik.fcgi?funk=gor_sokning&AvanceradSokning=N&artnr=&varum=&artgrp=&Sprak_Suffix=SV&term=whey"

        val htmlBody = httpClient.get().uri(query).retrieve().body<String>() ?: fail("Unable to fetch http")

        val document = Jsoup.parse(htmlBody, "https://www.tillskottsbolaget.se")

        Files.writeString(Path.of("debug.html"), document.outerHtml())

        val results = priceNodeFinder.findPriceNode(document) ?: listOf("Nothing")

        Debug.log("Price nodes: ${results.size}")

        results.forEach { Debug.log(it) }
    }

    @Test
    fun priceNodeFinderLazy() {
        val file = File("debug.html")
        val document = Jsoup.parse(file, "UTF-8", "https://www.bodylab.se")

        val results = priceNodeFinder.findPriceNode(document) ?: listOf("Nothing")

        Debug.log("Price nodes: ${results.size}")

        results.forEach { Debug.log(it) }

    }
}