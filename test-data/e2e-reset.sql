-- Local E2E reset/seed only.
-- Do not run this against production. It intentionally resets admin@test.com
-- and test1/test2/test3@test.com into a deterministic automation state.

BEGIN;

CREATE TEMP TABLE e2e_known_emails(email TEXT PRIMARY KEY) ON COMMIT DROP;
INSERT INTO e2e_known_emails(email) VALUES
    ('admin@test.com'),
    ('test1@test.com'),
    ('test2@test.com'),
    ('test3@test.com');

CREATE TEMP TABLE e2e_users_to_reset ON COMMIT DROP AS
SELECT id
FROM users
WHERE email IN (SELECT email FROM e2e_known_emails)
   OR email LIKE 'e2e-client-%@e2e.test';

CREATE TEMP TABLE e2e_admin_ids ON COMMIT DROP AS
SELECT id FROM users WHERE email = 'admin@test.com';

CREATE TEMP TABLE e2e_slots_to_reset ON COMMIT DROP AS
SELECT s.id
FROM slots s
WHERE s.note LIKE 'E2E%'
   OR s.admin_id IN (SELECT id FROM e2e_admin_ids)
      AND s.date BETWEEN date_trunc('week', CURRENT_DATE + INTERVAL '14 days')::date
                     AND date_trunc('week', CURRENT_DATE + INTERVAL '49 days')::date;

CREATE TEMP TABLE e2e_reservations_to_reset ON COMMIT DROP AS
SELECT r.id
FROM reservations r
WHERE r.user_id IN (SELECT id FROM e2e_users_to_reset)
   OR r.slot_id IN (SELECT id FROM e2e_slots_to_reset);

DELETE FROM reminders_sent WHERE reservation_id IN (SELECT id FROM e2e_reservations_to_reset);
DELETE FROM workout_logs WHERE reservation_id IN (SELECT id FROM e2e_reservations_to_reset);
DELETE FROM training_feedback WHERE reservation_id IN (SELECT id FROM e2e_reservations_to_reset);

DELETE FROM credit_expiration_notifications
WHERE user_id IN (SELECT id FROM e2e_users_to_reset)
   OR transaction_id IN (
       SELECT id FROM credit_transactions WHERE user_id IN (SELECT id FROM e2e_users_to_reset)
   );

DELETE FROM credit_transactions WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM purchased_plans WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM gopay_payments WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM stripe_payments WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM recurring_reservations WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM body_measurements WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM client_notes
WHERE client_id IN (SELECT id FROM e2e_users_to_reset)
   OR admin_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM verification_tokens WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM e2e_users_to_reset);
DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM e2e_users_to_reset);

DELETE FROM reservations WHERE id IN (SELECT id FROM e2e_reservations_to_reset);
DELETE FROM slot_pricing_items WHERE slot_id IN (SELECT id FROM e2e_slots_to_reset);
DELETE FROM slots WHERE id IN (SELECT id FROM e2e_slots_to_reset);

DELETE FROM template_slot_pricing_items
WHERE template_slot_id IN (
    SELECT ts.id
    FROM template_slots ts
    JOIN slot_templates st ON st.id = ts.template_id
    WHERE st.name IN ('E2E Morning Template', 'E2E Weekend Template')
       OR st.name LIKE 'E2E %'
);
DELETE FROM template_slots
WHERE template_id IN (
    SELECT id FROM slot_templates
    WHERE name IN ('E2E Morning Template', 'E2E Weekend Template')
       OR name LIKE 'E2E %'
);
DELETE FROM slot_templates
WHERE name IN ('E2E Morning Template', 'E2E Weekend Template')
   OR name LIKE 'E2E %';

DELETE FROM pricing_items WHERE name_cs LIKE 'E2E %';
DELETE FROM training_locations WHERE name_cs LIKE 'E2E %';

DELETE FROM users
WHERE email LIKE 'e2e-client-%@e2e.test'
  AND email NOT IN (SELECT email FROM e2e_known_emails);

UPDATE users
SET invite_code = NULL
WHERE invite_code = 'E2EADMINCODE'
  AND email <> 'admin@test.com';

INSERT INTO users (
    email, password_hash, first_name, last_name, role, credits, locale, theme,
    email_verified, trainer_id, calendar_start_hour, calendar_end_hour,
    invite_code, is_blocked, adjacent_booking_required, email_reminders_enabled,
    reminder_hours_before, created_at, updated_at
)
VALUES (
    'admin@test.com',
    '$2a$10$BQnvaqNvN5Lg1nrE/6v1SOTMste/adiiU7O0qeYfYNw5Z3gGJQ54i',
    'E2E', 'Admin', 'admin', 100, 'cs', 'dark',
    true, NULL, 7, 21,
    'E2EADMINCODE', false, false, true,
    24, NOW(), NOW()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    credits = EXCLUDED.credits,
    locale = EXCLUDED.locale,
    theme = EXCLUDED.theme,
    email_verified = EXCLUDED.email_verified,
    trainer_id = NULL,
    calendar_start_hour = EXCLUDED.calendar_start_hour,
    calendar_end_hour = EXCLUDED.calendar_end_hour,
    invite_code = EXCLUDED.invite_code,
    is_blocked = false,
    adjacent_booking_required = false,
    email_reminders_enabled = true,
    reminder_hours_before = 24,
    updated_at = NOW();

INSERT INTO users (
    email, password_hash, first_name, last_name, role, credits, locale, theme,
    email_verified, trainer_id, is_blocked, adjacent_booking_required,
    email_reminders_enabled, reminder_hours_before, created_at, updated_at
)
SELECT v.email,
       '$2a$10$BQnvaqNvN5Lg1nrE/6v1SOTMste/adiiU7O0qeYfYNw5Z3gGJQ54i',
       v.first_name,
       v.last_name,
       'client',
       10,
       'cs',
       'dark',
       true,
       admin.id,
       false,
       true,
       true,
       24,
       NOW(),
       NOW()
FROM (VALUES
    ('test1@test.com', 'E2E', 'Client One'),
    ('test2@test.com', 'E2E', 'Client Two'),
    ('test3@test.com', 'E2E', 'Client Three')
) AS v(email, first_name, last_name)
CROSS JOIN (SELECT id FROM users WHERE email = 'admin@test.com') admin
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    role = EXCLUDED.role,
    credits = EXCLUDED.credits,
    locale = EXCLUDED.locale,
    theme = EXCLUDED.theme,
    email_verified = EXCLUDED.email_verified,
    trainer_id = EXCLUDED.trainer_id,
    is_blocked = false,
    adjacent_booking_required = true,
    email_reminders_enabled = true,
    reminder_hours_before = 24,
    updated_at = NOW();

INSERT INTO training_locations (
    id, name_cs, name_en, address_cs, address_en, color, is_active, admin_id, created_at
)
SELECT gen_random_uuid(), 'E2E Gym', 'E2E Gym', 'E2E Local Address', 'E2E Local Address',
       '#3B82F6', true, admin.id, NOW()
FROM users admin
WHERE admin.email = 'admin@test.com';

INSERT INTO pricing_items (
    id, name_cs, name_en, description_cs, description_en, credits, duration_minutes,
    is_active, sort_order, admin_id, created_at
)
SELECT gen_random_uuid(), 'E2E Jeden trenink', 'E2E Single training',
       'E2E one-credit booking', 'E2E one-credit booking',
       1, 60, true, 1, admin.id, NOW()
FROM users admin
WHERE admin.email = 'admin@test.com';

DO $$
DECLARE
    v_admin_id UUID;
    v_pricing_id UUID;
    v_location_id UUID;
    base_monday DATE;
    slot_date DATE;
    slot_id UUID;
    morning_template_id UUID;
    weekend_template_id UUID;
    template_slot_id UUID;
    offset_day INT;
BEGIN
    SELECT id INTO v_admin_id FROM users WHERE email = 'admin@test.com';
    SELECT id INTO v_pricing_id FROM pricing_items WHERE name_cs = 'E2E Jeden trenink' AND admin_id = v_admin_id LIMIT 1;
    SELECT id INTO v_location_id FROM training_locations WHERE name_cs = 'E2E Gym' AND admin_id = v_admin_id LIMIT 1;
    base_monday := date_trunc('week', CURRENT_DATE + INTERVAL '21 days')::date;

    FOR offset_day IN 0..6 LOOP
        slot_date := base_monday + offset_day;
        INSERT INTO slots (
            id, date, start_time, end_time, duration_minutes, status,
            admin_id, location_id, capacity, note, created_at
        )
        VALUES (
            gen_random_uuid(), slot_date, TIME '09:00', TIME '10:00', 60,
            CASE WHEN offset_day IN (1, 3) THEN 'LOCKED' ELSE 'UNLOCKED' END,
            v_admin_id, v_location_id, 1, 'E2E seeded week slot', NOW()
        )
        RETURNING id INTO slot_id;

        INSERT INTO slot_pricing_items (id, slot_id, pricing_item_id)
        VALUES (gen_random_uuid(), slot_id, v_pricing_id)
        ON CONFLICT ON CONSTRAINT uk_slot_pricing_item DO NOTHING;
    END LOOP;

    INSERT INTO slots (
        id, date, start_time, end_time, duration_minutes, status,
        admin_id, location_id, capacity, note, created_at
    )
    VALUES
        (gen_random_uuid(), base_monday, TIME '10:00', TIME '11:00', 60, 'UNLOCKED', v_admin_id, v_location_id, 1, 'E2E double booking slot', NOW()),
        (gen_random_uuid(), base_monday + 1, TIME '12:00', TIME '13:00', 60, 'UNLOCKED', v_admin_id, v_location_id, 1, 'E2E admin reservation slot', NOW()),
        (gen_random_uuid(), base_monday + 2, TIME '11:00', TIME '12:00', 60, 'LOCKED', v_admin_id, v_location_id, 1, 'E2E locked visibility slot', NOW());

    INSERT INTO slot_pricing_items (id, slot_id, pricing_item_id)
    SELECT gen_random_uuid(), id, v_pricing_id
    FROM slots
    WHERE admin_id = v_admin_id
      AND note LIKE 'E2E %'
      AND date BETWEEN base_monday AND base_monday + 6
    ON CONFLICT ON CONSTRAINT uk_slot_pricing_item DO NOTHING;

    INSERT INTO slot_templates (id, name, is_active, location_id, admin_id, created_at)
    VALUES (gen_random_uuid(), 'E2E Morning Template', true, v_location_id, v_admin_id, NOW())
    RETURNING id INTO morning_template_id;

    INSERT INTO template_slots (
        id, template_id, day_of_week, start_time, end_time, duration_minutes, capacity, location_id
    )
    VALUES
        (gen_random_uuid(), morning_template_id, 'MONDAY', TIME '08:00', TIME '09:00', 60, 1, v_location_id),
        (gen_random_uuid(), morning_template_id, 'WEDNESDAY', TIME '08:00', TIME '09:00', 60, 1, v_location_id);

    FOR template_slot_id IN SELECT id FROM template_slots WHERE template_id = morning_template_id LOOP
        INSERT INTO template_slot_pricing_items (id, template_slot_id, pricing_item_id)
        VALUES (gen_random_uuid(), template_slot_id, v_pricing_id)
        ON CONFLICT ON CONSTRAINT uk_template_slot_pricing_item DO NOTHING;
    END LOOP;

    INSERT INTO slot_templates (id, name, is_active, location_id, admin_id, created_at)
    VALUES (gen_random_uuid(), 'E2E Weekend Template', true, v_location_id, v_admin_id, NOW())
    RETURNING id INTO weekend_template_id;

    INSERT INTO template_slots (
        id, template_id, day_of_week, start_time, end_time, duration_minutes, capacity, location_id
    )
    VALUES
        (gen_random_uuid(), weekend_template_id, 'SATURDAY', TIME '10:00', TIME '11:00', 60, 1, v_location_id),
        (gen_random_uuid(), weekend_template_id, 'SUNDAY', TIME '10:00', TIME '11:00', 60, 1, v_location_id);

    FOR template_slot_id IN SELECT id FROM template_slots WHERE template_id = weekend_template_id LOOP
        INSERT INTO template_slot_pricing_items (id, template_slot_id, pricing_item_id)
        VALUES (gen_random_uuid(), template_slot_id, v_pricing_id)
        ON CONFLICT ON CONSTRAINT uk_template_slot_pricing_item DO NOTHING;
    END LOOP;
END $$;

COMMIT;
