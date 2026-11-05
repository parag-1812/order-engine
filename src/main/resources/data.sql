INSERT INTO ingredients (id, name, price, prep_time_in_minutes, vegetarian)
VALUES
(1, 'Paneer', 100, 5, true),
(2, 'Cheese', 100, 5, true);

-- Kitchens across the city (lat/lng used for nearest-kitchen routing)
INSERT INTO kitchens (id, name, vegetarian_only, latitude, longitude)
VALUES
(1, 'Main Veg Kitchen', true, 22.7196, 75.8577),
(2, 'All Food Kitchen', false, 22.7533, 75.8937),
(3, 'Spice Garden (Veg)', true, 22.6708, 75.9064),
(4, 'Grill House', false, 22.7010, 75.8400),
(5, 'Tandoor Junction', false, 22.7760, 75.8900);

INSERT INTO inventory (id, kitchen_id, ingredient_id, available_quantity)
VALUES
(1, 1, 1, 50),
(2, 1, 2, 50),
(3, 2, 1, 100),
(4, 2, 2, 100);
