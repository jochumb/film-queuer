# Database Migrations

This directory contains database migration scripts for the Film Queuer application.

## Migration Overview

### V2: Improve Temporal Types

**Purpose**: Convert database columns to use proper temporal types instead of strings.

**Changes**:
- `queues.created_at`: `VARCHAR` → `TIMESTAMP`
- `queue_films.added_at`: `VARCHAR` → `TIMESTAMP` 
- `queue_films.sort_order`: New `INT` column for manual sorting
- `films.release_date`: `VARCHAR(10)` → `DATE`

**Benefits**:
- Better type safety and validation
- Improved query performance with proper indexing
- Native database temporal operations
- Reduced storage space

## Migration Files

### V2__improve_temporal_types.sql
Basic migration that assumes standard ISO format timestamps and dates.

### V2_1__improve_temporal_types_safe.sql  
**Recommended**: Robust migration that handles multiple timestamp formats and edge cases.

Features:
- Multiple timestamp format detection
- Fallback to CURRENT_TIMESTAMP for invalid data
- Preserves sort order based on original `added_at` values
- Handles partial dates (year-only) for release dates
- Adds performance indexes

### V2_2__rollback_temporal_types.sql
Rollback migration to revert changes if needed.

**⚠️ Warning**: Converting back to strings may lose timestamp precision.

## Usage

1. **Choose the appropriate migration**:
   - Use `V2_1__improve_temporal_types_safe.sql` for production
   - Use `V2__improve_temporal_types.sql` for clean test environments

2. **Run migration**:
   ```bash
   # Using Flyway or similar tool
   flyway migrate
   
   # Or manually execute the SQL file
   mysql -u username -p database_name < V2_1__improve_temporal_types_safe.sql
   ```

3. **Verify results**:
   ```sql
   DESCRIBE queues;
   DESCRIBE queue_films; 
   DESCRIBE films;
   
   -- Check data integrity
   SELECT COUNT(*) FROM queues WHERE created_at IS NULL;
   SELECT COUNT(*) FROM queue_films WHERE added_at IS NULL;
   ```

## Rollback Procedure

If you need to rollback:

1. Run the rollback migration:
   ```sql
   source V2_2__rollback_temporal_types.sql;
   ```

2. Update your application code to use the old temporal types

3. Restart the application

## Testing

Before running in production:

1. **Backup your database**
2. **Test on a copy** of production data
3. **Verify data integrity** after migration
4. **Test application functionality** with new types

## Application Code Changes

The migration works with these application changes:
- Domain models use `Instant` for timestamps, `LocalDate` for dates
- Repository layer handles conversion between database and domain types
- API layer converts between JSON strings and domain temporal types