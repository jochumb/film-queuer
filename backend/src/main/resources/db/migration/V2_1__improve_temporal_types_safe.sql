-- Safe migration approach for temporal types
-- This version handles edge cases and provides fallbacks

-- Step 1: Add new columns
ALTER TABLE queues ADD COLUMN created_at_new TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films ADD COLUMN added_at_new TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films ADD COLUMN sort_order INT DEFAULT 0;
ALTER TABLE films ADD COLUMN release_date_new DATE NULL;

-- Step 2: Update with safe conversion for timestamps
-- Handle different possible timestamp formats from Exposed/Kotlin

-- For queues.created_at - try multiple formats
UPDATE queues SET created_at_new = CASE 
    WHEN created_at REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}' 
        THEN STR_TO_DATE(SUBSTRING(created_at, 1, 19), '%Y-%m-%dT%H:%i:%s')
    WHEN created_at REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}' 
        THEN STR_TO_DATE(created_at, '%Y-%m-%d %H:%i:%s')
    ELSE CURRENT_TIMESTAMP
END WHERE created_at IS NOT NULL;

-- For queue_films.added_at - try multiple formats
UPDATE queue_films SET added_at_new = CASE 
    WHEN added_at REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}' 
        THEN STR_TO_DATE(SUBSTRING(added_at, 1, 19), '%Y-%m-%dT%H:%i:%s')
    WHEN added_at REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}' 
        THEN STR_TO_DATE(added_at, '%Y-%m-%d %H:%i:%s')
    ELSE CURRENT_TIMESTAMP
END WHERE added_at IS NOT NULL;

-- Step 3: Set sort_order based on original added_at order
UPDATE queue_films qf1 
SET sort_order = (
    SELECT COUNT(*) 
    FROM queue_films qf2 
    WHERE qf2.queue_id = qf1.queue_id 
    AND qf2.added_at <= qf1.added_at
) - 1;

-- Step 4: Convert release dates safely
UPDATE films SET release_date_new = CASE
    WHEN release_date IS NULL THEN NULL
    WHEN release_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' THEN STR_TO_DATE(release_date, '%Y-%m-%d')
    WHEN release_date REGEXP '^[0-9]{4}$' THEN STR_TO_DATE(CONCAT(release_date, '-01-01'), '%Y-%m-%d')
    ELSE NULL
END WHERE release_date IS NOT NULL;

-- Step 5: Drop old columns
ALTER TABLE queues DROP COLUMN created_at;
ALTER TABLE queue_films DROP COLUMN added_at;  
ALTER TABLE films DROP COLUMN release_date;

-- Step 6: Rename new columns
ALTER TABLE queues CHANGE COLUMN created_at_new created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films CHANGE COLUMN added_at_new added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE films CHANGE COLUMN release_date_new release_date DATE NULL;

-- Step 7: Add indexes for performance
CREATE INDEX idx_queue_films_sort_order ON queue_films(queue_id, sort_order);
CREATE INDEX idx_queues_created_at ON queues(created_at);
CREATE INDEX idx_queue_films_added_at ON queue_films(added_at);
CREATE INDEX idx_films_release_date ON films(release_date);