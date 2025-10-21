package com.badat.study1.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if success column already exists
            String checkColumnQuery = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'mmo_market' AND TABLE_NAME = 'auditlog' AND COLUMN_NAME = 'success'";
            
            Integer columnExists = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (columnExists == null || columnExists == 0) {
                log.info("Running database migration for auditlog table...");
                
                // Add success column
                jdbcTemplate.execute("ALTER TABLE auditlog ADD COLUMN success BOOLEAN NOT NULL DEFAULT TRUE");
                log.info("Added success column to auditlog table");
                
                // Add device_info column
                try {
                    jdbcTemplate.execute("ALTER TABLE auditlog ADD COLUMN device_info VARCHAR(500)");
                    log.info("Added device_info column to auditlog table");
                } catch (Exception e) {
                    log.warn("device_info column might already exist: {}", e.getMessage());
                }
                
                // Update existing records
                jdbcTemplate.execute("UPDATE auditlog SET device_info = '' WHERE device_info IS NULL");
                log.info("Updated existing auditlog records with empty device_info");
                
                // Add index for device_info
                try {
                    jdbcTemplate.execute("CREATE INDEX idx_auditlog_device_info ON auditlog(device_info)");
                    log.info("Created index on device_info column");
                } catch (Exception e) {
                    log.warn("Index on device_info column might already exist: {}", e.getMessage());
                }
                
                // Update existing records
                jdbcTemplate.execute("UPDATE auditlog SET success = TRUE WHERE success IS NULL");
                log.info("Updated existing auditlog records");
                
                // Add indexes
                try {
                    jdbcTemplate.execute("CREATE INDEX idx_auditlog_success ON auditlog(success)");
                    log.info("Created index on success column");
                } catch (Exception e) {
                    log.warn("Index on success column might already exist: {}", e.getMessage());
                }
                
                try {
                    jdbcTemplate.execute("CREATE INDEX idx_auditlog_action ON auditlog(action)");
                    log.info("Created index on action column");
                } catch (Exception e) {
                    log.warn("Index on action column might already exist: {}", e.getMessage());
                }
                
                try {
                    jdbcTemplate.execute("CREATE INDEX idx_auditlog_ip_address ON auditlog(ip_address)");
                    log.info("Created index on ip_address column");
                } catch (Exception e) {
                    log.warn("Index on ip_address column might already exist: {}", e.getMessage());
                }
                
                log.info("Database migration completed successfully!");
            } else {
                log.info("Database migration already applied, skipping...");
            }
            
            // Check and run avatar migration
            runAvatarMigration();
        } catch (Exception e) {
            log.error("Database migration failed: {}", e.getMessage(), e);
        }
    }
    
    private void runAvatarMigration() {
        try {
            // Check if avatar columns already exist
            String checkAvatarDataQuery = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'mmo_market' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'avatar_data'";
            
            Integer avatarDataExists = jdbcTemplate.queryForObject(checkAvatarDataQuery, Integer.class);
            
            if (avatarDataExists == null || avatarDataExists == 0) {
                log.info("Running avatar migration for user table...");
                
                // Add avatar columns
                jdbcTemplate.execute("ALTER TABLE user ADD COLUMN avatar_data LONGBLOB NULL COMMENT 'Avatar image data'");
                jdbcTemplate.execute("ALTER TABLE user ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT 'Avatar URL from OAuth provider'");
                
                log.info("Avatar migration completed successfully!");
            } else {
                log.info("Avatar migration already applied, skipping...");
            }
        } catch (Exception e) {
            log.error("Avatar migration failed: {}", e.getMessage(), e);
        }
    }
}
