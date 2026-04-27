-- Idempotent production schema migrations for existing databases.
-- Docker's /docker-entrypoint-initdb.d scripts only run on a fresh volume, so
-- deploy applies this file before starting the backend in validate mode.

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

CREATE INDEX IF NOT EXISTS idx_training_location_admin ON training_locations(admin_id);
CREATE INDEX IF NOT EXISTS idx_training_location_active ON training_locations(is_active);

ALTER TABLE IF EXISTS availability_blocks
    ADD COLUMN IF NOT EXISTS admin_id UUID;

ALTER TABLE IF EXISTS availability_blocks
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS slot_templates
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS slot_templates
    ADD COLUMN IF NOT EXISTS admin_id UUID REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_slot_template_admin ON slot_templates(admin_id);

ALTER TABLE IF EXISTS template_slots
    ADD COLUMN IF NOT EXISTS capacity INTEGER DEFAULT 1;

ALTER TABLE IF EXISTS template_slots
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS slots
    ADD COLUMN IF NOT EXISTS admin_id UUID;

ALTER TABLE IF EXISTS slots
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES training_locations(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS slots
    ADD COLUMN IF NOT EXISTS capacity INT DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_slot_admin ON slots(admin_id);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS calendar_start_hour INTEGER DEFAULT 6;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS calendar_end_hour INTEGER DEFAULT 22;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS adjacent_booking_required BOOLEAN DEFAULT true;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS avatar_path VARCHAR(255);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS email_reminders_enabled BOOLEAN DEFAULT true;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS reminder_hours_before INT DEFAULT 24;

ALTER TABLE IF EXISTS reservations
    ADD COLUMN IF NOT EXISTS slot_id UUID REFERENCES slots(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS reservations
    ADD COLUMN IF NOT EXISTS pricing_item_id UUID REFERENCES pricing_items(id) ON DELETE SET NULL;

ALTER TABLE IF EXISTS reservations
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

ALTER TABLE IF EXISTS reservations
    ADD COLUMN IF NOT EXISTS recurring_reservation_id UUID;
