ALTER TABLE destination ADD COLUMN university_note TEXT;

CREATE TABLE university (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  destination_id BIGINT NOT NULL REFERENCES destination (id),
  name_en        TEXT NOT NULL,
  kind           TEXT,
  location       TEXT,
  note           TEXT,
  UNIQUE (destination_id, name_en)
);
