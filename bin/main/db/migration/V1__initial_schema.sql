-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Regions table
CREATE TABLE regions (
    id                  BIGSERIAL PRIMARY KEY,
    geohash             VARCHAR(12) NOT NULL UNIQUE,
    centroid_lat        DOUBLE PRECISION NOT NULL,
    centroid_lng        DOUBLE PRECISION NOT NULL,
    centroid_point      GEOMETRY(Point, 4326),
    locality_score      NUMERIC(3,1),
    amenities_score     NUMERIC(3,1),
    connectivity_score  NUMERIC(3,1),
    traffic_score       NUMERIC(3,1),
    user_rating_score   NUMERIC(3,1),
    review_count        INTEGER DEFAULT 0,
    average_rating      NUMERIC(3,2),
    summary_text        TEXT,
    last_summary_updated TIMESTAMP WITH TIME ZONE,
    last_updated        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_regions_geohash ON regions(geohash);
CREATE INDEX idx_regions_centroid_point ON regions USING GIST(centroid_point);
CREATE INDEX idx_regions_last_updated ON regions(last_updated);

-- Facilities table
CREATE TABLE facilities (
    id              BIGSERIAL PRIMARY KEY,
    region_id       BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    place_id        VARCHAR(255),
    name            VARCHAR(500) NOT NULL,
    facility_type   VARCHAR(100) NOT NULL,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    location_point  GEOMETRY(Point, 4326),
    rating          NUMERIC(2,1),
    distance_meters DOUBLE PRECISION,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_facilities_region_id ON facilities(region_id);
CREATE INDEX idx_facilities_type ON facilities(facility_type);
CREATE INDEX idx_facilities_location ON facilities USING GIST(location_point);
CREATE UNIQUE INDEX idx_facilities_region_place ON facilities(region_id, place_id) WHERE place_id IS NOT NULL;

-- Connectivity data table
CREATE TABLE connectivity_data (
    id                          BIGSERIAL PRIMARY KEY,
    region_id                   BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE UNIQUE,
    nearest_airport_name        VARCHAR(500),
    nearest_airport_distance_m  DOUBLE PRECISION,
    metro_station_1_name        VARCHAR(500),
    metro_station_1_distance_m  DOUBLE PRECISION,
    metro_station_2_name        VARCHAR(500),
    metro_station_2_distance_m  DOUBLE PRECISION,
    connectivity_score          NUMERIC(3,1),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_connectivity_region_id ON connectivity_data(region_id);

-- Traffic data table
CREATE TABLE traffic_data (
    id                   BIGSERIAL PRIMARY KEY,
    region_id            BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE UNIQUE,
    baseline_time_secs   INTEGER,
    peak_time_secs       INTEGER,
    multiplier           NUMERIC(4,2),
    congestion_level     VARCHAR(20),
    traffic_score        NUMERIC(3,1),
    last_traffic_updated TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_traffic_region_id ON traffic_data(region_id);
CREATE INDEX idx_traffic_last_updated ON traffic_data(last_traffic_updated);

-- Reviews table
CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    region_id   BIGINT NOT NULL REFERENCES regions(id) ON DELETE CASCADE,
    user_id     VARCHAR(255) NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reviews_user_region UNIQUE (user_id, region_id)
);

CREATE INDEX idx_reviews_region_id ON reviews(region_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
