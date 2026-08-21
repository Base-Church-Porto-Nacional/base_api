CREATE TABLE pessoa_historico (
    id                  UUID PRIMARY KEY,
    pessoa_id           UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    descricao           TEXT NOT NULL,
    autor_nome          VARCHAR(150) NOT NULL,
    autor_cargo         VARCHAR(100),
    autor_pessoa_id     UUID REFERENCES pessoa (id),
    data_registro       DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pessoa_historico_pessoa ON pessoa_historico (pessoa_id);
CREATE INDEX idx_pessoa_historico_data ON pessoa_historico (data_registro DESC);
