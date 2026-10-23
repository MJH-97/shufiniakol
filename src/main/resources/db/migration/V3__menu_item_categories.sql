-- A menu item can belong to several categories (dashboard multi-select).
-- menu_items.category_id stays as the item's main category, so existing code keeps working.
create table menu_item_categories (
    menu_item_id uuid not null references menu_items (id) on delete cascade,
    category_id  uuid not null references categories (id) on delete cascade,
    primary key (menu_item_id, category_id)
);

create index idx_menu_item_categories_category on menu_item_categories (category_id);

-- existing items keep the category they already have
insert into menu_item_categories (menu_item_id, category_id)
select id, category_id from menu_items where category_id is not null;
