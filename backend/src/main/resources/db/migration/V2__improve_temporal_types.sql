-- Migration to improve temporal types in database
-- V2: Convert LocalDateTime to TIMESTAMP and String dates to DATE

-- Add new columns with correct types
ALTER TABLE queues ADD COLUMN created_at_new TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films ADD COLUMN added_at_new TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films ADD COLUMN sort_order INT DEFAULT 0;
ALTER TABLE films ADD COLUMN release_date_new DATE NULL;

-- Copy existing data, converting formats where possible
-- For timestamps, convert LocalDateTime strings to TIMESTAMP
UPDATE queues SET created_at_new = STR_TO_DATE(created_at, '%Y-%m-%dT%H:%i:%s') WHERE created_at IS NOT NULL;

-- For queue_films timestamps
UPDATE queue_films SET added_at_new = STR_TO_DATE(added_at, '%Y-%m-%dT%H:%i:%s') WHERE added_at IS NOT NULL;

-- For release dates, convert valid date strings to DATE
UPDATE films SET release_date_new = STR_TO_DATE(release_date, '%Y-%m-%d') 
WHERE release_date IS NOT NULL 
AND release_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$';

-- Drop old columns
ALTER TABLE queues DROP COLUMN created_at;
ALTER TABLE queue_films DROP COLUMN added_at;
ALTER TABLE films DROP COLUMN release_date;

-- Rename new columns to original names
ALTER TABLE queues CHANGE COLUMN created_at_new created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE queue_films CHANGE COLUMN added_at_new added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE films CHANGE COLUMN release_date_new release_date DATE NULL;

-- Add index on sort_order for better query performance
CREATE INDEX idx_queue_films_sort_order ON queue_films(queue_id, sort_order);