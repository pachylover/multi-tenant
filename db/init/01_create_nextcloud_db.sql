-- Create nextcloud database if it doesn't exist
-- Note: PostgreSQL's docker-entrypoint runs this script only once, so no need for conditional check
CREATE DATABASE nextcloud OWNER app;

