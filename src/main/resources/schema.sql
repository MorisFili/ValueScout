CREATE TABLE IF NOT EXISTS domains(
    domain TEXT PRIMARY KEY,
    cardElement TEXT,
    inputElement TEXT,
    category TEXT,
    queryCode TEXT
);
CREATE TABLE IF NOT EXISTS domain_priceElements(
    id INTEGER PRIMARY KEY,
    domain TEXT NOT NULL,
    priceElement TEXT,
    FOREIGN KEY (domain) REFERENCES domains(domain) ON DELETE CASCADE
)