INSERT INTO ingredients (id, name, price, prep_time_in_minutes, vegetarian)
VALUES
(1, 'Paneer', 100, 5, true),
(2, 'Cheese', 100, 5, true);

INSERT INTO kitchens (id, name, vegetarian_only)
VALUES
(1, 'Main Veg Kitchen', true),
(2, 'All Food Kitchen', false);

INSERT INTO inventory (id, kitchen_id, ingredient_id, available_quantity)
VALUES
(1, 1, 1, 50),
(2, 1, 2, 50),
(3, 2, 1, 100),
(4, 2, 2, 100);
