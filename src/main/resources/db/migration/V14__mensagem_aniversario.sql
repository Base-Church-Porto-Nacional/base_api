-- Mensagem de feliz aniversário: registro de envios por pessoa e data do aniversário,
-- com no máximo um envio por membro em cada aniversário (bloqueio de duplicidade).

CREATE TABLE aniversario_envio (
    id                 UUID PRIMARY KEY,
    pessoa_id          UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    igreja_id          UUID NOT NULL REFERENCES igreja (id),
    data_aniversario   DATE NOT NULL,
    data_processamento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    mensagem           TEXT NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    erro               TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_aniversario_envio UNIQUE (pessoa_id, data_aniversario),
    CONSTRAINT chk_aniversario_status CHECK (status IN ('PENDENTE', 'ENVIADO', 'FALHA'))
);

CREATE INDEX idx_aniversario_envio_pessoa ON aniversario_envio (pessoa_id);
CREATE INDEX idx_aniversario_envio_igreja ON aniversario_envio (igreja_id);
CREATE INDEX idx_aniversario_envio_data ON aniversario_envio (data_aniversario);

CREATE INDEX idx_pessoa_data_nascimento ON pessoa (data_nascimento) WHERE data_nascimento IS NOT NULL;