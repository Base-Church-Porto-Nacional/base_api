-- Cronograma (cultos/eventos) — domínio separado de escala
CREATE TABLE cronograma_evento (
    id              UUID PRIMARY KEY,
    titulo          VARCHAR(200) NOT NULL,
    tipo            VARCHAR(30) NOT NULL DEFAULT 'CULTO',
    descricao       TEXT,
    data_evento     DATE NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fim        TIME,
    local_evento    VARCHAR(200),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cronograma_tipo CHECK (
        tipo IN ('CULTO', 'EVENTO', 'ENSAIO', 'REUNIAO', 'OUTRO')
    )
);

CREATE INDEX idx_cronograma_data ON cronograma_evento (data_evento);
CREATE INDEX idx_cronograma_tipo ON cronograma_evento (tipo);

-- Escala pode opcionalmente referenciar um evento do cronograma
ALTER TABLE escala
    ADD COLUMN evento_id UUID REFERENCES cronograma_evento (id);

CREATE INDEX idx_escala_evento ON escala (evento_id);

-- Pausa no perfil (afastamento temporário)
CREATE TABLE pessoa_pausa (
    id              UUID PRIMARY KEY,
    pessoa_id       UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    data_inicio     DATE NOT NULL,
    data_fim        DATE,
    motivo          VARCHAR(255),
    observacoes     TEXT,
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pausa_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);

CREATE INDEX idx_pessoa_pausa_pessoa ON pessoa_pausa (pessoa_id);
CREATE INDEX idx_pessoa_pausa_ativa ON pessoa_pausa (ativa);
