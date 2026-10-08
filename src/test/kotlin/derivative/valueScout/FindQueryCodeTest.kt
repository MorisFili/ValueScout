package derivative.valueScout

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.Request
import com.microsoft.playwright.options.WaitUntilState
import derivative.valueScout.dataModels.Domain

import derivative.valueScout.repository.DomainRepo
import derivative.valueScout.utility.Debug
import org.jsoup.Jsoup

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import java.net.URI
import kotlin.use

@SpringBootTest
class FindQueryCodeTest(
    @Autowired private val domainRepo: DomainRepo,
) {

    @Test
    fun collectProductTest() {
        val result = findQueryCode(Domain("bodylab.se"))
        println(result)

    }

    private fun findQueryCode(domain: Domain): String? {

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
                        request.url().contains("whey", ignoreCase = true)
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

                input.fill("whey")
                input.press("Enter")
                page.waitForTimeout(1000.0)

                val match = requests.firstOrNull {
                    it.url().contains("whey", ignoreCase = true)
                } ?: continue

                // check here if query actually produces usable DOMs
                val initialDOMSize = Jsoup.parse(URI(match.url().replace
                    ("whey", "product")).toURL(), 2000).html().length
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

    private fun detectBestContainer(page: Page): String? {
        return page.evaluate(
            """
() => (async () => {
    const delay = ms => new Promise(r => setTimeout(r, ms));

    // unlock common root locks
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
        page.evaluate("""
(selector) => (async () => {
    const delay = ms => new Promise(r => setTimeout(r, ms));

    const el =
        selector === 'html'
            ? document.documentElement
            : selector === 'body'
                ? document.body
                : document.querySelector(selector);

    if (!el) return;

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

    // optional reset
    if (isRoot) {
        window.scrollTo(0, 0);
    } else {
        el.scrollTop = 0;
    }

    await delay(200);
})(arguments[0])
    """.trimIndent(), selector)
    }

    private fun linkFromDomain(domain: Domain): String {
        return "https://${domain.domain}"
    }

    private fun sameDomain(targetURL: String, page: Page): Boolean {
        val currentHost = URI(page.url()).host
        val targetHost = URI(targetURL).host

        return targetHost == currentHost || targetHost.endsWith(".$currentHost")
    }
}