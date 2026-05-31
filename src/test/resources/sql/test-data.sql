INSERT INTO items (id, title, description, img_path, price) VALUES
(1, 'Test item_1 title', 'Test item_1 description', NULL, 999),
(2, 'Test item_2 title', 'Test item_2 description', NULL, 2999),
(3, 'Test item_3 title', 'Test item_3 description', NULL, 7999),
(4, 'Test item_4 title', 'Test item_4 description', NULL, 4999),
(5, 'Test item_5 title', 'Test item_5 description', NULL, 11999),
(6, 'Item_6 title', 'Item_6 description', NULL, 14999);

INSERT INTO orders (id, total_sum) VALUES
(1, 4997),
(2, 23997);

INSERT INTO orders_items (id, order_id, item_id, count, price) VALUES
(1, 1, 1, 2, 999),
(2, 1, 2, 1, 2999),
(3, 2, 3, 3, 7999);

INSERT INTO cart_items(id, item_id, count) VALUES
(1, 1, 2),
(2, 2, 5);

ALTER TABLE items ALTER COLUMN id RESTART WITH 7;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 3;
ALTER TABLE orders_items ALTER COLUMN id RESTART WITH 4;
ALTER TABLE cart_items ALTER COLUMN id RESTART WITH 3;