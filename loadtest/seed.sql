SET SESSION cte_max_recursion_depth = 10000;

INSERT INTO member (email, name, created_at, updated_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100)
SELECT CONCAT('load', n, '@example.com'), CONCAT('load-member-', n), NOW(6), NOW(6)
FROM seq;

INSERT INTO product (name, price, status, created_at, updated_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 201)
SELECT CONCAT('load-product-', n), 10000, 'ON_SALE', NOW(6), NOW(6)
FROM seq;

INSERT INTO stock (product_id, on_hand, reserved, created_at, updated_at)
SELECT id, IF(name = 'load-product-201', 500, 1000000), 0, NOW(6), NOW(6)
FROM product;