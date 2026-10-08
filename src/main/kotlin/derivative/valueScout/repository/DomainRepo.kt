package derivative.valueScout.repository

import derivative.valueScout.dataModels.Domain
import derivative.valueScout.dataModels.DomainCategories
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class DomainRepo(private val jdbc: JdbcClient) {

    fun fetchAllByCategory(category: DomainCategories): List<Domain> {
        val domainData = jdbc
            .sql("SELECT domain, cardElement, category, queryCode FROM domains WHERE category = :category")
            .param("category", category.name)
            .query { result, _ ->
                Domain(
                    domain = result.getString("domain"),
                    cardElement = result.getString("cardElement"),
                ).also {
                    result.getString("category")?.let(it::setCategory)
                    result.getString("queryCode")?.let(it::setQueryCode)
                }
            }
            .list()

        for (domain in domainData) {
            domain.priceElements = fetchPriceElementsByDomain(domain.domain)
        }

        return domainData
    }

    fun fetch(domain: String): Domain? {
        val domainData = jdbc
            .sql("SELECT domain, cardElement, category, queryCode FROM domains WHERE domain = :domain")
            .param("domain", domain)
            .query { rs, _ ->
                Domain(
                    domain = rs.getString("domain"),
                    cardElement = rs.getString("cardElement"),
                ).also {
                    rs.getString("category")?.let(it::setCategory)
                    rs.getString("queryCode")?.let(it::setQueryCode)
                }
            }
            .optional()
            .orElse(null) ?: return null

        domainData.priceElements = fetchPriceElementsByDomain(domain)

        return domainData
    }

    fun save(domain: Domain) {
        val domainString = domain.domain
        val cardElement = domain.cardElement
        val priceElements = domain.priceElements
        val category = domain.category
        val queryCode = domain.queryCode

        jdbc.sql(
            """
        INSERT INTO domains(domain, cardElement, category, queryCode)
        VALUES (:domain, :cardElement, :category, :queryCode)
        ON CONFLICT(domain) DO UPDATE SET
            cardElement = COALESCE(excluded.cardElement, domains.cardElement),
            category = COALESCE(excluded.category, domains.category),
            queryCode = COALESCE(excluded.queryCode, domains.queryCode)
        """
        )
            .param("domain", domainString)
            .param("cardElement", cardElement)
            .param("category", category)
            .param("queryCode", queryCode)
            .update()

        jdbc.sql("DELETE FROM domain_priceElements WHERE domain = :domain")
            .param("domain", domainString)
            .update()

        priceElements?.forEach { element ->
            jdbc.sql(
                """
            INSERT INTO domain_priceElements(domain, priceElement)
            VALUES (:domain, :priceElement)
            """
            )
                .param("domain", domainString)
                .param("priceElement", element)
                .update()
        }
    }

    private fun fetchPriceElementsByDomain(domain: String): List<String> {
        return jdbc
            .sql("SELECT priceElement FROM domain_priceElements WHERE domain = :domain")
            .param("domain", domain)
            .query { rs, _ -> rs.getString("priceElement") }
            .list()
            .filterNotNull()
    }
}