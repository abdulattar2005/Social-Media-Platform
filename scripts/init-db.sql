-- Each microservice owns its own database (database-per-service pattern).
-- This script runs once when the postgres-db container's data volume is
-- first initialized (docker-entrypoint-initdb.d).

CREATE DATABASE userdb;
CREATE DATABASE postdb;
CREATE DATABASE notificationdb;
CREATE DATABASE chatdb;
CREATE DATABASE analyticsdb;
