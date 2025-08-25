-- Rollback migration for temporal types
-- Use this if you need to revert to the original string-based temporal columns
-- WARNING: This will convert timestamps back to strings and may lose precision

-- Add old-style columns back
ALTER TABLE queues ADD COLUMN created_at_old VARCHAR(50);
ALTER TABLE queue_films ADD COLUMN added_at_old VARCHAR(50);
ALTER TABLE films ADD COLUMN release_date_old VARCHAR(10);

-- Convert current timestamp data back to string format
UPDATE queues SET created_at_old = DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') 
WHERE created_at IS NOT NULL;

UPDATE queue_films SET added_at_old = DATE_FORMAT(added_at, '%Y-%m-%dT%H:%i:%s') 
WHERE added_at IS NOT NULL;

UPDATE films SET release_date_old = DATE_FORMAT(release_date, '%Y-%m-%d') 
WHERE release_date IS NOT NULL;

-- Drop current columns
ALTER TABLE queues DROP COLUMN created_at;
ALTER TABLE queue_films DROP COLUMN added_at;
ALTER TABLE queue_films DROP COLUMN sort_order;
ALTER TABLE films DROP COLUMN release_date;

-- Rename old columns back
ALTER TABLE queues CHANGE COLUMN created_at_old created_at VARCHAR(50);
ALTER TABLE queue_films CHANGE COLUMN added_at_old added_at VARCHAR(50);  
ALTER TABLE films CHANGE COLUMN release_date_old release_date VARCHAR(10);

-- Drop the indexes we created
DROP INDEX IF EXISTS idx_queue_films_sort_order;
DROP INDEX IF EXISTS idx_queues_created_at;  
DROP INDEX IF EXISTS idx_queue_films_added_at;
DROP INDEX IF EXISTS idx_films_release_date;