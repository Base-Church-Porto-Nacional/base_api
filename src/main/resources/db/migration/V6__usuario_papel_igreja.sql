-- Papel por igreja em usuario_papel

ALTER TABLE usuario_papel
    ADD COLUMN igreja_id UUID REFERENCES igreja (id);

UPDATE usuario_papel up
SET igreja_id = u.igreja_id
FROM usuario u
WHERE up.usuario_id = u.id
  AND up.igreja_id IS NULL;

UPDATE usuario_papel
SET igreja_id = 'c1000000-0000-4000-8000-000000000001'
WHERE igreja_id IS NULL;

ALTER TABLE usuario_papel
    ALTER COLUMN igreja_id SET NOT NULL;

ALTER TABLE usuario_papel DROP CONSTRAINT IF EXISTS usuario_papel_pkey;

ALTER TABLE usuario_papel
    ADD PRIMARY KEY (usuario_id, papel_id, igreja_id);

CREATE INDEX idx_usuario_papel_igreja ON usuario_papel (igreja_id);
