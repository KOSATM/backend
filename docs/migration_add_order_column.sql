-- Migration: Add 'order' column to plan_places table
-- Date: 2025-12-08
-- Purpose: Support place ordering within each day for CRUD operations

-- Step 1: Add the order column
ALTER TABLE plan_places ADD COLUMN IF NOT EXISTS "order" INTEGER;

-- Step 2: Initialize order values based on existing start_at times
-- This uses a window function to assign sequential numbers within each day
WITH ordered_places AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY day_id ORDER BY start_at, id) as new_order
    FROM plan_places
)
UPDATE plan_places
SET "order" = ordered_places.new_order
FROM ordered_places
WHERE plan_places.id = ordered_places.id;

-- Step 3: Verify the migration
SELECT
    pp.id,
    pp.day_id,
    pp."order",
    pp.place_name,
    pp.start_at
FROM plan_places pp
ORDER BY pp.day_id, pp."order";

-- Optional: Add index for better performance
CREATE INDEX IF NOT EXISTS idx_plan_places_day_order
ON plan_places(day_id, "order");

-- Optional: Add constraint to ensure order is not null for future inserts
-- (You can uncomment this after testing)
-- ALTER TABLE plan_places ALTER COLUMN "order" SET NOT NULL;
