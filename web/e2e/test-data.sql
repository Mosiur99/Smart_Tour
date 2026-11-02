-- Fake data for end-to-end tests and local demos. It publishes Sylhet with
-- invented places, a hotel and prices so the planner has something to plan.
-- NEVER run this on a production database: nothing in it is verified.
UPDATE destination
SET status = 'PUBLISHED', lat = 24.89, lng = 91.87, min_days = 2, best_months = '{10,11,12,1,2}',
    food_daily = 600, local_transport_daily = 400
WHERE slug = 'sylhet';

INSERT INTO poi (destination_id, kind, name_en, name_bn, lat, lng, priority, visit_minutes, fee_min, fee_max,
                 checked_at, status)
SELECT d.id, p.kind::poi_kind, p.name_en, p.name_bn, p.lat, p.lng, p.priority, p.visit_minutes, p.fee_min,
       p.fee_max, current_date, 'PUBLISHED'
FROM destination d
CROSS JOIN (VALUES
    ('SPOT', 'Test Lake', 'পরীক্ষা হ্রদ', 24.90, 91.88, 1, 90, 50, 100),
    ('SPOT', 'Test Tea Garden', 'পরীক্ষা চা-বাগান', 24.92, 91.86, 2, 120, NULL, NULL),
    ('HERITAGE', 'Test Shrine', 'পরীক্ষা মাজার', 24.89, 91.87, 3, 60, NULL, NULL),
    ('FOOD', 'Test Restaurant', 'পরীক্ষা রেস্তোরাঁ', 24.89, 91.87, 1, NULL, NULL, NULL),
    ('MARKET', 'Test Market', 'পরীক্ষা বাজার', 24.89, 91.87, 1, NULL, NULL, NULL),
    ('CRAFT', 'Test Cane Craft', 'পরীক্ষা বেতশিল্প', 24.89, 91.87, 1, NULL, NULL, NULL)
) AS p(kind, name_en, name_bn, lat, lng, priority, visit_minutes, fee_min, fee_max)
WHERE d.slug = 'sylhet';

INSERT INTO hotel (destination_id, name_en, name_bn, tier, lat, lng, price_min, price_max, source_url, checked_at,
                   status)
SELECT id, 'Test Hotel', 'পরীক্ষা হোটেল', 'MID', 24.90, 91.88, 2000, 3000, 'https://example.com', current_date,
       'PUBLISHED'
FROM destination WHERE slug = 'sylhet';

UPDATE transport_leg
SET status = 'PUBLISHED', duration_min = 420, checked_at = current_date, source_url = 'https://example.com'
WHERE mode = 'BUS'
  AND (from_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet') OR to_hub_id = (SELECT id FROM hub WHERE slug = 'sylhet'));

UPDATE catalog_version SET version = version + 1;
