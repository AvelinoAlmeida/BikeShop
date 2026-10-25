-- Clientes de exemplo. Corre em cada arranque; ON CONFLICT só insere o que ainda não existe.
INSERT INTO clientes (nome, email, telefone, morada, codigo_postal, localidade, criado_em, alterado_em) VALUES
  ('Rita Moreira', 'rita@bikeshop.pt', '912345678', 'Rua das Flores 10', '4050-262', 'Porto', now(), now()),
  ('Tiago Fernandes', 'tiago@bikeshop.pt', '934567812', 'Avenida da Liberdade 120', '1250-146', 'Lisboa', now(), now()),
  ('Inês Carvalho', 'ines@bikeshop.pt', '965432187', 'Rua de Santa Catarina 300', '4000-447', 'Porto', now(), now())
ON CONFLICT (email) DO NOTHING;
