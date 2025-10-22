-- Security Enhancement Migration
-- Add security_events and ip_lockouts tables

-- Create security_events table
CREATE TABLE IF NOT EXISTS security_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    email VARCHAR(255),
    username VARCHAR(100),
    details TEXT,
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_security_events_ip_address (ip_address),
    INDEX idx_security_events_email (email),
    INDEX idx_security_events_created_at (created_at),
    INDEX idx_security_events_event_type (event_type)
);

-- Create ip_lockouts table
CREATE TABLE IF NOT EXISTS ip_lockouts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    reason VARCHAR(500),
    attempt_count INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unlocked_at TIMESTAMP,
    INDEX idx_ip_lockouts_ip_address (ip_address),
    INDEX idx_ip_lockouts_created_at (created_at),
    INDEX idx_ip_lockouts_is_active (is_active)
);

-- Insert sample security event types (for reference)
-- These are handled by the enum in the Java code

