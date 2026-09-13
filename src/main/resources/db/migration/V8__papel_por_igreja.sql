-- Isolamento multi-tenant do catálogo de papéis.
-- Papéis de sistema (ADMIN, PASTOR, LIDER, VOLUNTARIO, MEMBRO) permanecem globais (igreja_id IS NULL);
-- papéis customizados passam a pertencer a uma igreja.

ALTER TABLE papel
    ADD COLUMN igreja_id UUID REFERENCES igreja (id);

-- A antiga unicidade global de nome não vale mais para papéis customizados de igrejas distintas.
ALTER TABLE papel DROP CONSTRAINT IF EXISTS papel_nome_key;

-- Nome único entre os papéis de sistema (globais).
CREATE UNIQUE INDEX ux_papel_sistema_nome ON papel (nome) WHERE igreja_id IS NULL;

-- Nome único por igreja para papéis customizados.
CREATE UNIQUE INDEX ux_papel_igreja_nome ON papel (igreja_id, nome) WHERE igreja_id IS NOT NULL;

CREATE INDEX idx_papel_igreja ON papel (igreja_id);
