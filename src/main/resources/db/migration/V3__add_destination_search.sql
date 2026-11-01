CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX destination_name_en_trgm_idx ON destination USING gin (name_en gin_trgm_ops);
CREATE INDEX destination_name_bn_trgm_idx ON destination USING gin (name_bn gin_trgm_ops);
CREATE INDEX poi_destination_idx ON poi (destination_id);
CREATE INDEX hotel_destination_idx ON hotel (destination_id);
CREATE INDEX transport_leg_to_idx ON transport_leg (to_hub_id);
