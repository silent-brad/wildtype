CREATE TABLE IF NOT EXISTS sightings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    detected_at TEXT NOT NULL,
    species TEXT NOT NULL,
    confidence REAL NOT NULL,
    image_path TEXT NOT NULL,
    downlink_format INTEGER
);

CREATE INDEX IF NOT EXISTS idx_sightings_time ON sightings(detected_at DESC);
