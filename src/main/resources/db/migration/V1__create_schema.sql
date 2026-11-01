CREATE TYPE record_status AS ENUM ('DRAFT', 'IMPORTED', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE poi_kind      AS ENUM ('SPOT', 'HERITAGE', 'MARKET', 'FOOD', 'CRAFT');
CREATE TYPE travel_mode   AS ENUM ('BUS', 'TRAIN', 'LAUNCH', 'SHIP', 'FLIGHT', 'JEEP', 'BOAT');
CREATE TYPE hotel_tier    AS ENUM ('BUDGET', 'MID', 'PREMIUM');

CREATE TABLE division (
  id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug    TEXT NOT NULL UNIQUE,
  name_en TEXT NOT NULL,
  name_bn TEXT NOT NULL
);

CREATE TABLE district (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug        TEXT NOT NULL UNIQUE,
  division_id BIGINT NOT NULL REFERENCES division (id),
  name_en     TEXT NOT NULL,
  name_bn     TEXT NOT NULL,
  lat         NUMERIC(8, 5) CHECK (lat BETWEEN 20 AND 27),
  lng         NUMERIC(8, 5) CHECK (lng BETWEEN 88 AND 93)
);
CREATE INDEX district_division_idx ON district (division_id);

CREATE TABLE hub (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug        TEXT NOT NULL UNIQUE,
  district_id BIGINT NOT NULL REFERENCES district (id),
  kind        TEXT NOT NULL DEFAULT 'CITY' CHECK (kind IN ('CITY', 'TERMINAL', 'STATION', 'PORT', 'AIRPORT')),
  name_en     TEXT NOT NULL,
  name_bn     TEXT,
  lat         NUMERIC(8, 5) CHECK (lat BETWEEN 20 AND 27),
  lng         NUMERIC(8, 5) CHECK (lng BETWEEN 88 AND 93)
);
CREATE INDEX hub_district_idx ON hub (district_id);

CREATE TABLE destination (
  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug                  TEXT NOT NULL UNIQUE,
  district_id           BIGINT NOT NULL REFERENCES district (id),
  arrival_hub_id        BIGINT REFERENCES hub (id),
  name_en               TEXT NOT NULL,
  name_bn               TEXT,
  lat                   NUMERIC(8, 5) CHECK (lat BETWEEN 20 AND 27),
  lng                   NUMERIC(8, 5) CHECK (lng BETWEEN 88 AND 93),
  road_factor           NUMERIC(3, 2) NOT NULL DEFAULT 1.40 CHECK (road_factor >= 1), -- hill roads run slower than straight distance
  best_months           SMALLINT[] NOT NULL DEFAULT '{1,2,3,4,5,6,7,8,9,10,11,12}',
  min_days              SMALLINT NOT NULL DEFAULT 1 CHECK (min_days > 0),
  local_transport_daily INTEGER CHECK (local_transport_daily >= 0),
  food_daily            INTEGER CHECK (food_daily >= 0),
  travel_note           TEXT,
  hotel_note            TEXT,
  guide_url             TEXT,
  status                record_status NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT destination_complete_when_published CHECK (
    status <> 'PUBLISHED'
    OR (name_bn IS NOT NULL AND lat IS NOT NULL AND lng IS NOT NULL AND arrival_hub_id IS NOT NULL)
  )
);
CREATE INDEX destination_district_idx ON destination (district_id);

CREATE TABLE poi (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  destination_id BIGINT NOT NULL REFERENCES destination (id),
  kind           poi_kind NOT NULL,
  name_en        TEXT NOT NULL,
  name_bn        TEXT,
  lat            NUMERIC(8, 5) CHECK (lat BETWEEN 20 AND 27),
  lng            NUMERIC(8, 5) CHECK (lng BETWEEN 88 AND 93),
  priority       SMALLINT NOT NULL DEFAULT 100,
  visit_minutes  SMALLINT CHECK (visit_minutes > 0),
  fee_min        INTEGER CHECK (fee_min >= 0),
  fee_max        INTEGER,
  closed_days    SMALLINT[] NOT NULL DEFAULT '{}',
  tags           TEXT[] NOT NULL DEFAULT '{}',
  note           TEXT,
  source_url     TEXT,
  checked_at     DATE,
  status         record_status NOT NULL DEFAULT 'DRAFT',
  UNIQUE (destination_id, kind, name_en),
  CONSTRAINT poi_fee_range CHECK (fee_max IS NULL OR fee_max >= fee_min),
  CONSTRAINT poi_checked_when_published CHECK (
    status <> 'PUBLISHED' OR (name_bn IS NOT NULL AND checked_at IS NOT NULL)
  ),
  CONSTRAINT poi_routable_when_published CHECK (
    status <> 'PUBLISHED' OR kind NOT IN ('SPOT', 'HERITAGE')
    OR (lat IS NOT NULL AND lng IS NOT NULL AND visit_minutes IS NOT NULL)
  )
);

CREATE TABLE hotel (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  destination_id BIGINT NOT NULL REFERENCES destination (id),
  name_en        TEXT NOT NULL,
  name_bn        TEXT,
  tier           hotel_tier,
  address        TEXT,
  lat            NUMERIC(8, 5) CHECK (lat BETWEEN 20 AND 27),
  lng            NUMERIC(8, 5) CHECK (lng BETWEEN 88 AND 93),
  room_capacity  SMALLINT NOT NULL DEFAULT 2 CHECK (room_capacity > 0),
  price_min      INTEGER CHECK (price_min > 0), -- NULL means unknown, never zero
  price_max      INTEGER,
  website        TEXT,
  note           TEXT,
  source_url     TEXT,
  checked_at     DATE,
  status         record_status NOT NULL DEFAULT 'DRAFT',
  UNIQUE (destination_id, name_en),
  CONSTRAINT hotel_price_range CHECK (
    (price_min IS NULL) = (price_max IS NULL) AND (price_max IS NULL OR price_max >= price_min)
  ),
  CONSTRAINT hotel_complete_when_published CHECK (
    status <> 'PUBLISHED'
    OR (tier IS NOT NULL AND lat IS NOT NULL AND lng IS NOT NULL AND price_min IS NOT NULL
        AND source_url IS NOT NULL AND checked_at IS NOT NULL)
  )
);

CREATE TABLE transport_leg (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  from_hub_id    BIGINT NOT NULL REFERENCES hub (id),
  to_hub_id      BIGINT NOT NULL REFERENCES hub (id),
  mode           travel_mode NOT NULL,
  service_name   TEXT NOT NULL,
  duration_min   INTEGER CHECK (duration_min > 0),
  departure_note TEXT,
  fare_min       INTEGER CHECK (fare_min > 0), -- NULL means unknown, never zero
  fare_max       INTEGER,
  months         SMALLINT[] NOT NULL DEFAULT '{1,2,3,4,5,6,7,8,9,10,11,12}', -- Saint Martin ships are seasonal
  note           TEXT,
  source_url     TEXT,
  checked_at     DATE,
  status         record_status NOT NULL DEFAULT 'DRAFT',
  UNIQUE (from_hub_id, to_hub_id, mode, service_name),
  CONSTRAINT leg_connects_two_hubs CHECK (from_hub_id <> to_hub_id),
  CONSTRAINT leg_fare_range CHECK (
    (fare_min IS NULL) = (fare_max IS NULL) AND (fare_max IS NULL OR fare_max >= fare_min)
  ),
  CONSTRAINT leg_complete_when_published CHECK (
    status <> 'PUBLISHED'
    OR (duration_min IS NOT NULL AND checked_at IS NOT NULL AND (fare_min IS NULL OR source_url IS NOT NULL))
  )
);
CREATE INDEX transport_leg_from_idx ON transport_leg (from_hub_id) WHERE status = 'PUBLISHED';

CREATE TABLE photo (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  owner_type TEXT NOT NULL CHECK (owner_type IN ('DESTINATION', 'POI', 'HOTEL')),
  owner_id   BIGINT NOT NULL,
  url        TEXT NOT NULL,
  credit     TEXT NOT NULL,
  license    TEXT NOT NULL
);
CREATE INDEX photo_owner_idx ON photo (owner_type, owner_id);

CREATE TABLE catalog_version (
  only_row BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (only_row),
  version  INTEGER NOT NULL
);
INSERT INTO catalog_version (version) VALUES (1);

CREATE TABLE trip_plan (
  id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  public_id       TEXT NOT NULL UNIQUE, -- random, so plans can't be listed by counting
  request_hash    TEXT NOT NULL,
  catalog_version INTEGER NOT NULL,
  request         JSONB NOT NULL,
  result          JSONB NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (request_hash, catalog_version) -- identical requests share one plan
);

CREATE TABLE plan_text (
  plan_id    BIGINT NOT NULL REFERENCES trip_plan (id) ON DELETE CASCADE,
  locale     TEXT NOT NULL CHECK (locale IN ('bn', 'en')),
  status     TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'READY', 'SKIPPED')),
  body       JSONB,
  model      TEXT,
  cost_usd   NUMERIC(10, 6),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (plan_id, locale)
);

CREATE TABLE job (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  type       TEXT NOT NULL,
  payload    JSONB NOT NULL,
  status     TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
  attempts   SMALLINT NOT NULL DEFAULT 0,
  run_after  TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX job_ready_idx ON job (run_after) WHERE status = 'PENDING';

CREATE TABLE audit_log (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor      TEXT NOT NULL,
  entity     TEXT NOT NULL,
  entity_id  BIGINT NOT NULL,
  action     TEXT NOT NULL,
  before     JSONB,
  after      JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX audit_log_entity_idx ON audit_log (entity, entity_id);
