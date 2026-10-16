-- Food categories used by the app's category chips (icons match the app's twemoji set).
insert into categories (slug, name_ar, name_en, name_fr, emoji, icon_url, sort_order) values
 ('burger',   'برغر',     'Burger',    'Burger',       '🍔', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f354.png', 1),
 ('shawarma', 'شاورما',   'Shawarma',  'Chawarma',     '🥙', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f959.png', 2),
 ('pizza',    'بيتزا',    'Pizza',     'Pizza',        '🍕', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f355.png', 3),
 ('broasted', 'بروستد',   'Broasted',  'Poulet frit',  '🍗', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f357.png', 4),
 ('snacks',   'سناكات',   'Snacks',    'Snacks',       '🍿', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f37f.png', 5),
 ('desserts', 'حلويات',   'Desserts',  'Desserts',     '🍰', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f370.png', 6),
 ('coffee',   'قهوة',     'Coffee',    'Café',         '☕', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/2615.png',  7),
 ('drinks',   'مشروبات',  'Drinks',    'Boissons',     '🥤', 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f964.png', 8);
