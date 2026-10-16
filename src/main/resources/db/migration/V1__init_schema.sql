-- =====================================================================
-- شو فيني آكل بهيك سعر؟  -  initial schema
-- All money values are whole Syrian pounds (BIGINT). Timestamps are UTC.
-- =====================================================================

-- ---------- Users / auth ----------
create table users (
    id            uuid primary key default gen_random_uuid(),
    phone         varchar(20)  not null unique,
    full_name     varchar(120),
    email         varchar(160),
    language      varchar(5)   not null default 'ar',
    role          varchar(30)  not null default 'CUSTOMER',
    status        varchar(20)  not null default 'ACTIVE',
    last_login_at timestamptz,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);

create table otp_codes (
    id          uuid primary key default gen_random_uuid(),
    phone       varchar(20)  not null,
    code_hash   varchar(128) not null,
    attempts    integer      not null default 0,
    expires_at  timestamptz  not null,
    consumed_at timestamptz,
    created_at  timestamptz  not null default now()
);
create index idx_otp_phone_created on otp_codes (phone, created_at desc);

create table refresh_tokens (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid         not null references users (id) on delete cascade,
    token_hash varchar(64)  not null unique,
    expires_at timestamptz  not null,
    revoked_at timestamptz,
    created_at timestamptz  not null default now()
);
create index idx_refresh_user on refresh_tokens (user_id);

create table addresses (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid         not null references users (id) on delete cascade,
    type       varchar(20)  not null default 'HOME',
    label      varchar(60),
    city       varchar(80)  not null,
    area       varchar(80)  not null,
    street     varchar(160),
    building   varchar(40),
    floor      varchar(20),
    apartment  varchar(20),
    landmark   varchar(200),
    latitude   double precision,
    longitude  double precision,
    is_primary boolean      not null default false,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);
create index idx_addresses_user on addresses (user_id);

-- ---------- Catalog ----------
create table categories (
    id         uuid primary key default gen_random_uuid(),
    slug       varchar(40)  not null unique,
    name_ar    varchar(80)  not null,
    name_en    varchar(80)  not null,
    name_fr    varchar(80)  not null,
    emoji      varchar(16),
    icon_url   varchar(300),
    sort_order integer      not null default 0,
    active     boolean      not null default true,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create table restaurants (
    id                   uuid primary key default gen_random_uuid(),
    owner_id             uuid references users (id) on delete set null,
    name                 varchar(120) not null,
    description          varchar(1000),
    logo_url             varchar(500),
    cover_url            varchar(500),
    phone                varchar(30),
    city                 varchar(80)  not null default 'دمشق',
    area                 varchar(80),
    address_line         varchar(250),
    latitude             double precision not null,
    longitude            double precision not null,
    opens_at             time,
    closes_at            time,
    accepting_orders     boolean      not null default true,
    active               boolean      not null default true,
    default_delivery_fee bigint       not null default 0,
    min_order_amount     bigint       not null default 0,
    avg_prep_minutes     integer      not null default 25,
    commission_percent   double precision not null default 10,
    rating_avg           double precision not null default 0,
    rating_count         integer      not null default 0,
    created_at           timestamptz  not null default now(),
    updated_at           timestamptz  not null default now()
);
create index idx_restaurants_owner on restaurants (owner_id);
create index idx_restaurants_active on restaurants (active);

create table restaurant_categories (
    restaurant_id uuid not null references restaurants (id) on delete cascade,
    category_id   uuid not null references categories (id) on delete cascade,
    primary key (restaurant_id, category_id)
);

create table delivery_zones (
    id               uuid primary key default gen_random_uuid(),
    restaurant_id    uuid         not null references restaurants (id) on delete cascade,
    name             varchar(100) not null,
    max_distance_km  double precision not null,
    fee              bigint       not null default 0,
    min_order_amount bigint       not null default 0,
    estimated_minutes integer     not null default 30,
    active           boolean      not null default true,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now()
);
create index idx_zones_restaurant on delivery_zones (restaurant_id);

create table menu_items (
    id            uuid primary key default gen_random_uuid(),
    restaurant_id uuid         not null references restaurants (id) on delete cascade,
    category_id   uuid references categories (id) on delete set null,
    name          varchar(150) not null,
    description   varchar(600),
    price         bigint       not null check (price >= 0),
    image_url     varchar(500),
    available     boolean      not null default true,
    sort_order    integer      not null default 0,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now()
);
create index idx_menu_restaurant on menu_items (restaurant_id);

create table offers (
    id              uuid primary key default gen_random_uuid(),
    restaurant_id   uuid         not null references restaurants (id) on delete cascade,
    category_id     uuid         not null references categories (id),
    title           varchar(150) not null,
    description     varchar(1000),
    image_url       varchar(500),
    price           bigint       not null check (price > 0),
    original_price  bigint       not null,
    items_count     integer      not null default 1,
    servings        integer      not null default 1,
    max_addons      integer      not null default 0,
    stock_remaining integer,
    starts_at       timestamptz  not null default now(),
    ends_at         timestamptz,
    active          boolean      not null default true,
    sponsored       boolean      not null default false,
    rating_avg      double precision not null default 0,
    rating_count    integer      not null default 0,
    view_count      bigint       not null default 0,
    order_count     bigint       not null default 0,
    expiry_notified boolean      not null default false,
    created_at      timestamptz  not null default now(),
    updated_at      timestamptz  not null default now(),
    constraint chk_offer_prices check (original_price >= price)
);
create index idx_offers_restaurant on offers (restaurant_id);
create index idx_offers_category on offers (category_id);
create index idx_offers_active_price on offers (active, price);
create index idx_offers_ends_at on offers (ends_at);

create table offer_images (
    offer_id uuid         not null references offers (id) on delete cascade,
    sort_index integer    not null,
    url        varchar(500) not null,
    primary key (offer_id, sort_index)
);

create table offer_items (
    id           uuid primary key default gen_random_uuid(),
    offer_id     uuid         not null references offers (id) on delete cascade,
    menu_item_id uuid references menu_items (id) on delete set null,
    name         varchar(150) not null,
    quantity     integer      not null default 1,
    sort_order   integer      not null default 0
);
create index idx_offer_items_offer on offer_items (offer_id);

create table offer_addons (
    id          uuid primary key default gen_random_uuid(),
    offer_id    uuid         not null references offers (id) on delete cascade,
    name        varchar(120) not null,
    extra_price bigint       not null default 0,
    available   boolean      not null default true,
    sort_order  integer      not null default 0
);
create index idx_offer_addons_offer on offer_addons (offer_id);

-- day_of_week follows ISO-8601: 1 = Monday ... 7 = Sunday.
-- An offer without rows here is available at any time inside its starts_at/ends_at window.
create table offer_availability (
    id          uuid primary key default gen_random_uuid(),
    offer_id    uuid    not null references offers (id) on delete cascade,
    day_of_week integer not null check (day_of_week between 1 and 7),
    start_time  time    not null,
    end_time    time    not null
);
create index idx_offer_availability_offer on offer_availability (offer_id);

create table price_history (
    id             uuid primary key default gen_random_uuid(),
    offer_id       uuid        not null references offers (id) on delete cascade,
    price          bigint      not null,
    original_price bigint      not null,
    created_at     timestamptz not null default now()
);
create index idx_price_history_offer on price_history (offer_id, created_at);

-- ---------- User collections ----------
create table favorites (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid        not null references users (id) on delete cascade,
    offer_id      uuid references offers (id) on delete cascade,
    restaurant_id uuid references restaurants (id) on delete cascade,
    created_at    timestamptz not null default now(),
    constraint chk_favorite_target check ((offer_id is not null) <> (restaurant_id is not null)),
    constraint uq_favorite_offer unique (user_id, offer_id),
    constraint uq_favorite_restaurant unique (user_id, restaurant_id)
);

create table price_alerts (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid        not null references users (id) on delete cascade,
    offer_id     uuid        not null references offers (id) on delete cascade,
    target_price bigint      not null check (target_price > 0),
    active       boolean     not null default true,
    triggered_at timestamptz,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    constraint uq_alert_user_offer unique (user_id, offer_id)
);
create index idx_alerts_offer on price_alerts (offer_id);

-- ---------- Orders ----------
create sequence order_number_seq start with 1000 increment by 1;

create table orders (
    id                    uuid primary key default gen_random_uuid(),
    order_number          varchar(20)  not null unique,
    user_id               uuid         not null references users (id),
    restaurant_id         uuid         not null references restaurants (id),
    status                varchar(30)  not null,
    delivery_type         varchar(20)  not null,
    subtotal              bigint       not null,
    delivery_fee          bigint       not null default 0,
    total                 bigint       not null,
    saving_total          bigint       not null default 0,
    commission_amount     bigint       not null default 0,
    driver_notes          varchar(500),
    contact_phone         varchar(20)  not null,
    delivery_address_text varchar(500),
    delivery_latitude     double precision,
    delivery_longitude    double precision,
    estimated_minutes     integer,
    client_request_id     varchar(64),
    cancelled_reason      varchar(300),
    delivered_at          timestamptz,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    constraint uq_order_client_request unique (user_id, client_request_id)
);
create index idx_orders_user_created on orders (user_id, created_at desc);
create index idx_orders_restaurant_created on orders (restaurant_id, created_at desc);
create index idx_orders_status on orders (status);

create table order_items (
    id                  uuid primary key default gen_random_uuid(),
    order_id            uuid         not null references orders (id) on delete cascade,
    offer_id            uuid references offers (id) on delete set null,
    title               varchar(150) not null,
    image_url           varchar(500),
    unit_price          bigint       not null,
    original_unit_price bigint       not null,
    addons_total        bigint       not null default 0,
    quantity            integer      not null,
    line_total          bigint       not null,
    note                varchar(300)
);
create index idx_order_items_order on order_items (order_id);
create index idx_order_items_offer on order_items (offer_id);

create table order_item_addons (
    id            uuid primary key default gen_random_uuid(),
    order_item_id uuid         not null references order_items (id) on delete cascade,
    name          varchar(120) not null,
    extra_price   bigint       not null default 0
);
create index idx_order_item_addons_item on order_item_addons (order_item_id);

create table order_status_history (
    id         uuid primary key default gen_random_uuid(),
    order_id   uuid        not null references orders (id) on delete cascade,
    status     varchar(30) not null,
    note       varchar(300),
    changed_by uuid,
    created_at timestamptz not null default now()
);
create index idx_order_history_order on order_status_history (order_id, created_at);

create table payments (
    id         uuid primary key default gen_random_uuid(),
    order_id   uuid         not null unique references orders (id) on delete cascade,
    method     varchar(30)  not null,
    status     varchar(30)  not null,
    amount     bigint       not null,
    reference  varchar(100),
    paid_at    timestamptz,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

-- ---------- Reviews / notifications ----------
create table reviews (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid        not null references users (id) on delete cascade,
    order_id      uuid references orders (id) on delete set null,
    restaurant_id uuid        not null references restaurants (id) on delete cascade,
    offer_id      uuid references offers (id) on delete cascade,
    rating        integer     not null check (rating between 1 and 5),
    comment       varchar(1000),
    hidden        boolean     not null default false,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);
create unique index uq_review_order_restaurant on reviews (order_id) where offer_id is null and order_id is not null;
create unique index uq_review_order_offer on reviews (order_id, offer_id) where offer_id is not null and order_id is not null;
create index idx_reviews_restaurant on reviews (restaurant_id, created_at desc);
create index idx_reviews_offer on reviews (offer_id, created_at desc);

create table notifications (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid         not null references users (id) on delete cascade,
    type          varchar(30)  not null,
    title         varchar(200) not null,
    body          varchar(500),
    offer_id      uuid,
    order_id      uuid,
    restaurant_id uuid,
    read_at       timestamptz,
    created_at    timestamptz  not null default now()
);
create index idx_notifications_user_created on notifications (user_id, created_at desc);
