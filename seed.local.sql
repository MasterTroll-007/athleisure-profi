-- Local development seed data only. Production compose does not mount this file.

-- Password hash matches the existing development fixtures; change seeded
-- passwords immediately if this file is ever used outside local/dev data.
INSERT INTO users (email, password_hash, first_name, last_name, role, credits, email_verified)
VALUES
    ('admin@fitness.cz', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Admin', 'Fitness', 'admin', 999, true),
    ('admin@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Test', 'Admin', 'admin', 10, true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password_hash, first_name, last_name, role, credits, email_verified, trainer_id)
VALUES
    ('test1@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Jana', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
    ('test2@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Eva', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
    ('test3@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Lucka', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
    ('test4@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Petra', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com')),
    ('test5@test.com', '$2a$10$skxlXuQ1S42ImuhXx51LzuDPCv.aamV6QuQ6lv9k9qmIjh3z/fOTm', 'Misa', 'TestPrijmeni', 'client', 10, true, (SELECT id FROM users WHERE email = 'admin@test.com'))
ON CONFLICT (email) DO NOTHING;

WITH trainer AS (
    SELECT id FROM users WHERE email = 'admin@test.com'
), defaults(name_cs, name_en, credits, sort_order) AS (
    VALUES
        ('Jeden trénink', 'Single training', 1, 1),
        ('Trénink student', 'Student training', 1, 2),
        ('10 tréninků', '10 trainings', 10, 3),
        ('Individuální tréninkový plán', 'Individual training plan', 5, 4),
        ('Kompletní fitness plán', 'Complete fitness plan', 8, 5)
)
INSERT INTO pricing_items (name_cs, name_en, credits, sort_order, admin_id)
SELECT d.name_cs, d.name_en, d.credits, d.sort_order, trainer.id
FROM trainer CROSS JOIN defaults d
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_items p
    WHERE p.admin_id = trainer.id AND p.name_cs = d.name_cs
);

WITH trainer AS (
    SELECT id FROM users WHERE email = 'admin@test.com'
), defaults(name_cs, name_en, credits, bonus_credits, price_czk, sort_order) AS (
    VALUES
        ('1 kredit', '1 credit', 1, 0, 500.00, 1),
        ('5 kreditů', '5 credits', 5, 0, 2250.00, 2),
        ('10 kreditů', '10 credits', 10, 1, 4000.00, 3),
        ('20 kreditů', '20 credits', 20, 3, 7000.00, 4)
)
INSERT INTO credit_packages (name_cs, name_en, credits, bonus_credits, price_czk, sort_order, trainer_id)
SELECT d.name_cs, d.name_en, d.credits, d.bonus_credits, d.price_czk, d.sort_order, trainer.id
FROM trainer CROSS JOIN defaults d
WHERE NOT EXISTS (
    SELECT 1 FROM credit_packages cp
    WHERE cp.trainer_id = trainer.id AND cp.name_cs = d.name_cs
);

INSERT INTO availability_blocks (name, days_of_week, start_time, end_time, slot_duration_minutes, admin_id)
SELECT 'Odpolední blok', '1,2,3,4,5', '14:00', '18:00', 60, id
FROM users
WHERE email = 'admin@test.com'
  AND NOT EXISTS (
      SELECT 1 FROM availability_blocks ab
      WHERE ab.admin_id = users.id AND ab.name = 'Odpolední blok'
  );

-- Generate slots for current week + next week (Monday-Friday, 14:00-18:00)
-- with 50% of slots reserved by test users.
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
    monday_of_week := date_trunc('week', CURRENT_DATE)::date;

    SELECT array_agg(id ORDER BY email) INTO test_user_ids
    FROM users
    WHERE email LIKE 'test%@test.com' AND role = 'client'
    LIMIT 5;

    IF test_user_ids IS NULL OR array_length(test_user_ids, 1) = 0 THEN
        RAISE NOTICE 'No test users found, skipping slot generation';
        RETURN;
    END IF;

    FOR week_offset IN 0..1 LOOP
        FOR day_offset IN 0..4 LOOP
            slot_date := monday_of_week + (week_offset * 7 + day_offset);

            IF slot_date >= CURRENT_DATE THEN
                FOR hour_val IN 14..17 LOOP
                    slot_id := gen_random_uuid();

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
                    ON CONFLICT (date, start_time, admin_id) DO NOTHING;

                    IF FOUND AND slot_count % 2 = 0 THEN
                        test_user_id := test_user_ids[1 + (reservation_count % array_length(test_user_ids, 1))];

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

                        INSERT INTO credit_transactions (id, user_id, amount, type, note, created_at)
                        VALUES (
                            gen_random_uuid(),
                            test_user_id,
                            -1,
                            'reservation',
                            'Rezervace na ' || slot_date::text,
                            NOW()
                        );

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
