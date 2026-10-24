-- Dados de exemplo do catálogo. Corre em cada arranque; ON CONFLICT só insere o que ainda não existe.
INSERT INTO categorias (codigo, nome, descricao) VALUES
  ('BAT', 'Baterias', 'Baterias de iões de lítio para quadro e porta-bagagens'),
  ('MOT', 'Motores', 'Motores centrais e de cubo'),
  ('CTR', 'Controladores', 'Controladores e sensores de binário'),
  ('TRV', 'Travões', 'Travões de disco hidráulicos, discos e pastilhas'),
  ('PNE', 'Pneus', 'Pneus reforçados para e-bike'),
  ('CAR', 'Carregadores', 'Carregadores rápidos e normais'),
  ('ECR', 'Ecrãs', 'Ecrãs e comandos de guiador')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO pecas (referencia, nome, marca, descricao, compatibilidade, preco, categoria_id, criado_em, alterado_em)
SELECT v.referencia, v.nome, v.marca, v.descricao, v.compatibilidade, v.preco, c.id, now(), now()
FROM (VALUES
  ('BAT-500', 'PowerTube 500 Wh', 'Bosch', 'Bateria integrada no quadro, 500 Wh', 'Bosch Smart System', 649.00, 'BAT'),
  ('BAT-750', 'PowerTube 750 Wh', 'Bosch', 'Bateria integrada no quadro, 750 Wh', 'Bosch Smart System', 899.00, 'BAT'),
  ('BAT-630', 'EnergyPak 630 Wh', 'Shimano', 'Bateria de tubo inferior, 630 Wh', 'Shimano EP', 759.00, 'BAT'),
  ('MOT-CX5', 'Performance Line CX', 'Bosch', 'Motor central de 85 Nm para montanha', 'Bosch Smart System', 1149.00, 'MOT'),
  ('MOT-EP8', 'EP801', 'Shimano', 'Motor central de 85 Nm, leve', 'Shimano EP', 1049.00, 'MOT'),
  ('MOT-H250', 'Motor de cubo 250 W', 'Bafang', 'Motor traseiro de cubo para cidade', 'Universal 36 V', 289.00, 'MOT'),
  ('CTR-36', 'Controlador 36 V 15 A', 'Bafang', 'Controlador para motores de cubo', 'Universal 36 V', 89.90, 'CTR'),
  ('CTR-SEN', 'Sensor de velocidade', 'Bosch', 'Sensor de roda com íman', 'Bosch Smart System', 39.90, 'CTR'),
  ('TRV-MT5', 'MT5 4 pistões', 'Magura', 'Travão hidráulico de 4 pistões (par)', 'Universal', 219.00, 'TRV'),
  ('TRV-D203', 'Disco 203 mm', 'Shimano', 'Disco de travão RT-MT800 para e-bike', 'Universal', 54.90, 'TRV'),
  ('TRV-PAS', 'Pastilhas metálicas', 'Magura', 'Pastilhas 9.P para uso intenso', 'Magura MT', 24.90, 'TRV'),
  ('PNE-275', 'Magic Mary 27,5 x 2,6', 'Schwalbe', 'Pneu de montanha com reforço e-bike', 'Roda 27,5', 69.90, 'PNE'),
  ('PNE-29', 'Nobby Nic 29 x 2,4', 'Schwalbe', 'Pneu misto para trilho', 'Roda 29', 64.90, 'PNE'),
  ('PNE-700', 'Marathon E-Plus 700 x 40', 'Schwalbe', 'Pneu de cidade antifuro', 'Roda 700c', 49.90, 'PNE'),
  ('CAR-4A', 'Carregador 4 A', 'Bosch', 'Carregamento normal', 'Bosch Smart System', 119.00, 'CAR'),
  ('CAR-6A', 'Carregador rápido 6 A', 'Bosch', 'Carrega 500 Wh em cerca de 2 horas', 'Bosch Smart System', 179.00, 'CAR'),
  ('CAR-36', 'Carregador 42 V 2 A', 'Bafang', 'Carregador para baterias de 36 V', 'Universal 36 V', 39.90, 'CAR'),
  ('ECR-KIOX', 'Kiox 300', 'Bosch', 'Ecrã a cores com navegação', 'Bosch Smart System', 249.00, 'ECR'),
  ('ECR-SC', 'SC-EN600', 'Shimano', 'Ecrã compacto de guiador', 'Shimano EP', 139.00, 'ECR'),
  ('ECR-LED', 'Comando LED Remote', 'Bosch', 'Comando de guiador com cinco níveis', 'Bosch Smart System', 99.00, 'ECR')
) AS v(referencia, nome, marca, descricao, compatibilidade, preco, categoria)
JOIN categorias c ON c.codigo = v.categoria
ON CONFLICT (referencia) DO NOTHING;
