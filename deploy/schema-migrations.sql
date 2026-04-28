-- Idempotent production schema catch-up for existing PostgreSQL databases.
-- The backend runs with spring.jpa.hibernate.ddl-auto=validate in production,
-- so every table/column used by the current JPA model must exist before boot.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "unaccent";

CREATE TABLE IF NOT EXISTS users (
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

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'client';
ALTER TABLE users ADD COLUMN IF NOT EXISTS credits INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(5) DEFAULT 'cs';
ALTER TABLE users ADD COLUMN IF NOT EXISTS theme VARCHAR(10) DEFAULT 'system';
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS trainer_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS calendar_start_hour INTEGER DEFAULT 6;
ALTER TABLE users ADD COLUMN IF NOT EXISTS calendar_end_hour INTEGER DEFAULT 22;
ALTER TABLE users ADD COLUMN IF NOT EXISTS invite_code VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS adjacent_booking_required BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_reminders_enabled BOOLEAN DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_hours_before INTEGER DEFAULT 24;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    description_cs TEXT,
    description_en TEXT,
    credits INTEGER NOT NULL,
    duration_minutes INTEGER DEFAULT 60,
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS name_en VARCHAR(255);
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS description_cs TEXT;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS description_en TEXT;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS duration_minutes INTEGER DEFAULT 60;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS admin_id UUID;
ALTER TABLE pricing_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS training_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_cs VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    address_cs TEXT,
    address_en TEXT,
    color VARCHAR(7) NOT NULL DEFAULT '#ffb347',
    is_active BOOLEAN DEFAULT true,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS availability_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100),
    days_of_week VARCHAR(50) NOT NULL DEFAULT '',
    day_of_week VARCHAR(20),
    specific_date DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER DEFAULT 60,
    slot_duration INTEGER,
    break_after_slots INTEGER,
    break_duration_minutes INTEGER,
    is_recurring BOOLEAN DEFAULT true,
    is_blocked BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    admin_id UUID,
    location_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS day_of_week VARCHAR(20);
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS specific_date DATE;
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS slot_duration INTEGER;
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN DEFAULT true;
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN DEFAULT false;
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS admin_id UUID;
ALTER TABLE availability_blocks ADD COLUMN IF NOT EXISTS location_id UUID;

CREATE TABLE IF NOT EXISTS credit_packages (
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
    sort_order INTEGER DEFAULT 0,
    highlight_type VARCHAR(30) DEFAULT 'NONE',
    is_basic BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS trainer_id UUID;
ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS bonus_credits INTEGER DEFAULT 0;
ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'CZK';
ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS highlight_type VARCHAR(30) DEFAULT 'NONE';
ALTER TABLE credit_packages ADD COLUMN IF NOT EXISTS is_basic BOOLEAN DEFAULT false;

CREATE TABLE IF NOT EXISTS credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference_id UUID,
    gopay_payment_id VARCHAR(255),
    stripe_payment_id VARCHAR(255),
    note TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS stripe_payment_id VARCHAR(255);
ALTER TABLE credit_transactions ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) DEFAULT '' NOT NULL,
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
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS name VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS name_en VARCHAR(255);
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS description_cs TEXT;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS description_en TEXT;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS preview_image VARCHAR(500);
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) DEFAULT 0 NOT NULL;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'CZK';
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS validity_days INTEGER DEFAULT 30;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS sessions_count INTEGER;
ALTER TABLE training_plans ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;

CREATE TABLE IF NOT EXISTS slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    assigned_user_id UUID,
    note TEXT,
    template_id UUID,
    admin_id UUID,
    location_id UUID,
    capacity INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE slots ADD COLUMN IF NOT EXISTS duration_minutes INTEGER DEFAULT 60;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS assigned_user_id UUID;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS note TEXT;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS template_id UUID;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS admin_id UUID;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS location_id UUID;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS capacity INTEGER DEFAULT 1;
ALTER TABLE slots ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    block_id UUID,
    slot_id UUID,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'confirmed',
    credits_used INTEGER DEFAULT 1,
    pricing_item_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    cancelled_at TIMESTAMP,
    completed_at TIMESTAMP,
    recurring_reservation_id UUID,
    note TEXT
);

ALTER TABLE reservations ADD COLUMN IF NOT EXISTS slot_id UUID;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS pricing_item_id UUID;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS recurring_reservation_id UUID;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS note TEXT;

CREATE TABLE IF NOT EXISTS purchased_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    purchase_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE NOT NULL DEFAULT (CURRENT_DATE + INTERVAL '30 days'),
    sessions_remaining INTEGER,
    gopay_payment_id VARCHAR(255),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS purchase_date DATE DEFAULT CURRENT_DATE;
ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS expiry_date DATE DEFAULT (CURRENT_DATE + INTERVAL '30 days');
ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS sessions_remaining INTEGER;
ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS gopay_payment_id VARCHAR(255);
ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'active';
ALTER TABLE purchased_plans ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS client_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID,
    admin_id UUID,
    content TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE client_notes ADD COLUMN IF NOT EXISTS client_id UUID;
ALTER TABLE client_notes ADD COLUMN IF NOT EXISTS admin_id UUID;
ALTER TABLE client_notes ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE client_notes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW();

CREATE TABLE IF NOT EXISTS gopay_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    gopay_id BIGINT,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    state VARCHAR(30) NOT NULL,
    credit_package_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS stripe_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    stripe_session_id VARCHAR(255) UNIQUE NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'CZK',
    status VARCHAR(30) NOT NULL,
    credit_package_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS slot_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    location_id UUID,
    admin_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE slot_templates ADD COLUMN IF NOT EXISTS location_id UUID;
ALTER TABLE slot_templates ADD COLUMN IF NOT EXISTS admin_id UUID;

CREATE TABLE IF NOT EXISTS template_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    capacity INTEGER DEFAULT 1,
    location_id UUID
);

ALTER TABLE template_slots ADD COLUMN IF NOT EXISTS capacity INTEGER DEFAULT 1;
ALTER TABLE template_slots ADD COLUMN IF NOT EXISTS location_id UUID;

CREATE TABLE IF NOT EXISTS template_slot_pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_slot_id UUID NOT NULL,
    pricing_item_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS slot_pricing_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id UUID NOT NULL,
    pricing_item_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS recurring_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    weeks_count INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    pricing_item_id UUID,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cancellation_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL UNIQUE,
    full_refund_hours INTEGER NOT NULL DEFAULT 24,
    partial_refund_hours INTEGER,
    partial_refund_percentage INTEGER,
    no_refund_hours INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    recipients_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS body_measurements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    date DATE NOT NULL,
    weight DOUBLE PRECISION,
    body_fat DOUBLE PRECISION,
    chest DOUBLE PRECISION,
    waist DOUBLE PRECISION,
    hips DOUBLE PRECISION,
    bicep DOUBLE PRECISION,
    thigh DOUBLE PRECISION,
    notes TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS training_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workout_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL UNIQUE,
    exercises TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reminders_sent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS credit_expiration_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    user_id UUID NOT NULL,
    days_before INTEGER NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS waitlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    created_at TIMESTAMP DEFAULT NOW(),
    notified_at TIMESTAMP,
    expires_at TIMESTAMP
);

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
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'purchased_plans' AND column_name = 'credits_used' AND is_nullable = 'NO') THEN
        ALTER TABLE purchased_plans ALTER COLUMN credits_used DROP NOT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'slots_date_start_time_key') THEN
        ALTER TABLE slots DROP CONSTRAINT slots_date_start_time_key;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_slots_date_start_admin') THEN
        ALTER TABLE slots ADD CONSTRAINT uk_slots_date_start_admin UNIQUE (date, start_time, admin_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_slot_pricing_item') THEN
        ALTER TABLE slot_pricing_items ADD CONSTRAINT uk_slot_pricing_item UNIQUE (slot_id, pricing_item_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_template_slot_pricing_item') THEN
        ALTER TABLE template_slot_pricing_items ADD CONSTRAINT uk_template_slot_pricing_item UNIQUE (template_slot_id, pricing_item_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_waitlist_user_slot') THEN
        ALTER TABLE waitlist_entries ADD CONSTRAINT uk_waitlist_user_slot UNIQUE (user_id, slot_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_reminder_reservation_type') THEN
        ALTER TABLE reminders_sent ADD CONSTRAINT uk_reminder_reservation_type UNIQUE (reservation_id, reminder_type);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_credit_expiration_notification') THEN
        ALTER TABLE credit_expiration_notifications ADD CONSTRAINT uk_credit_expiration_notification UNIQUE (transaction_id, days_before);
    END IF;
END $$;

UPDATE users
SET invite_code = LOWER(SUBSTRING(MD5(RANDOM()::TEXT || id::TEXT), 1, 8))
WHERE role = 'admin' AND invite_code IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_trainer ON users(trainer_id);
CREATE INDEX IF NOT EXISTS idx_user_invite_code ON users(invite_code);
CREATE INDEX IF NOT EXISTS idx_pricing_items_admin ON pricing_items(admin_id);
CREATE INDEX IF NOT EXISTS idx_training_location_admin ON training_locations(admin_id);
CREATE INDEX IF NOT EXISTS idx_training_location_active ON training_locations(is_active);
CREATE INDEX IF NOT EXISTS idx_availability_block_admin ON availability_blocks(admin_id);
CREATE INDEX IF NOT EXISTS idx_credit_package_trainer ON credit_packages(trainer_id);
CREATE INDEX IF NOT EXISTS idx_credit_tx_user_created ON credit_transactions(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_stripe_payment_user ON stripe_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_stripe_payment_intent ON stripe_payments(stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_stripe_payment_status ON stripe_payments(status);
CREATE INDEX IF NOT EXISTS idx_slot_date_status ON slots(date, status);
CREATE INDEX IF NOT EXISTS idx_slot_assigned_user ON slots(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_slot_template ON slots(template_id);
CREATE INDEX IF NOT EXISTS idx_slot_admin ON slots(admin_id);
CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservation_slot ON reservations(slot_id);
CREATE INDEX IF NOT EXISTS idx_reservation_user_status ON reservations(user_id, status);
CREATE INDEX IF NOT EXISTS idx_reservation_user_date ON reservations(user_id, date);
CREATE INDEX IF NOT EXISTS idx_reservation_date_status ON reservations(date, status);
CREATE INDEX IF NOT EXISTS idx_purchased_plans_user_id ON purchased_plans(user_id);
CREATE INDEX IF NOT EXISTS idx_client_note_client ON client_notes(client_id);
CREATE INDEX IF NOT EXISTS idx_client_note_admin ON client_notes(admin_id);
CREATE INDEX IF NOT EXISTS idx_slot_template_admin ON slot_templates(admin_id);
CREATE INDEX IF NOT EXISTS idx_tspi_template_slot ON template_slot_pricing_items(template_slot_id);
CREATE INDEX IF NOT EXISTS idx_tspi_pricing_item ON template_slot_pricing_items(pricing_item_id);
CREATE INDEX IF NOT EXISTS idx_spi_slot ON slot_pricing_items(slot_id);
CREATE INDEX IF NOT EXISTS idx_spi_pricing_item ON slot_pricing_items(pricing_item_id);
CREATE INDEX IF NOT EXISTS idx_recurring_user ON recurring_reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_status ON recurring_reservations(status);
CREATE INDEX IF NOT EXISTS idx_cancellation_policy_trainer ON cancellation_policies(trainer_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_token_expires ON verification_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_user ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_expires ON password_reset_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_announcement_trainer ON announcements(trainer_id);
CREATE INDEX IF NOT EXISTS idx_measurement_user ON body_measurements(user_id);
CREATE INDEX IF NOT EXISTS idx_measurement_date ON body_measurements(user_id, date);
CREATE INDEX IF NOT EXISTS idx_feedback_reservation ON training_feedback(reservation_id);
CREATE INDEX IF NOT EXISTS idx_feedback_user ON training_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_workout_reservation ON workout_logs(reservation_id);
CREATE INDEX IF NOT EXISTS idx_reminder_reservation ON reminders_sent(reservation_id);
CREATE INDEX IF NOT EXISTS idx_reminder_user ON reminders_sent(user_id);
CREATE INDEX IF NOT EXISTS idx_reminder_sent_at ON reminders_sent(sent_at);
CREATE INDEX IF NOT EXISTS idx_waitlist_slot_status ON waitlist_entries(slot_id, status);
CREATE INDEX IF NOT EXISTS idx_waitlist_user ON waitlist_entries(user_id);
