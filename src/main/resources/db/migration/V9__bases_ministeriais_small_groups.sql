-- Bases ministeriais e pequenos grupos
-- Cada base agrupa ministérios e tem líder/coordenador próprios.

CREATE TABLE base_ministerial (
    id                    UUID PRIMARY KEY,
    nome                  VARCHAR(120) NOT NULL,
    descricao             TEXT,
    igreja_id             UUID NOT NULL REFERENCES igreja (id),
    lider_pessoa_id       UUID REFERENCES pessoa (id),
    coordenador_pessoa_id UUID REFERENCES pessoa (id),
    ordem                 TEXT,
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_base_ministerial_igreja_nome UNIQUE (igreja_id, nome)
);

CREATE INDEX idx_base_ministerial_igreja ON base_ministerial (igreja_id);

-- Cada ministério pode pertencer a uma base ministerial.
ALTER TABLE ministerio
    ADD COLUMN base_ministerial_id UUID REFERENCES base_ministerial (id);

CREATE INDEX idx_ministerio_base ON ministerio (base_ministerial_id);

-- Pequeno grupo: encontro semanal, nota (0-5) fica no grupo.
-- Cada grupo tem UM líder; o ministério de pequenos grupos (ministerio_id) tem líder/coordenador
-- e é quem gerencia os grupos.
CREATE TABLE small_group (
    id              UUID PRIMARY KEY,
    nome            VARCHAR(120) NOT NULL,
    igreja_id       UUID NOT NULL REFERENCES igreja (id),
    dia_semana      VARCHAR(20) NOT NULL,
    horario         VARCHAR(10),
    lider_pessoa_id UUID REFERENCES pessoa (id),
    ministerio_id   UUID REFERENCES ministerio (id),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    nota            SMALLINT NOT NULL DEFAULT 0 CHECK (nota BETWEEN 0 AND 5),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_small_group_igreja_nome UNIQUE (igreja_id, nome),
    CONSTRAINT chk_small_group_dia CHECK (
        dia_semana IN ('DOMINGO', 'SEGUNDA', 'TERCA', 'QUARTA', 'QUINTA', 'SEXTA', 'SABADO')
    )
);

CREATE INDEX idx_small_group_igreja ON small_group (igreja_id);
CREATE INDEX idx_small_group_ministerio ON small_group (ministerio_id);

CREATE TABLE small_group_encontro (
    id                  UUID PRIMARY KEY,
    small_group_id      UUID NOT NULL REFERENCES small_group (id) ON DELETE CASCADE,
    data                DATE NOT NULL,
    anfitriao_pessoa_id UUID REFERENCES pessoa (id),
    anotacoes           TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_small_group_encontro_data UNIQUE (small_group_id, data)
);

CREATE INDEX idx_encontro_small_group ON small_group_encontro (small_group_id);

CREATE TABLE small_group_participante (
    id          UUID PRIMARY KEY,
    encontro_id UUID NOT NULL REFERENCES small_group_encontro (id) ON DELETE CASCADE,
    pessoa_id   UUID REFERENCES pessoa (id),
    pessoa_nome VARCHAR(150) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_encontro_participante UNIQUE (encontro_id, pessoa_id)
);

CREATE INDEX idx_participante_encontro ON small_group_participante (encontro_id);