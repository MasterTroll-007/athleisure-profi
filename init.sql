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
    created_at TIMESTAMP DEFAULT NOW()
);

-- Reservations
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

-- Indexes
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_date ON reservations(date);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_purchased_plans_user_id ON purchased_plans(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Insert default admin user (password: admin123)
INSERT INTO users (email, password_hash, first_name, last_name, role, credits)
VALUES ('admin@fitness.cz', '$2a$10$N9qo8uLOickgx2ZMRZoHK.ZS1xHzKLEDKJ3bGxSRXqzEvdNKqNqWy', 'Admin', 'Fitness', 'admin', 999);

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

-- Insert default availability block
INSERT INTO availability_blocks (name, days_of_week, start_time, end_time, slot_duration_minutes)
VALUES ('Odpolední blok', '1,2,3,4,5', '14:00', '18:00', 60);
