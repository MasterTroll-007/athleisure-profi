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
    is_blocked BOOLEAN DEFAULT false,
    adjacent_booking_required BOOLEAN DEFAULT true,
    avatar_path VARCHAR(500),
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
    duration_minutes INTEGER DEFAULT 60,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Training locations (where trainings take place: gym A, gym B, ...)
CREATE TABLE training_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    address_cs TEXT,
    address_en TEXT,
    color VARCHAR(7) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_training_location_admin ON training_locations(admin_id);
CREATE INDEX idx_training_location_active ON training_locations(is_active);

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
    location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Reservations (note: slots table is created by Hibernate)
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    block_id UUID REFERENCES availability_blocks(id) ON DELETE SET NULL,
    slot_id UUID,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'confirmed',
    credits_used INTEGER DEFAULT 1,
    pricing_item_id UUID REFERENCES pricing_items(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    cancelled_at TIMESTAMP,
    completed_at TIMESTAMP,
    recurring_reservation_id UUID,
    note TEXT
);

-- Credit packages
CREATE TABLE credit_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID,
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description TEXT,
    credits INTEGER NOT NULL,
    bonus_credits INTEGER DEFAULT 0,
    price_czk DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    highlight_type VARCHAR(30) DEFAULT 'NONE',
    is_basic BOOLEAN DEFAULT false,
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
    stripe_payment_id VARCHAR(255),
    note TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Training plans
CREATE TABLE training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) DEFAULT '',
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description TEXT,
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL,
    file_path VARCHAR(500),
    preview_image VARCHAR(500),
    price DECIMAL(10,2) DEFAULT 0 NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    validity_days INTEGER DEFAULT 30,
    sessions_count INTEGER,
    is_active BOOLEAN DEFAULT true,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Purchased plans
CREATE TABLE purchased_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    plan_id UUID REFERENCES training_plans(id) ON DELETE CASCADE,
    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '30 days'),
    sessions_remaining INTEGER,
    gopay_payment_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Client notes
CREATE TABLE client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID REFERENCES users(id) ON DELETE CASCADE,
    admin_id UUID REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
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

-- Stripe payments
CREATE TABLE stripe_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    stripe_session_id VARCHAR(255) UNIQUE NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    status VARCHAR(30) NOT NULL,
    credit_package_id UUID REFERENCES credit_packages(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_stripe_payment_user ON stripe_payments(user_id);
CREATE INDEX idx_stripe_payment_intent ON stripe_payments(stripe_payment_intent_id);
CREATE INDEX idx_stripe_payment_status ON stripe_payments(status);

-- Slot templates
CREATE TABLE slot_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL,
    admin_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_slot_template_admin ON slot_templates(admin_id);

CREATE TABLE template_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES slot_templates(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    capacity INTEGER DEFAULT 1,
    location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL
);

CREATE TABLE template_slot_pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_slot_id UUID NOT NULL REFERENCES template_slots(id) ON DELETE CASCADE,
    pricing_item_id UUID NOT NULL REFERENCES pricing_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_template_slot_pricing_item UNIQUE (template_slot_id, pricing_item_id)
);

CREATE INDEX idx_tspi_template_slot ON template_slot_pricing_items(template_slot_id);
CREATE INDEX idx_tspi_pricing_item ON template_slot_pricing_items(pricing_item_id);

-- Recurring reservations
CREATE TABLE recurring_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    weeks_count INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    pricing_item_id UUID REFERENCES pricing_items(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_recurring_user ON recurring_reservations(user_id);
CREATE INDEX idx_recurring_status ON recurring_reservations(status);

-- Cancellation policies
CREATE TABLE cancellation_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_refund_hours INTEGER NOT NULL DEFAULT 24,
    partial_refund_hours INTEGER,
    partial_refund_percentage INTEGER,
    no_refund_hours INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_cancellation_policy_trainer ON cancellation_policies(trainer_id);

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

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    recipients_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE body_measurements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    weight DOUBLE PRECISION,
    body_fat DOUBLE PRECISION,
    chest DOUBLE PRECISION,
    waist DOUBLE PRECISION,
    hips DOUBLE PRECISION,
    bicep DOUBLE PRECISION,
    thigh DOUBLE PRECISION,
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE training_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE workout_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id) ON DELETE CASCADE,
    exercises TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reminders_sent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_reminder_reservation_type UNIQUE (reservation_id, reminder_type)
);

CREATE TABLE credit_expiration_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES credit_transactions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    days_before INTEGER NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (transaction_id, days_before)
);

-- Indexes
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
-- idx_reservations_date removed (covered by idx_reservation_date_status)
CREATE INDEX idx_reservations_status ON reservations(status);
-- idx_credit_transactions_user_id removed (covered by idx_credit_tx_user_created)

CREATE INDEX idx_purchased_plans_user_id ON purchased_plans(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_verification_token_expires ON verification_tokens(expires_at);
CREATE INDEX idx_password_reset_token_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_token_expires ON password_reset_tokens(expires_at);
CREATE INDEX idx_reservations_date_time ON reservations(date, start_time);
CREATE INDEX idx_reservation_slot ON reservations(slot_id);
CREATE INDEX idx_reservation_user_status ON reservations(user_id, status);
CREATE INDEX idx_reservation_user_date ON reservations(user_id, date);
CREATE INDEX idx_reservation_date_status ON reservations(date, status);
CREATE INDEX idx_announcement_trainer ON announcements(trainer_id);
CREATE INDEX idx_measurement_user ON body_measurements(user_id);
CREATE INDEX idx_measurement_date ON body_measurements(user_id, date);
CREATE INDEX idx_feedback_reservation ON training_feedback(reservation_id);
CREATE INDEX idx_feedback_user ON training_feedback(user_id);
CREATE INDEX idx_workout_reservation ON workout_logs(reservation_id);
CREATE INDEX idx_reminder_reservation ON reminders_sent(reservation_id);
CREATE INDEX idx_reminder_user ON reminders_sent(user_id);
CREATE INDEX idx_reminder_sent_at ON reminders_sent(sent_at);
CREATE INDEX idx_credit_package_trainer ON credit_packages(trainer_id);
CREATE INDEX idx_client_note_client ON client_notes(client_id);
CREATE INDEX idx_client_note_admin ON client_notes(admin_id);

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
    location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL,
    capacity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uk_slots_date_start_admin UNIQUE (date, start_time, admin_id)
);

CREATE INDEX IF NOT EXISTS idx_slot_date_status ON slots(date, status);
CREATE INDEX IF NOT EXISTS idx_slot_assigned_user ON slots(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_slot_template ON slots(template_id);
CREATE INDEX IF NOT EXISTS idx_slot_admin ON slots(admin_id);
CREATE INDEX IF NOT EXISTS idx_availability_block_admin ON availability_blocks(admin_id);
CREATE INDEX IF NOT EXISTS idx_user_trainer ON users(trainer_id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'slots_date_start_time_key') THEN
        ALTER TABLE slots DROP CONSTRAINT slots_date_start_time_key;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_slots_date_start_admin') THEN
        ALTER TABLE slots ADD CONSTRAINT uk_slots_date_start_admin UNIQUE (date, start_time, admin_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS slot_pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id UUID NOT NULL REFERENCES slots(id) ON DELETE CASCADE,
    pricing_item_id UUID NOT NULL REFERENCES pricing_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_slot_pricing_item UNIQUE (slot_id, pricing_item_id)
);

CREATE INDEX IF NOT EXISTS idx_spi_slot ON slot_pricing_items(slot_id);
CREATE INDEX IF NOT EXISTS idx_spi_pricing_item ON slot_pricing_items(pricing_item_id);

-- Migrations for existing databases: create training_locations table if missing
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'training_locations') THEN
        CREATE TABLE training_locations (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name_cs VARCHAR(100) NOT NULL,
            name_en VARCHAR(100),
            address_cs TEXT,
            address_en TEXT,
            color VARCHAR(7) NOT NULL,
            is_active BOOLEAN DEFAULT true,
            admin_id UUID,
            created_at TIMESTAMP DEFAULT NOW()
        );
        CREATE INDEX IF NOT EXISTS idx_training_location_admin ON training_locations(admin_id);
        CREATE INDEX IF NOT EXISTS idx_training_location_active ON training_locations(is_active);
    END IF;
END $$;

-- Migrations for existing databases: add location_id to block/template/slot tables
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'availability_blocks' AND column_name = 'location_id') THEN
        ALTER TABLE availability_blocks ADD COLUMN location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'slot_templates')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'slot_templates' AND column_name = 'location_id') THEN
        ALTER TABLE slot_templates ADD COLUMN location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'slot_templates')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'slot_templates' AND column_name = 'admin_id') THEN
        ALTER TABLE slot_templates ADD COLUMN admin_id UUID REFERENCES users(id) ON DELETE CASCADE;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_slots')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'template_slots' AND column_name = 'capacity') THEN
        ALTER TABLE template_slots ADD COLUMN capacity INTEGER DEFAULT 1;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'slots' AND column_name = 'location_id') THEN
        ALTER TABLE slots ADD COLUMN location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'slots' AND column_name = 'capacity') THEN
        ALTER TABLE slots ADD COLUMN capacity INTEGER DEFAULT 1;
    END IF;
END $$;

-- Defensive CHECK constraints on financial/credit columns. The service layer
-- already guards these invariants; the DB-level checks are a last line of
-- defence against concurrency bugs that would otherwise corrupt balances.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_users_credits_nonneg') THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_credits_nonneg CHECK (credits >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_pricing_items_credits_pos') THEN
        ALTER TABLE pricing_items ADD CONSTRAINT chk_pricing_items_credits_pos CHECK (credits > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_credit_packages_credits_pos') THEN
        ALTER TABLE credit_packages ADD CONSTRAINT chk_credit_packages_credits_pos CHECK (credits > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_credit_packages_price_pos') THEN
        ALTER TABLE credit_packages ADD CONSTRAINT chk_credit_packages_price_pos CHECK (price_czk > 0);
    END IF;
END $$;

-- Relax legacy NOT NULL constraints on client_notes (the entity uses client_id/content;
-- older installs may still have note/user_id as NOT NULL from an older schema).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'client_notes' AND column_name = 'note' AND is_nullable = 'NO') THEN
        ALTER TABLE client_notes ALTER COLUMN note DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'client_notes' AND column_name = 'user_id' AND is_nullable = 'NO') THEN
        ALTER TABLE client_notes ALTER COLUMN user_id DROP NOT NULL;
    END IF;
    -- purchased_plans.credits_used was dropped from the entity; relax legacy NOT NULL
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'purchased_plans' AND column_name = 'credits_used' AND is_nullable = 'NO') THEN
        ALTER TABLE purchased_plans ALTER COLUMN credits_used DROP NOT NULL;
    END IF;
END $$;

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

-- Create cancellation_policies table if it doesn't exist (for existing databases)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'cancellation_policies') THEN
        CREATE TABLE cancellation_policies (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            trainer_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
            full_refund_hours INTEGER NOT NULL DEFAULT 24,
            partial_refund_hours INTEGER,
            partial_refund_percentage INTEGER,
            no_refund_hours INTEGER NOT NULL DEFAULT 0,
            is_active BOOLEAN NOT NULL DEFAULT true,
            created_at TIMESTAMP DEFAULT NOW(),
            updated_at TIMESTAMP DEFAULT NOW()
        );
        CREATE INDEX idx_cancellation_policy_trainer ON cancellation_policies(trainer_id);
    END IF;
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

-- Foreign Key constraints for Hibernate-managed tables
DO $$ BEGIN
  -- users
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_trainer') THEN
    ALTER TABLE users ADD CONSTRAINT fk_users_trainer FOREIGN KEY (trainer_id) REFERENCES users(id) ON DELETE SET NULL;
  END IF;
  -- slots
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_slots_admin') THEN
    ALTER TABLE slots ADD CONSTRAINT fk_slots_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL;
  END IF;
  -- Only add the FK if the referenced `slot_templates` table already exists —
  -- on a fresh install this FK block runs before Hibernate creates the table.
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_slots_template')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'slot_templates') THEN
    ALTER TABLE slots ADD CONSTRAINT fk_slots_template FOREIGN KEY (template_id) REFERENCES slot_templates(id) ON DELETE SET NULL;
  END IF;
  -- announcements (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_announcements_trainer')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'announcements') THEN
    ALTER TABLE announcements ADD CONSTRAINT fk_announcements_trainer FOREIGN KEY (trainer_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- body_measurements (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_measurements_user')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'body_measurements') THEN
    ALTER TABLE body_measurements ADD CONSTRAINT fk_measurements_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- client_notes (client_id added by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_client_notes_client')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'client_notes' AND column_name = 'client_id') THEN
    ALTER TABLE client_notes ADD CONSTRAINT fk_client_notes_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- credit_packages (trainer_id added by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_credit_packages_trainer')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'credit_packages' AND column_name = 'trainer_id') THEN
    ALTER TABLE credit_packages ADD CONSTRAINT fk_credit_packages_trainer FOREIGN KEY (trainer_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- pricing_items (admin_id added by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_pricing_items_admin')
     AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'pricing_items' AND column_name = 'admin_id') THEN
    ALTER TABLE pricing_items ADD CONSTRAINT fk_pricing_items_admin FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- password_reset_tokens (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_password_reset_user')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'password_reset_tokens') THEN
    ALTER TABLE password_reset_tokens ADD CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- training_feedback (created by Hibernate)
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'training_feedback') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_feedback_user') THEN
      ALTER TABLE training_feedback ADD CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_feedback_reservation') THEN
      ALTER TABLE training_feedback ADD CONSTRAINT fk_feedback_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE;
    END IF;
  END IF;
  -- workout_logs (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workout_reservation')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'workout_logs') THEN
    ALTER TABLE workout_logs ADD CONSTRAINT fk_workout_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE;
  END IF;
  -- stripe_payments (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stripe_user')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stripe_payments') THEN
    ALTER TABLE stripe_payments ADD CONSTRAINT fk_stripe_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- recurring_reservations (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_recurring_user')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recurring_reservations') THEN
    ALTER TABLE recurring_reservations ADD CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
  -- reminders_sent (created by Hibernate)
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'reminders_sent') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reminders_sent_user') THEN
      ALTER TABLE reminders_sent ADD CONSTRAINT fk_reminders_sent_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reminders_sent_reservation') THEN
      ALTER TABLE reminders_sent ADD CONSTRAINT fk_reminders_sent_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE;
    END IF;
  END IF;
  -- slot_pricing_items (created by Hibernate)
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'slot_pricing_items') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spi_slot') THEN
      ALTER TABLE slot_pricing_items ADD CONSTRAINT fk_spi_slot FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_spi_pricing') THEN
      ALTER TABLE slot_pricing_items ADD CONSTRAINT fk_spi_pricing FOREIGN KEY (pricing_item_id) REFERENCES pricing_items(id) ON DELETE CASCADE;
    END IF;
  END IF;
  -- template_slots (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_template_slots_template')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_slots')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'slot_templates') THEN
    ALTER TABLE template_slots ADD CONSTRAINT fk_template_slots_template FOREIGN KEY (template_id) REFERENCES slot_templates(id) ON DELETE CASCADE;
  END IF;
  -- template_slot_pricing_items (created by Hibernate)
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_slot_pricing_items') THEN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tspi_slot')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_slots') THEN
      ALTER TABLE template_slot_pricing_items ADD CONSTRAINT fk_tspi_slot FOREIGN KEY (template_slot_id) REFERENCES template_slots(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tspi_pricing') THEN
      ALTER TABLE template_slot_pricing_items ADD CONSTRAINT fk_tspi_pricing FOREIGN KEY (pricing_item_id) REFERENCES pricing_items(id) ON DELETE CASCADE;
    END IF;
  END IF;
  -- credit_expiration_notifications (created by Hibernate)
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_credit_exp_user')
     AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'credit_expiration_notifications') THEN
    ALTER TABLE credit_expiration_notifications ADD CONSTRAINT fk_credit_exp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
  END IF;
END $$;
