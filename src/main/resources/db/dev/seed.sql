-- Development-only seed data for the inventory service.
-- PostgreSQL dialect: runs after Hibernate creates/updates the schema (deferred
-- data-source initialization) and is idempotent, so a service restart never
-- duplicates rows. The base_unit_of_measure_id values 1 and 2 are a deliberate
-- id contract with the unit-of-measure seed (Kilogram, Gram).
INSERT INTO public.inventory_item (id, name, description, base_unit_of_measure_id, standard_cost, unit_per_purchase_uom, reorder_point_quantity)
VALUES (1, 'Steel Bolt M10', 'High-strength steel bolt, metric M10 x 50mm', 1, 2.50, 100.00, 500.00),
       (2, 'Aluminum Sheet 4x8', 'Aluminum sheet 4 feet by 8 feet, 1mm thickness', 2, 45.00, 50.00, 100.00)
ON CONFLICT DO NOTHING;

-- Realign the identity sequence so application inserts continue after the seeded ids.
SELECT setval(pg_get_serial_sequence('public.inventory_item', 'id'),
              COALESCE(max(id), 1), max(id) IS NOT NULL)
FROM public.inventory_item;
