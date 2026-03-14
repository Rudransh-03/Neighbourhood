CREATE TABLE geocoding_cache (
    id                 BIGSERIAL PRIMARY KEY,
    query_key          VARCHAR(500) NOT NULL UNIQUE,
    lat                DOUBLE PRECISION NOT NULL,
    lng                DOUBLE PRECISION NOT NULL,
    formatted_address  VARCHAR(500),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_geocoding_cache_query ON geocoding_cache(query_key);
