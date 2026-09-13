-- Gestão de Cultos: categorias (templates recorrentes) e instâncias de cultos com relatórios

CREATE TABLE categoria_culto (
    id                  UUID PRIMARY KEY,
    igreja_id           UUID NOT NULL REFERENCES igreja (id),
    titulo              VARCHAR(200) NOT NULL,
    descricao           TEXT,
    dia_semana          VARCHAR(20) NOT NULL,
    hora_inicio         TIME NOT NULL,
    duracao_minutos     INTEGER NOT NULL DEFAULT 90,
    pregador_padrao_id  UUID REFERENCES pessoa (id),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_categoria_culto_dia CHECK (
        dia_semana IN ('DOMINGO', 'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO')
    )
);

CREATE INDEX idx_categoria_culto_igreja ON categoria_culto (igreja_id);

CREATE TABLE culto (
    id                  UUID PRIMARY KEY,
    igreja_id           UUID NOT NULL REFERENCES igreja (id),
    categoria_culto_id  UUID NOT NULL REFERENCES categoria_culto (id),
    titulo              VARCHAR(200) NOT NULL,
    tema                VARCHAR(300),
    data                DATE NOT NULL,
    hora_inicio         TIME NOT NULL,
    hora_fim            TIME,
    pregador_id         UUID REFERENCES pessoa (id),
    louvor              TEXT,
    texto_pregacao      TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    escala_id           UUID REFERENCES escala (id),
    competencia         VARCHAR(7) NOT NULL,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_culto_status CHECK (status IN ('AGENDADO', 'ABERTO', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT uq_culto_categoria_data UNIQUE (categoria_culto_id, data)
);

CREATE INDEX idx_culto_igreja ON culto (igreja_id);
CREATE INDEX idx_culto_categoria ON culto (categoria_culto_id);
CREATE INDEX idx_culto_competencia ON culto (competencia);
CREATE INDEX idx_culto_escala ON culto (escala_id);

CREATE TABLE culto_relatorio (
    id              UUID PRIMARY KEY,
    culto_id        UUID NOT NULL REFERENCES culto (id) ON DELETE CASCADE,
    adultos         INTEGER NOT NULL DEFAULT 0,
    criancas        INTEGER NOT NULL DEFAULT 0,
    conversoes      INTEGER NOT NULL DEFAULT 0,
    reconciliacoes  INTEGER NOT NULL DEFAULT 0,
    aliancas        INTEGER NOT NULL DEFAULT 0,
    carros          INTEGER NOT NULL DEFAULT 0,
    motos           INTEGER NOT NULL DEFAULT 0,
    bicicletas      INTEGER NOT NULL DEFAULT 0,
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_culto_relatorio UNIQUE (culto_id)
);

CREATE INDEX idx_culto_relatorio_culto ON culto_relatorio (culto_id);
