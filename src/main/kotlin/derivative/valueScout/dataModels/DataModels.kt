package derivative.valueScout.dataModels

data class Domain(
    val domain: String,
    var cardElement: String? = null,
    var priceElements: List<String>? = null
) {
    var category: DomainCategories? = null
        private set

    var queryCode: String? = null
        private set

    fun setQueryCode(queryCode: String) {
        this.queryCode = queryCode // better refinement later
    }

    fun setCategory(category: String) {
        this.category = runCatching {
            enumValueOf<DomainCategories>(category)
        }.getOrNull()
    }

    fun setCategory(category: DomainCategories) {
        this.category = category;
    }
}

data class Product(
    var name: String? = null,
    var weightValue: Double? = null,
    var weightUnit: String? = null,
    var price: Double? = null,
    var currency: String? = null,
    var linkToProduct: String? = null,
    var linkToImage: String? = null)

enum class DomainCategories {
    ELECTRONICS, SUPPLEMENTS, CLOTHES, GENERAL, UNCLASSIFIED
}

