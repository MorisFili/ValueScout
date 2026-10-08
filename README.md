# ValueScout

An experimental web crawler and product scraper designed to extract structured product data from websites without predefined selectors or site-specific scraping rules.

Rather than relying on hardcoded DOM paths, ValueScout analyzes the structure of unfamiliar e-commerce pages and attempts to infer where products, prices and product cards are located.

### Features

* Automatic discovery of website search/query mechanisms
* DOM traversal and heuristic product-card detection
* Price-node identification and scoring
* Product link and name inference
* Playwright + Chromium fallback for JavaScript-rendered websites
* Network request monitoring for dynamically loaded search results
* Per-domain discovery and configuration caching
* Jsoup-based HTML analysis and extraction

### Status

Experimental and no longer actively developed. The project explores generalized product extraction across websites with different DOM structures and rendering strategies.
