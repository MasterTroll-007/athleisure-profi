-- Fitness Reservation System Database Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enable unaccent extension for diacritic-insensitive search
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'client',
    credits INTEGER DEFAULT 0,
    locale VARCHAR(5) DEFAULT 'cs',
    theme VARCHAR(10) DEFAULT 'system',
    email_verified BOOLEAN DEFAULT false,
    trainer_id UUID,
    calendar_start_hour INTEGER DEFAULT 6,
    calendar_end_hour INTEGER DEFAULT 22,
    invite_code VARCHAR(20) UNIQUE,
    email_reminders_enabled BOOLEAN DEFAULT true,
    reminder_hours_before INTEGER DEFAULT 24,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Pricing items (training types)
CREATE TABLE pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Availability blocks
CREATE TABLE availability_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100),
    days_of_week VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT DEFAULT 60,
    break_after_slots INT,
    break_duration_minutes INT,
    is_active BOOLEAN DEFAULT true,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Reservations (note: slots table is created by Hibernate)
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    block_id UUID REFERENCES availability_blocks(id) ON DELETE SET NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'confirmed',
    credits_used INTEGER DEFAULT 1,
    pricing_item_id UUID REFERENCES pricing_items(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    cancelled_at TIMESTAMP
);

-- Credit packages
CREATE TABLE credit_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    credits INTEGER NOT NULL,
    bonus_credits INTEGER DEFAULT 0,
    price_czk DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Credit transactions
CREATE TABLE credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    amount INTEGER NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference_id UUID,
    gopay_payment_id VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Training plans
CREATE TABLE training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL,
    file_path VARCHAR(500),
    preview_image VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Purchased plans
CREATE TABLE purchased_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID REFERENCES training_plans(id) ON DELETE CASCADE,
    credits_used INTEGER NOT NULL,
    purchased_at TIMESTAMP DEFAULT NOW()
);

-- Client notes
CREATE TABLE client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- GoPay payments
CREATE TABLE gopay_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    gopay_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    state VARCHAR(30) NOT NULL,
    credit_package_id UUID REFERENCES credit_packages(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Email verification tokens
CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_date ON reservations(date);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_purchased_plans_user_id ON purchased_plans(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_reservations_date_time ON reservations(date, start_time);

-- Insert default admin user (password: Nujfo6oJbo)
INSERT INTO users (email, password_hash, first_name, last_name, role, credits, email_verified)
VALUES ('admin@fitness.cz', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Admin', 'Fitness', 'admin', 999, true);

-- Insert test admin user (password: Nujfo6oJbo)
INSERT INTO users (email, password_hash, first_name, last_name, role, credits, email_verified)
VALUES ('admin@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Test', 'Admin', 'admin', 10, true);

-- Insert 5 test users assigned to admin@test.com (password: Nujfo6oJbo, 10 credits each)
INSERT INTO users (email, password_hash, first_name, last_name, role, credits, email_verified, trainer_id) VALUES
('test1@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Jana', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
('test2@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Eva', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
('test3@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Lucka', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
('test4@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Petra', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
('test5@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Misa', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com'));

-- Insert default pricing items
INSERT INTO pricing_items (name_cs, name_en, credits, sort_order) VALUES
('Jeden trénink', 'Single training', 1, 1),
('Trénink student', 'Student training', 1, 2),
('10 tréninků', '10 trainings', 10, 3),
('Individuální tréninkový plán', 'Individual training plan', 5, 4),
('Kompletní fitness plán', 'Complete fitness plan', 8, 5);

-- Insert default credit packages
INSERT INTO credit_packages (name_cs, name_en, credits, bonus_credits, price_czk, sort_order) VALUES
('1 kredit', '1 credit', 1, 0, 500.00, 1),
('5 kreditů', '5 credits', 5, 0, 2250.00, 2),
('10 kreditů', '10 credits', 10, 1, 4000.00, 3),
('20 kreditů', '20 credits', 20, 3, 7000.00, 4);

-- Insert default availability block for admin@test.com
INSERT INTO availability_blocks (name, days_of_week, start_time, end_time, slot_duration_minutes, admin_id)
VALUES ('Odpolední blok', '1,2,3,4,5', '14:00', '18:00', 60, (SELECT id FROM users WHERE email = 'admin@test.com'));

-- Slots table (created here to avoid dependency on Hibernate)
CREATE TABLE IF NOT EXISTS slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INT DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    assigned_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    note TEXT,
    template_id UUID,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (date, start_time)
);

CREATE INDEX IF NOT EXISTS idx_slot_date_status ON slots(date, status);
CREATE INDEX IF NOT EXISTS idx_slot_assigned_user ON slots(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_slot_template ON slots(template_id);
CREATE INDEX IF NOT EXISTS idx_slot_admin ON slots(admin_id);
CREATE INDEX IF NOT EXISTS idx_availability_block_admin ON availability_blocks(admin_id);
CREATE INDEX IF NOT EXISTS idx_user_trainer ON users(trainer_id);

-- Migrations for existing databases (add new columns if they don't exist)
DO $$
BEGIN
    -- Add trainer_id to users
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'trainer_id') THEN
        ALTER TABLE users ADD COLUMN trainer_id UUID;
    END IF;
    -- Add invite_code to users (for trainers/admins)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'invite_code') THEN
        ALTER TABLE users ADD COLUMN invite_code VARCHAR(20) UNIQUE;
    END IF;
    -- Add calendar_start_hour to users
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'calendar_start_hour') THEN
        ALTER TABLE users ADD COLUMN calendar_start_hour INTEGER DEFAULT 6;
    END IF;
    -- Add calendar_end_hour to users
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'calendar_end_hour') THEN
        ALTER TABLE users ADD COLUMN calendar_end_hour INTEGER DEFAULT 22;
    END IF;
    -- Add admin_id to availability_blocks
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'availability_blocks' AND column_name = 'admin_id') THEN
        ALTER TABLE availability_blocks ADD COLUMN admin_id UUID;
    END IF;
    -- Add admin_id to slots
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'slots' AND column_name = 'admin_id') THEN
        ALTER TABLE slots ADD COLUMN admin_id UUID;
    END IF;
END $$;

-- Add slot_id column to reservations if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reservations' AND column_name = 'slot_id') THEN
        ALTER TABLE reservations ADD COLUMN slot_id UUID REFERENCES slots(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Add note column to reservations if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'reservations' AND column_name = 'note') THEN
        ALTER TABLE reservations ADD COLUMN note TEXT;
    END IF;
END $$;

-- Generate slots for current week + next week (Monday-Friday, 14:00-18:00)
-- with 50% of slots reserved by test users
DO $$
DECLARE
    monday_of_week DATE;
    slot_date DATE;
    week_offset INT;
    day_offset INT;
    hour_val INT;
    slot_id UUID;
    test_user_id UUID;
    test_user_ids UUID[];
    slot_count INT := 0;
    reservation_count INT := 0;
BEGIN
    -- Get Monday of current week
    monday_of_week := date_trunc('week', CURRENT_DATE)::date;

    -- Get test user IDs
    SELECT array_agg(id ORDER BY email) INTO test_user_ids
    FROM users
    WHERE email LIKE 'test%@test.com' AND role = 'client'
    LIMIT 5;

    IF test_user_ids IS NULL OR array_length(test_user_ids, 1) = 0 THEN
        RAISE NOTICE 'No test users found, skipping slot generation';
        RETURN;
    END IF;

    -- Loop through 2 weeks (current + next)
    FOR week_offset IN 0..1 LOOP
        -- Loop through Monday to Friday
        FOR day_offset IN 0..4 LOOP
            slot_date := monday_of_week + (week_offset * 7 + day_offset);

            -- Only create slots for today or future
            IF slot_date >= CURRENT_DATE THEN
                -- Create 4 hourly slots: 14:00, 15:00, 16:00, 17:00
                FOR hour_val IN 14..17 LOOP
                    -- Generate new UUID for slot
                    slot_id := gen_random_uuid();

                    -- Insert slot (every other slot will be RESERVED, others UNLOCKED)
                    INSERT INTO slots (id, date, start_time, end_time, duration_minutes, status, admin_id, created_at)
                    VALUES (
                        slot_id,
                        slot_date,
                        make_time(hour_val, 0, 0),
                        make_time(hour_val + 1, 0, 0),
                        60,
                        CASE WHEN slot_count % 2 = 0 THEN 'RESERVED' ELSE 'UNLOCKED' END,
                        (SELECT id FROM users WHERE email = 'admin@test.com'),
                        NOW()
                    )
                    ON CONFLICT (date, start_time) DO NOTHING;

                    -- If slot was inserted and should be reserved (every other slot)
                    IF FOUND AND slot_count % 2 = 0 THEN
                        -- Pick test user in round-robin fashion
                        test_user_id := test_user_ids[1 + (reservation_count % array_length(test_user_ids, 1))];

                        -- Create reservation
                        INSERT INTO reservations (id, user_id, slot_id, date, start_time, end_time, status, credits_used, created_at)
                        VALUES (
                            gen_random_uuid(),
                            test_user_id,
                            slot_id,
                            slot_date,
                            make_time(hour_val, 0, 0),
                            make_time(hour_val + 1, 0, 0),
                            'confirmed',
                            1,
                            NOW()
                        );

                        -- Create credit transaction
                        INSERT INTO credit_transactions (id, user_id, amount, type, note, created_at)
                        VALUES (
                            gen_random_uuid(),
                            test_user_id,
                            -1,
                            'reservation',
                            'Rezervace na ' || slot_date::text,
                            NOW()
                        );

                        -- Deduct credit from user
                        UPDATE users SET credits = credits - 1 WHERE id = test_user_id;

                        reservation_count := reservation_count + 1;
                    END IF;

                    slot_count := slot_count + 1;
                END LOOP;
            END IF;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Created % slots with % reservations (~50%% occupancy)', slot_count, reservation_count;
END $$;

-- Generate invite codes for existing admins who don't have one
UPDATE users
SET invite_code = LOWER(SUBSTRING(MD5(RANDOM()::TEXT || id::TEXT), 1, 8))
WHERE role = 'admin' AND invite_code IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_invite_code ON users(invite_code);

-- Add reminder columns to users if they don't exist (migration for existing databases)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'email_reminders_enabled') THEN
        ALTER TABLE users ADD COLUMN email_reminders_enabled BOOLEAN DEFAULT true;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'reminder_hours_before') THEN
        ALTER TABLE users ADD COLUMN reminder_hours_before INTEGER DEFAULT 24;
    END IF;
END $$;

-- Table for tracking sent reminders to avoid duplicates
CREATE TABLE IF NOT EXISTS reminder_sent_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sent_at TIMESTAMP DEFAULT NOW(),
    reminder_type VARCHAR(20) NOT NULL DEFAULT 'email',
    UNIQUE(reservation_id, reminder_type)
);

CREATE INDEX IF NOT EXISTS idx_reminder_sent_reservation ON reminder_sent_log(reservation_id);
CREATE INDEX IF NOT EXISTS idx_reminder_sent_user ON reminder_sent_log(user_id);
