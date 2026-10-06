-- Mensagem padrão de aniversário configurável por igreja. Sem registro, a filial
-- herda a mensagem da matriz e, na falta dela, vale o template de configuração.

CREATE TABLE mensagem_aniversario (
    id         UUID PRIMARY KEY,
    igreja_id  UUID NOT NULL REFERENCES igreja (id) ON DELETE CASCADE,
    mensagem   TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mensagem_aniversario_igreja UNIQUE (igreja_id)
);
