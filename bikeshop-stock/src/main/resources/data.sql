-- Stock inicial das peças do catálogo. Poucas unidades nas peças caras, para se ver o "stock insuficiente".
INSERT INTO stock (referencia, quantidade, reservada) VALUES
  ('BAT-500', 8, 0), ('BAT-750', 2, 0), ('BAT-630', 5, 0),
  ('MOT-CX5', 1, 0), ('MOT-EP8', 3, 0), ('MOT-H250', 6, 0),
  ('CTR-36', 12, 0), ('CTR-SEN', 25, 0),
  ('TRV-MT5', 10, 0), ('TRV-D203', 20, 0), ('TRV-PAS', 40, 0),
  ('PNE-275', 15, 0), ('PNE-29', 15, 0), ('PNE-700', 18, 0),
  ('CAR-4A', 10, 0), ('CAR-6A', 6, 0), ('CAR-36', 12, 0),
  ('ECR-KIOX', 4, 0), ('ECR-SC', 7, 0), ('ECR-LED', 9, 0)
ON CONFLICT (referencia) DO NOTHING;
