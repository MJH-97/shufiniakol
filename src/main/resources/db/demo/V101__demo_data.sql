-- =====================================================================
-- DEMO / TEST DATA, part 2  (runs after V100__demo_data.sql)
-- Loaded only when FLYWAY_LOCATIONS includes classpath:db/demo (docker-compose default, or the `dev` profile).
-- Fills the tables V100 leaves empty, so every table has rows to test with:
--   users, menu_items, offer_availability, otp_codes, refresh_tokens, addresses, favorites, price_alerts,
--   orders, order_items, order_item_addons, order_status_history, payments, reviews, notifications.
-- Fixed UUIDs make it re-readable; amounts are consistent with OrderService (line = (price+addons)*qty,
-- total = subtotal + fee, saving = (original - price)*qty, commission = 10% of subtotal).
-- Remove this folder from FLYWAY_LOCATIONS for production.
-- =====================================================================

-- ---------- Users ----------
-- V100 already has: admin (+963900000001) and owner (+963900000002, owns Burger House + Pizza Time).
insert into users (id, phone, full_name, email, language, role, status, last_login_at) values
                                                                                           ('b0000000-0000-4000-8000-000000000001', '+963900000003', 'صاحب مطعم ٢',      'owner2@example.com',   'ar', 'RESTAURANT_OWNER', 'ACTIVE',  now() - interval '2 hours'),
                                                                                           ('c0000000-0000-4000-8000-000000000001', '+963911000001', 'أحمد الخطيب',      'ahmad@example.com',    'ar', 'CUSTOMER',         'ACTIVE',  now() - interval '1 hour'),
                                                                                           ('c0000000-0000-4000-8000-000000000002', '+963911000002', 'ليلى حداد',        'layla@example.com',    'en', 'CUSTOMER',         'ACTIVE',  now() - interval '3 hours'),
                                                                                           ('c0000000-0000-4000-8000-000000000003', '+963911000003', 'عمر درويش',        null,                   'fr', 'CUSTOMER',         'ACTIVE',  now() - interval '1 day'),
                                                                                           ('c0000000-0000-4000-8000-000000000004', '+963911000004', 'سارة منصور',       'sara@example.com',     'ar', 'CUSTOMER',         'ACTIVE',  now() - interval '30 minutes'),
                                                                                           ('c0000000-0000-4000-8000-000000000005', '+963911000005', 'مستخدم محظور',     null,                   'ar', 'CUSTOMER',         'BLOCKED', now() - interval '20 days');

-- owner2 takes the two restaurants V100 left without an owner (Sultan Shawarma + Broasted Al-Sham)
update restaurants set owner_id = 'b0000000-0000-4000-8000-000000000001'
where id in ('8e182880-7522-5700-a577-48a458ef0654', 'fe8ef563-9004-5ed6-85bf-0701051ce5cd');

-- ---------- Auth tables ----------
-- code_hash = sha256(phone:code:pepper) with the default pepper "dev-pepper" (OTP_PEPPER). Dev login always accepts OTP_DEV_FIXED_CODE anyway.
insert into otp_codes (phone, code_hash, attempts, expires_at, consumed_at, created_at) values
                                                                                            ('+963911000001', encode(sha256(convert_to('+963911000001:123456:dev-pepper', 'UTF8')), 'hex'), 1, now() - interval '59 minutes', now() - interval '1 hour',      now() - interval '1 hour 4 minutes'),
                                                                                            ('+963911000002', encode(sha256(convert_to('+963911000002:654321:dev-pepper', 'UTF8')), 'hex'), 0, now() + interval '4 minutes',  null,                            now() - interval '1 minute'),
                                                                                            ('+963911000003', encode(sha256(convert_to('+963911000003:111111:dev-pepper', 'UTF8')), 'hex'), 5, now() - interval '2 days',     null,                            now() - interval '2 days 5 minutes');

insert into refresh_tokens (user_id, token_hash, expires_at, revoked_at, created_at) values
                                                                                         ('c0000000-0000-4000-8000-000000000001', encode(sha256(convert_to('demo-refresh-token-1', 'UTF8')), 'hex'), now() + interval '29 days', null,                     now() - interval '1 day'),
                                                                                         ('c0000000-0000-4000-8000-000000000002', encode(sha256(convert_to('demo-refresh-token-2', 'UTF8')), 'hex'), now() + interval '28 days', null,                     now() - interval '2 days'),
                                                                                         ('c0000000-0000-4000-8000-000000000003', encode(sha256(convert_to('demo-refresh-token-3', 'UTF8')), 'hex'), now() + interval '20 days', now() - interval '1 day',   now() - interval '10 days'),
                                                                                         ('c0000000-0000-4000-8000-000000000004', encode(sha256(convert_to('demo-refresh-token-4', 'UTF8')), 'hex'), now() - interval '1 day',   null,                     now() - interval '31 days');

-- ---------- Addresses ----------
insert into addresses (id, user_id, type, label, city, area, street, building, floor, apartment, landmark, latitude, longitude, is_primary) values
                                                                                                                                                ('a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000001', 'HOME',  'البيت',  'دمشق', 'المزة',     'شارع المزة',        '12', '3', '5',  'قرب جامع المزة',          33.5120, 36.2560, true),
                                                                                                                                                ('a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000001', 'WORK',  'الشغل',  'دمشق', 'أبو رمانة', 'شارع الجلاء',       '7',  '2', '4',  'مقابل البنك',             33.5190, 36.2800, false),
                                                                                                                                                ('a0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000002', 'HOME',  'البيت',  'دمشق', 'الصالحية',  'شارع الصالحية',     '21', '1', '2',  null,                      33.5205, 36.2960, true),
                                                                                                                                                ('a0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000003', 'HOME',  'البيت',  'دمشق', 'باب توما',  'الشارع المستقيم',   '5',  '4', '8',  'بجانب كنيسة',             33.5125, 36.2995, true),
                                                                                                                                                ('a0000000-0000-4000-8000-000000000005', 'c0000000-0000-4000-8000-000000000004', 'HOME',  'البيت',  'دمشق', 'شعلان',     'شارع الجلاء',       '9',  '5', '10', null,                      33.5160, 36.2900, true),
                                                                                                                                                ('a0000000-0000-4000-8000-000000000006', 'c0000000-0000-4000-8000-000000000004', 'OTHER', 'بيت أهلي','دمشق', 'المزة',     'فيلات غربية',       '3',  null, null, 'خلف المدرسة',            33.5100, 36.2580, false);

-- ---------- Menu items ----------
-- Names match V100's offer_items, so the update at the end can link them (offer_items.menu_item_id).
insert into menu_items (restaurant_id, category_id, name, description, price, available, sort_order) values
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='burger'),   'برغر دبل',        'قطعتين لحم مشوي على الفحم',   30000, true, 0),
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='burger'),   'برغر لحم',        'قطعة لحم مع الخضار والصوص',    22000, true, 1),
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='snacks'),   'بطاطا',           'بطاطا مقلية مقرمشة',            8000, true, 2),
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='snacks'),   'ناغتس',           '6 قطع ناغتس دجاج',             14000, true, 3),
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='snacks'),   'صوص',            'صوص حسب الاختيار',              2000, true, 4),
                                                                                                         ('c15f4d87-a59e-5b0a-9580-a4a844790b09', (select id from categories where slug='drinks'),   'مشروب',           'مشروب غازي بارد',               5000, true, 5),

                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='shawarma'), 'شاورما دجاج',     'شاورما دجاج مع الثوم والمخلل', 12000, true, 0),
                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='shawarma'), 'شاورما لحم',      'شاورما لحم على أصولها',        18000, true, 1),
                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='snacks'),   'بطاطا',           'بطاطا مقلية',                   8000, true, 2),
                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='snacks'),   'ثوم',             'علبة ثوم',                      1000, true, 3),
                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='snacks'),   'مخلل',            'مخلل مشكل',                     1000, true, 4),
                                                                                                         ('8e182880-7522-5700-a577-48a458ef0654', (select id from categories where slug='drinks'),   'مشروب',           'مشروب غازي بارد',               5000, true, 5),

                                                                                                         ('2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', (select id from categories where slug='pizza'),    'بيتزا وسط',       'بيتزا وسط بعجينة إيطالية',     35000, true,  0),
                                                                                                         ('2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', (select id from categories where slug='pizza'),    'بيتزا كبيرة',     'بيتزا كبيرة تكفي 3 أشخاص',     55000, true,  1),
                                                                                                         ('2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', (select id from categories where slug='snacks'),   'خبز بالثوم',     'خبز محمص بالثوم والزبدة',      12000, true,  2),
                                                                                                         ('2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', (select id from categories where slug='drinks'),   'مشروب',           'مشروب غازي بارد',               5000, true,  3),
                                                                                                         ('2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', (select id from categories where slug='desserts'), 'تيراميسو',        'غير متوفر حالياً (للاختبار)',  15000, false, 4),

                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='broasted'), '4 قطع بروستد',    '4 قطع بروستد مقرمشة',          30000, true, 0),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='broasted'), '8 قطع بروستد',    '8 قطع بروستد مقرمشة',          58000, true, 1),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'بطاطا',           'بطاطا مقلية',                   8000, true, 2),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'بطاطا كبيرة',     'حصة بطاطا كبيرة',              12000, true, 3),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'ثوم',             'علبة ثوم',                      1000, true, 4),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'مخلل',            'مخلل مشكل',                     1000, true, 5),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'خبز',             'خبز عربي',                      3000, true, 6),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='snacks'),   'صوص',            'صوص حسب الاختيار',              2000, true, 7),
                                                                                                         ('fe8ef563-9004-5ed6-85bf-0701051ce5cd', (select id from categories where slug='drinks'),   'مشروب كبير',      'مشروب غازي 1 لتر',              8000, true, 8),

                                                                                                         ('0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', (select id from categories where slug='snacks'),   'ناغتس',           'ناغتس دجاج',                   14000, true, 0),
                                                                                                         ('0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', (select id from categories where slug='snacks'),   'بطاطا',           'بطاطا مقلية',                   8000, true, 1),
                                                                                                         ('0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', (select id from categories where slug='snacks'),   'أصابع موزاريلا',  'أصابع جبنة موزاريلا مقلية',    15000, true, 2),
                                                                                                         ('0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', (select id from categories where slug='snacks'),   'سناك',            'سناك خفيف',                     8000, true, 3),
                                                                                                         ('0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', (select id from categories where slug='drinks'),   'مشروب',           'مشروب بارد',                    5000, true, 4),

                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='desserts'), 'تشيز كيك',        'قطعة تشيز كيك',                18000, true, 0),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='desserts'), 'بقلاوة',          'بقلاوة بالفستق',               12000, true, 1),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='desserts'), 'كنافة',           'كنافة نابلسية',                14000, true, 2),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='desserts'), 'مبرومة',          'مبرومة بالفستق',               13000, true, 3),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='desserts'), 'كرواسون',         'كرواسون طازج',                  8000, true, 4),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='coffee'),   'قهوة',            'فنجان قهوة',                    8000, true, 5),
                                                                                                         ('92f2cfc0-1287-5f9f-b550-d7c1936c0f46', (select id from categories where slug='coffee'),   'لاتيه',           'لاتيه ساخن',                   10000, true, 6);

-- link V100's offer_items to the menu items created above (same restaurant + same name)
update offer_items oi
set menu_item_id = mi.id
    from offers o, menu_items mi
where oi.offer_id = o.id
  and mi.restaurant_id = o.restaurant_id
  and mi.name = oi.name
  and oi.menu_item_id is null;

-- ---------- Weekly availability windows (ISO day: 1=Mon .. 7=Sun) ----------
-- Offers without rows are available whenever they're active; these two have a weekly schedule.
insert into offer_availability (offer_id, day_of_week, start_time, end_time) values
                                                                                 ('36f4bc74-94a2-5d49-a21e-5f912c6277ad', 4, '12:00', '23:30'),  -- family broasted: Thu-Sun
                                                                                 ('36f4bc74-94a2-5d49-a21e-5f912c6277ad', 5, '12:00', '23:30'),
                                                                                 ('36f4bc74-94a2-5d49-a21e-5f912c6277ad', 6, '12:00', '23:30'),
                                                                                 ('36f4bc74-94a2-5d49-a21e-5f912c6277ad', 7, '12:00', '23:30'),
                                                                                 ('59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 5, '12:00', '23:59'),  -- family pizza: Fri-Sun
                                                                                 ('59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 6, '12:00', '23:59'),
                                                                                 ('59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 7, '12:00', '23:59');

-- ---------- Orders ----------
-- Cover every status, both delivery types, both payment methods. Customers: c..01 Ahmad, c..02 Layla, c..03 Omar, c..04 Sara.
insert into orders (id, order_number, user_id, restaurant_id, status, delivery_type, subtotal, delivery_fee, total, saving_total, commission_amount,
                    driver_notes, contact_phone, delivery_address_text, delivery_latitude, delivery_longitude, estimated_minutes,
                    client_request_id, cancelled_reason, delivered_at, created_at, updated_at) values
                                                                                                   ('d0000000-0000-4000-8000-000000000001', 'SFA-1001', 'c0000000-0000-4000-8000-000000000001', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', 'DELIVERED',        'DELIVERY', 70000, 4000, 74000, 21000, 7000,
                                                                                                    'الرجاء الاتصال عند الوصول', '+963911000001', 'دمشق، المزة، شارع المزة، بناء 12، طابق 3، شقة 5', 33.5120, 36.2560, 25, 'seed-req-1',  null,
                                                                                                    now() - interval '5 days' + interval '50 minutes', now() - interval '5 days',  now() - interval '5 days' + interval '50 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000002', 'SFA-1002', 'c0000000-0000-4000-8000-000000000002', '8e182880-7522-5700-a577-48a458ef0654', 'DELIVERED',        'DELIVERY', 48000, 0,    48000, 12000, 4800,
                                                                                                    null,                         '+963911000002', 'دمشق، الصالحية، شارع الصالحية، بناء 21، طابق 1، شقة 2', 33.5205, 36.2960, 25, 'seed-req-2',  null,
                                                                                                    now() - interval '4 days' + interval '40 minutes', now() - interval '4 days',  now() - interval '4 days' + interval '40 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000003', 'SFA-1003', 'c0000000-0000-4000-8000-000000000003', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', 'DELIVERED',        'PICKUP',   46000, 0,    46000, 11000, 4600,
                                                                                                    null,                         '+963911000003', null, null, null, 15, 'seed-req-3',  null,
                                                                                                    now() - interval '3 days' + interval '30 minutes', now() - interval '3 days',  now() - interval '3 days' + interval '30 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000004', 'SFA-1004', 'c0000000-0000-4000-8000-000000000004', 'fe8ef563-9004-5ed6-85bf-0701051ce5cd', 'OUT_FOR_DELIVERY', 'DELIVERY', 84000, 4000, 88000, 26000, 8400,
                                                                                                    'الطابق الخامس، الجرس لا يعمل', '+963911000004', 'دمشق، شعلان، شارع الجلاء، بناء 9، طابق 5، شقة 10', 33.5160, 36.2900, 25, 'seed-req-4',  null,
                                                                                                    null, now() - interval '50 minutes', now() - interval '20 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000005', 'SFA-1005', 'c0000000-0000-4000-8000-000000000001', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', 'PREPARING',        'DELIVERY', 57000, 5000, 62000, 13000, 5700,
                                                                                                    null,                         '+963911000001', 'دمشق، المزة، شارع المزة، بناء 12، طابق 3، شقة 5', 33.5120, 36.2560, 25, 'seed-req-5',  null,
                                                                                                    null, now() - interval '35 minutes', now() - interval '25 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000006', 'SFA-1006', 'c0000000-0000-4000-8000-000000000002', '0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', 'CONFIRMED',        'PICKUP',   30000, 0,    30000, 8000,  3000,
                                                                                                    null,                         '+963911000002', null, null, null, 12, 'seed-req-6',  null,
                                                                                                    null, now() - interval '15 minutes', now() - interval '10 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000007', 'SFA-1007', 'c0000000-0000-4000-8000-000000000003', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', 'PENDING',          'DELIVERY', 78000, 6000, 84000, 22000, 7800,
                                                                                                    null,                         '+963911000003', 'دمشق، باب توما، الشارع المستقيم، بناء 5، طابق 4، شقة 8', 33.5125, 36.2995, 40, 'seed-req-7',  null,
                                                                                                    null, now() - interval '5 minutes',  now() - interval '5 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000008', 'SFA-1008', 'c0000000-0000-4000-8000-000000000004', '8e182880-7522-5700-a577-48a458ef0654', 'CANCELLED',        'DELIVERY', 38000, 0,    38000, 10000, 3800,
                                                                                                    null,                         '+963911000004', 'دمشق، شعلان، شارع الجلاء، بناء 9، طابق 5، شقة 10', 33.5160, 36.2900, 25, 'seed-req-8',  'غيرت رأيي',
                                                                                                    null, now() - interval '1 day', now() - interval '1 day' + interval '5 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000009', 'SFA-1009', 'c0000000-0000-4000-8000-000000000001', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', 'READY_FOR_PICKUP',  'PICKUP',   35000, 0,    35000, 10000, 3500,
                                                                                                    null,                         '+963911000001', null, null, null, 15, 'seed-req-9',  null,
                                                                                                    null, now() - interval '25 minutes', now() - interval '5 minutes'),
                                                                                                   ('d0000000-0000-4000-8000-000000000010', 'SFA-1010', 'c0000000-0000-4000-8000-000000000002', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', 'DELIVERED',        'DELIVERY', 85000, 7000, 92000, 25000, 8500,
                                                                                                    null,                         '+963911000002', 'دمشق، الصالحية، شارع الصالحية، بناء 21، طابق 1، شقة 2', 33.5205, 36.2960, 40, 'seed-req-10', null,
                                                                                                    now() - interval '2 days' + interval '55 minutes', now() - interval '2 days',  now() - interval '2 days' + interval '55 minutes');

-- Lines: title / prices are snapshotted from the offer; line_total = (price + addons_total) * quantity
insert into order_items (id, order_id, offer_id, title, image_url, unit_price, original_unit_price, addons_total, quantity, line_total, note)
select v.id::uuid, v.order_id::uuid, o.id, o.title, o.image_url, o.price, o.original_price, v.addons_total, v.quantity,
       (o.price + v.addons_total) * v.quantity, v.note
from (values
          ('e0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', '31e225ca-f8fa-55ef-9b15-725cc35aaa46', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', '356e3453-70b0-5ae4-9c62-5cfe907a374e', 1, 3000::bigint, 'بدون بصل'),
          ('e0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000002', 'edf840c7-e587-5bfc-b0fe-7bff69e9f324', 2, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000003', 'ce764de1-0d08-5fc0-818c-e66e71b81ca1', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000003', '39ad3564-c4ee-54bc-9135-b0c6577deb0a', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000006', 'd0000000-0000-4000-8000-000000000004', 'fb033d75-28f5-5a96-bc78-18ae123590d1', 2, 0::bigint,    'ثوم زيادة'),
          ('e0000000-0000-4000-8000-000000000007', 'd0000000-0000-4000-8000-000000000005', 'fb56ea2c-873e-5d0c-88d2-9225baf366b0', 1, 5000::bigint, null),
          ('e0000000-0000-4000-8000-000000000008', 'd0000000-0000-4000-8000-000000000006', '7ef8d4c0-d006-5ef5-96d9-d5a2a8fd9098', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000009', 'd0000000-0000-4000-8000-000000000007', '8cb78e3f-f50c-5fdd-ad98-54132dc38403', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000010', 'd0000000-0000-4000-8000-000000000008', '583ae803-fafb-5410-9358-ddae952ec29b', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000011', 'd0000000-0000-4000-8000-000000000009', '225e3bd1-15e5-5076-82c1-6afff21a3455', 1, 0::bigint,    null),
          ('e0000000-0000-4000-8000-000000000012', 'd0000000-0000-4000-8000-000000000010', '59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 1, 0::bigint,    null)
     ) as v(id, order_id, offer_id, quantity, addons_total, note)
         join offers o on o.id = v.offer_id::uuid;

-- The addons picked on lines above (their prices add up to order_items.addons_total)
insert into order_item_addons (order_item_id, name, extra_price) values
                                                                     ('e0000000-0000-4000-8000-000000000002', 'جبنة شيدر',   3000),   -- nuggets box, 3000
                                                                     ('e0000000-0000-4000-8000-000000000007', 'جبنة إضافية', 0),      -- pizza: 2 extras, one paid (5000)
                                                                     ('e0000000-0000-4000-8000-000000000007', 'سلطة صغيرة',  5000),
                                                                     ('e0000000-0000-4000-8000-000000000008', 'صوص حار',     0);      -- snack box, free addon

-- ---------- Status timeline ----------
-- minutes = offset from the order's created_at. changed_by: customer / restaurant owner / admin.
insert into order_status_history (order_id, status, note, changed_by, created_at)
select v.order_id::uuid, v.status, v.note, v.changed_by::uuid, o.created_at + v.mins * interval '1 minute'
from (values
    -- 1: Ahmad @ Burger House (owner 14eed5a7...)
    ('d0000000-0000-4000-8000-000000000001', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000001',  0),
    ('d0000000-0000-4000-8000-000000000001', 'CONFIRMED',        null,        '14eed5a7-2397-56d7-817e-26e679378205',  3),
    ('d0000000-0000-4000-8000-000000000001', 'PREPARING',        null,        '14eed5a7-2397-56d7-817e-26e679378205',  8),
    ('d0000000-0000-4000-8000-000000000001', 'OUT_FOR_DELIVERY', null,        '14eed5a7-2397-56d7-817e-26e679378205', 25),
    ('d0000000-0000-4000-8000-000000000001', 'DELIVERED',        null,        '14eed5a7-2397-56d7-817e-26e679378205', 50),
    -- 2: Layla @ Sultan Shawarma (owner2)
    ('d0000000-0000-4000-8000-000000000002', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000002',  0),
    ('d0000000-0000-4000-8000-000000000002', 'CONFIRMED',        null,        'b0000000-0000-4000-8000-000000000001',  5),
    ('d0000000-0000-4000-8000-000000000002', 'PREPARING',        null,        'b0000000-0000-4000-8000-000000000001', 10),
    ('d0000000-0000-4000-8000-000000000002', 'OUT_FOR_DELIVERY', null,        'b0000000-0000-4000-8000-000000000001', 22),
    ('d0000000-0000-4000-8000-000000000002', 'DELIVERED',        null,        'b0000000-0000-4000-8000-000000000001', 40),
    -- 3: Omar pickup @ Sweets (no owner -> admin)
    ('d0000000-0000-4000-8000-000000000003', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000003',  0),
    ('d0000000-0000-4000-8000-000000000003', 'CONFIRMED',        null,        '98f76c6c-a511-5375-a120-975a82cbc851',  4),
    ('d0000000-0000-4000-8000-000000000003', 'PREPARING',        null,        '98f76c6c-a511-5375-a120-975a82cbc851',  8),
    ('d0000000-0000-4000-8000-000000000003', 'READY_FOR_PICKUP', null,        '98f76c6c-a511-5375-a120-975a82cbc851', 20),
    ('d0000000-0000-4000-8000-000000000003', 'DELIVERED',        null,        '98f76c6c-a511-5375-a120-975a82cbc851', 30),
    -- 4: Sara @ Broasted (owner2), on the way
    ('d0000000-0000-4000-8000-000000000004', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000004',  0),
    ('d0000000-0000-4000-8000-000000000004', 'CONFIRMED',        null,        'b0000000-0000-4000-8000-000000000001',  3),
    ('d0000000-0000-4000-8000-000000000004', 'PREPARING',        null,        'b0000000-0000-4000-8000-000000000001', 10),
    ('d0000000-0000-4000-8000-000000000004', 'OUT_FOR_DELIVERY', null,        'b0000000-0000-4000-8000-000000000001', 30),
    -- 5: Ahmad @ Pizza Time (owner 14eed5a7...), preparing
    ('d0000000-0000-4000-8000-000000000005', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000001',  0),
    ('d0000000-0000-4000-8000-000000000005', 'CONFIRMED',        null,        '14eed5a7-2397-56d7-817e-26e679378205',  3),
    ('d0000000-0000-4000-8000-000000000005', 'PREPARING',        null,        '14eed5a7-2397-56d7-817e-26e679378205', 10),
    -- 6: Layla pickup @ Snack Corner, confirmed
    ('d0000000-0000-4000-8000-000000000006', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000002',  0),
    ('d0000000-0000-4000-8000-000000000006', 'CONFIRMED',        null,        '98f76c6c-a511-5375-a120-975a82cbc851',  5),
    -- 7: Omar @ Burger House, just placed
    ('d0000000-0000-4000-8000-000000000007', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000003',  0),
    -- 8: Sara cancelled
    ('d0000000-0000-4000-8000-000000000008', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000004',  0),
    ('d0000000-0000-4000-8000-000000000008', 'CANCELLED',        'غيرت رأيي', 'c0000000-0000-4000-8000-000000000004',  5),
    -- 9: Ahmad pickup @ Sweets, ready
    ('d0000000-0000-4000-8000-000000000009', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000001',  0),
    ('d0000000-0000-4000-8000-000000000009', 'CONFIRMED',        null,        '98f76c6c-a511-5375-a120-975a82cbc851',  3),
    ('d0000000-0000-4000-8000-000000000009', 'PREPARING',        null,        '98f76c6c-a511-5375-a120-975a82cbc851',  8),
    ('d0000000-0000-4000-8000-000000000009', 'READY_FOR_PICKUP', null,        '98f76c6c-a511-5375-a120-975a82cbc851', 20),
    -- 10: Layla @ Pizza Time, delivered
    ('d0000000-0000-4000-8000-000000000010', 'PENDING',          null,        'c0000000-0000-4000-8000-000000000002',  0),
    ('d0000000-0000-4000-8000-000000000010', 'CONFIRMED',        null,        '14eed5a7-2397-56d7-817e-26e679378205',  4),
    ('d0000000-0000-4000-8000-000000000010', 'PREPARING',        null,        '14eed5a7-2397-56d7-817e-26e679378205', 12),
    ('d0000000-0000-4000-8000-000000000010', 'OUT_FOR_DELIVERY', null,        '14eed5a7-2397-56d7-817e-26e679378205', 32),
    ('d0000000-0000-4000-8000-000000000010', 'DELIVERED',        null,        '14eed5a7-2397-56d7-817e-26e679378205', 55)
    ) as v(order_id, status, note, changed_by, mins)
    join orders o on o.id = v.order_id::uuid;

-- ---------- Payments (one per order; amount = order total) ----------
insert into payments (order_id, method, status, amount, reference, paid_at, created_at, updated_at) values
                                                                                                        ('d0000000-0000-4000-8000-000000000001', 'CASH_ON_DELIVERY', 'PAID',                  74000, null,        now() - interval '5 days' + interval '50 minutes', now() - interval '5 days',          now() - interval '5 days' + interval '50 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000002', 'SHAM_CASH',        'PAID',                  48000, 'SC-884213', now() - interval '4 days' + interval '10 minutes', now() - interval '4 days',          now() - interval '4 days' + interval '10 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000003', 'CASH_ON_DELIVERY', 'PAID',                  46000, null,        now() - interval '3 days' + interval '30 minutes', now() - interval '3 days',          now() - interval '3 days' + interval '30 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000004', 'CASH_ON_DELIVERY', 'PENDING',               88000, null,        null,                                              now() - interval '50 minutes',      now() - interval '50 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000005', 'SHAM_CASH',        'PAID',                  62000, 'SC-771902', now() - interval '30 minutes',                     now() - interval '35 minutes',      now() - interval '30 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000006', 'SHAM_CASH',        'AWAITING_VERIFICATION', 30000, 'SC-559031', null,                                              now() - interval '15 minutes',      now() - interval '15 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000007', 'CASH_ON_DELIVERY', 'PENDING',               84000, null,        null,                                              now() - interval '5 minutes',       now() - interval '5 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000008', 'CASH_ON_DELIVERY', 'CANCELLED',             38000, null,        null,                                              now() - interval '1 day',           now() - interval '1 day' + interval '5 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000009', 'CASH_ON_DELIVERY', 'PENDING',               35000, null,        null,                                              now() - interval '25 minutes',      now() - interval '25 minutes'),
                                                                                                        ('d0000000-0000-4000-8000-000000000010', 'CASH_ON_DELIVERY', 'PAID',                  92000, null,        now() - interval '2 days' + interval '55 minutes', now() - interval '2 days',          now() - interval '2 days' + interval '55 minutes');

-- next real order gets SFA-1011 (the app uses order_number_seq)
select setval('order_number_seq', greatest(1010, (select last_value from order_number_seq)));

-- ---------- Reviews (only for DELIVERED orders, like the app enforces) ----------
-- offer_id null = review of the restaurant; offer_id set = review of one offer in that order.
insert into reviews (user_id, order_id, restaurant_id, offer_id, rating, comment, hidden, created_at, updated_at) values
                                                                                                                      ('c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                                   5, 'ممتاز، الطلب وصل ساخن وبسرعة',       false, now() - interval '5 days' + interval '2 hours', now() - interval '5 days' + interval '2 hours'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', '31e225ca-f8fa-55ef-9b15-725cc35aaa46', 5, 'أفضل برغر جربته',                      false, now() - interval '5 days' + interval '2 hours', now() - interval '5 days' + interval '2 hours'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', '356e3453-70b0-5ae4-9c62-5cfe907a374e', 4, 'جيد لكن الكمية قليلة',                false, now() - interval '5 days' + interval '2 hours', now() - interval '5 days' + interval '2 hours'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002', '8e182880-7522-5700-a577-48a458ef0654', null,                                   4, 'طعم رائع',                              false, now() - interval '4 days' + interval '1 hour',  now() - interval '4 days' + interval '1 hour'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000002', '8e182880-7522-5700-a577-48a458ef0654', 'edf840c7-e587-5bfc-b0fe-7bff69e9f324', 5, 'شاورما ممتازة وسعر مناسب',            false, now() - interval '4 days' + interval '1 hour',  now() - interval '4 days' + interval '1 hour'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', null,                                   5, 'حلويات طازجة وتعامل راقي',            false, now() - interval '3 days' + interval '1 hour',  now() - interval '3 days' + interval '1 hour'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', 'ce764de1-0d08-5fc0-818c-e66e71b81ca1', 5, 'تشيز كيك رائع',                        false, now() - interval '3 days' + interval '1 hour',  now() - interval '3 days' + interval '1 hour'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000003', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', '39ad3564-c4ee-54bc-9135-b0c6577deb0a', 4, 'اللاتيه جيد',                          false, now() - interval '3 days' + interval '1 hour',  now() - interval '3 days' + interval '1 hour'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000010', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', null,                                   3, 'الطلب تأخر عن الوقت المتوقع',        false, now() - interval '2 days' + interval '2 hours', now() - interval '2 days' + interval '2 hours'),
                                                                                                                      ('c0000000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000010', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', '59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 4, 'البيتزا لذيذة',                         false, now() - interval '2 days' + interval '2 hours', now() - interval '2 days' + interval '2 hours'),
                                                                                                                      -- a hidden (moderated) review with no order, to test the admin reviews screen
                                                                                                                      ('c0000000-0000-4000-8000-000000000004', null,                                   'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                                   1, 'تعليق مخالف (مخفي بواسطة الإدارة)',  true,  now() - interval '6 days', now() - interval '6 days');

-- ---------- Favorites (each row = one offer OR one restaurant) ----------
insert into favorites (user_id, offer_id, restaurant_id, created_at) values
                                                                         ('c0000000-0000-4000-8000-000000000001', '31e225ca-f8fa-55ef-9b15-725cc35aaa46', null,                                   now() - interval '6 days'),
                                                                         ('c0000000-0000-4000-8000-000000000001', 'fb56ea2c-873e-5d0c-88d2-9225baf366b0', null,                                   now() - interval '3 days'),
                                                                         ('c0000000-0000-4000-8000-000000000001', null,                                   'c15f4d87-a59e-5b0a-9580-a4a844790b09', now() - interval '6 days'),
                                                                         ('c0000000-0000-4000-8000-000000000002', 'edf840c7-e587-5bfc-b0fe-7bff69e9f324', null,                                   now() - interval '4 days'),
                                                                         ('c0000000-0000-4000-8000-000000000002', null,                                   '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', now() - interval '2 days'),
                                                                         ('c0000000-0000-4000-8000-000000000003', 'ce764de1-0d08-5fc0-818c-e66e71b81ca1', null,                                   now() - interval '3 days'),
                                                                         ('c0000000-0000-4000-8000-000000000003', null,                                   '8e182880-7522-5700-a577-48a458ef0654', now() - interval '1 day'),
                                                                         ('c0000000-0000-4000-8000-000000000004', 'fb033d75-28f5-5a96-bc78-18ae123590d1', null,                                   now() - interval '1 day');

-- ---------- Price alerts ----------
insert into price_alerts (user_id, offer_id, target_price, active, triggered_at, created_at, updated_at) values
                                                                                                             ('c0000000-0000-4000-8000-000000000001', '31e225ca-f8fa-55ef-9b15-725cc35aaa46', 40000, true,  null,                         now() - interval '5 days',  now() - interval '5 days'),
                                                                                                             ('c0000000-0000-4000-8000-000000000001', '59a740e2-289a-54cc-ba1b-15e52b9d7c2d', 80000, true,  null,                         now() - interval '2 days',  now() - interval '2 days'),
                                                                                                             ('c0000000-0000-4000-8000-000000000002', '36f4bc74-94a2-5d49-a21e-5f912c6277ad', 85000, true,  null,                         now() - interval '3 days',  now() - interval '3 days'),
                                                                                                             ('c0000000-0000-4000-8000-000000000003', '225e3bd1-15e5-5076-82c1-6afff21a3455', 36000, false, now() - interval '1 day',      now() - interval '10 days', now() - interval '1 day'),
                                                                                                             ('c0000000-0000-4000-8000-000000000004', 'fb56ea2c-873e-5d0c-88d2-9225baf366b0', 45000, true,  null,                         now() - interval '1 day',   now() - interval '1 day');

-- ---------- Notifications (mix of read / unread, all six types) ----------
insert into notifications (user_id, type, title, body, offer_id, order_id, restaurant_id, read_at, created_at) values
                                                                                                                   ('c0000000-0000-4000-8000-000000000001', 'ORDER_STATUS',   'تم توصيل طلبك SFA-1001',               'بالهناء والشفاء! قيّم تجربتك.',                         null,                                   'd0000000-0000-4000-8000-000000000001', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', now() - interval '5 days' + interval '1 hour', now() - interval '5 days' + interval '50 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000001', 'PRICE_DROP',     'انخفض سعر "وجبة برغر دبل"',             'وصل السعر إلى 45000 ل.س',                                '31e225ca-f8fa-55ef-9b15-725cc35aaa46', null,                                   'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                               now() - interval '2 hours'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000001', 'ORDER_STATUS',   'طلبك SFA-1005 قيد التحضير',            'المطعم بدأ بتحضير طلبك.',                               null,                                   'd0000000-0000-4000-8000-000000000005', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', null,                               now() - interval '25 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000001', 'ORDER_STATUS',   'طلبك SFA-1009 جاهز للاستلام',          'تفضل لاستلام طلبك من المطعم.',                          null,                                   'd0000000-0000-4000-8000-000000000009', '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', null,                               now() - interval '5 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000001', 'OFFER_EXPIRING', 'عرض "برغر ثنائي" ينتهي قريباً',         'متبقي أقل من 36 ساعة.',                                 '8cb78e3f-f50c-5fdd-ad98-54132dc38403', null,                                   'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                               now() - interval '1 hour'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000002', 'ORDER_STATUS',   'تم توصيل طلبك SFA-1002',               'بالهناء والشفاء!',                                      null,                                   'd0000000-0000-4000-8000-000000000002', '8e182880-7522-5700-a577-48a458ef0654', now() - interval '3 days',            now() - interval '4 days' + interval '40 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000002', 'ORDER_STATUS',   'تم تأكيد طلبك SFA-1006',               'يمكنك استلامه بعد قليل.',                               null,                                   'd0000000-0000-4000-8000-000000000006', '0036d5cc-c0d3-56a0-bccb-dacfc67ed4ba', null,                               now() - interval '10 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000002', 'NEW_OFFER',      'عرض جديد من حلويات الشام',             'بوكس حلويات شرقية بسعر مميز.',                          '225e3bd1-15e5-5076-82c1-6afff21a3455', null,                                   '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', null,                               now() - interval '1 day'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000003', 'SYSTEM',         'أهلاً بك في "شو فيني آكل؟"',            'أدخل ميزانيتك وسنعرض لك أفضل العروض.',                  null,                                   null,                                   null,                                   now() - interval '9 days',            now() - interval '10 days'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000003', 'PRICE_DROP',     'تحقق تنبيه السعر',                      'بوكس حلويات شرقية وصل إلى سعرك المطلوب.',               '225e3bd1-15e5-5076-82c1-6afff21a3455', null,                                   '92f2cfc0-1287-5f9f-b550-d7c1936c0f46', now() - interval '20 hours',          now() - interval '1 day'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000003', 'ORDER_STATUS',   'تم استلام طلبك SFA-1007',              'بانتظار تأكيد المطعم.',                                 null,                                   'd0000000-0000-4000-8000-000000000007', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                               now() - interval '5 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000004', 'ORDER_STATUS',   'طلبك SFA-1004 خرج للتوصيل',            'السائق في الطريق إليك.',                                null,                                   'd0000000-0000-4000-8000-000000000004', 'fe8ef563-9004-5ed6-85bf-0701051ce5cd', null,                               now() - interval '20 minutes'),
                                                                                                                   ('c0000000-0000-4000-8000-000000000004', 'ORDER_STATUS',   'تم إلغاء طلبك SFA-1008',               'سبب الإلغاء: غيرت رأيي',                                null,                                   'd0000000-0000-4000-8000-000000000008', '8e182880-7522-5700-a577-48a458ef0654', now() - interval '23 hours',          now() - interval '1 day' + interval '5 minutes'),
                                                                                                                   -- restaurant owners / admin
                                                                                                                   ('14eed5a7-2397-56d7-817e-26e679378205', 'NEW_ORDER',      'طلب جديد SFA-1007',                    'إجمالي الطلب 84000 ل.س',                                null,                                   'd0000000-0000-4000-8000-000000000007', 'c15f4d87-a59e-5b0a-9580-a4a844790b09', null,                               now() - interval '5 minutes'),
                                                                                                                   ('14eed5a7-2397-56d7-817e-26e679378205', 'NEW_ORDER',      'طلب جديد SFA-1005',                    'إجمالي الطلب 62000 ل.س',                                null,                                   'd0000000-0000-4000-8000-000000000005', '2b56dd0b-78f8-58fc-9f5d-4bda9c2b3121', now() - interval '30 minutes',        now() - interval '35 minutes'),
                                                                                                                   ('b0000000-0000-4000-8000-000000000001', 'NEW_ORDER',      'طلب جديد SFA-1004',                    'إجمالي الطلب 88000 ل.س',                                null,                                   'd0000000-0000-4000-8000-000000000004', 'fe8ef563-9004-5ed6-85bf-0701051ce5cd', now() - interval '45 minutes',        now() - interval '50 minutes'),
                                                                                                                   ('98f76c6c-a511-5375-a120-975a82cbc851', 'SYSTEM',         'تم تحميل بيانات التجربة',               'تمت إضافة مطاعم وعروض وطلبات تجريبية.',                 null,                                   null,                                   null,                                   null,                               now() - interval '1 minute');