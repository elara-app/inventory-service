-- Development-only seed data for the inventory service.
-- PostgreSQL dialect: runs after Hibernate creates/updates the schema (deferred
-- data-source initialization) and is idempotent, so a service restart never
-- duplicates rows. The base_unit_of_measure_id values 1 and 2 are a deliberate
-- id contract with the unit-of-measure seed (Kilogram, Gram).
INSERT INTO public.inventory_item (id, name, description, base_unit_of_measure_id, standard_cost, unit_per_purchase_uom, reorder_point_quantity)
VALUES (1, 'Steel Bolt M10', 'High-strength steel bolt, metric M10 x 50mm', 1, 2.50, 100.00, 500.00),
       (2, 'Aluminum Sheet 4x8', 'Aluminum sheet 4 feet by 8 feet, 1mm thickness', 2, 45.00, 50.00, 100.00),
       (3, 'Steel Nut M10', 'Hexagonal steel nut, metric M10', 1, 0.15, 500.00, 1000.00),
       (4, 'Copper Wire 2.5mm', 'Insulated copper wire, 2.5 mm cross-section', 1, 8.50, 100.00, 200.00),
       (5, 'PVC Pipe 1in', 'Rigid PVC pipe, 1 inch diameter', 1, 1.20, 50.00, 100.00),
       (6, 'Welding Rod 3.2mm', 'E6013 welding electrode, 3.2 mm', 1, 4.75, 200.00, 400.00),
       (7, 'Hydraulic Oil 20L', 'Mineral hydraulic oil, 20 litre container', 1, 2.80, 20.00, 40.00),
       (8, 'Ball Bearing 6204', 'Deep groove ball bearing, 6204', 1, 12.00, 200.00, 400.00),
       (9, 'Rubber Gasket 50mm', 'NBR rubber gasket, 50 mm', 1, 0.90, 300.00, 600.00),
       (10, 'Aluminum Rivet 4mm', 'Blind aluminum rivet, 4 mm', 1, 6.40, 100.00, 250.00)
ON CONFLICT DO NOTHING;

-- Realign the identity sequence so application inserts continue after the seeded ids.
SELECT setval(pg_get_serial_sequence('public.inventory_item', 'id'),
              COALESCE(max(id), 1), max(id) IS NOT NULL)
FROM public.inventory_item;
