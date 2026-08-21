-- Estrutura inicial do Sistema de Gestão da Igreja

CREATE TABLE pessoa (
    id              UUID PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    nome_social     VARCHAR(150),
    email           VARCHAR(255),
    telefone        VARCHAR(30),
    data_nascimento DATE,
    data_cadastro   DATE NOT NULL DEFAULT CURRENT_DATE,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pessoa_nome ON pessoa (nome);
CREATE INDEX idx_pessoa_email ON pessoa (email);
CREATE INDEX idx_pessoa_ativo ON pessoa (ativo);

CREATE TABLE jornada_membro (
    id                          UUID PRIMARY KEY,
    pessoa_id                   UUID NOT NULL UNIQUE REFERENCES pessoa (id) ON DELETE CASCADE,
    aceitou_jesus               BOOLEAN NOT NULL DEFAULT FALSE,
    data_aceitou_jesus          DATE,
    batizado                    BOOLEAN NOT NULL DEFAULT FALSE,
    data_batismo                DATE,
    concluiu_trilha_base        BOOLEAN NOT NULL DEFAULT FALSE,
    data_conclusao_trilha_base  DATE,
    membro                      BOOLEAN NOT NULL DEFAULT FALSE,
    data_membro                 DATE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Catálogo extensível de etapas da jornada (permite novas etapas sem alterar a tabela principal)
CREATE TABLE jornada_etapa_catalogo (
    id          UUID PRIMARY KEY,
    codigo      VARCHAR(80) NOT NULL UNIQUE,
    nome        VARCHAR(150) NOT NULL,
    descricao   TEXT,
    ordem       INTEGER,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE pessoa_jornada_etapa (
    id              UUID PRIMARY KEY,
    pessoa_id       UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    etapa_id        UUID NOT NULL REFERENCES jornada_etapa_catalogo (id),
    concluida       BOOLEAN NOT NULL DEFAULT FALSE,
    data_conclusao  DATE,
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pessoa_jornada_etapa UNIQUE (pessoa_id, etapa_id)
);

CREATE INDEX idx_pessoa_jornada_etapa_pessoa ON pessoa_jornada_etapa (pessoa_id);

CREATE TABLE ministerio (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL UNIQUE,
    descricao   TEXT,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE servico (
    id              UUID PRIMARY KEY,
    ministerio_id   UUID NOT NULL REFERENCES ministerio (id),
    nome            VARCHAR(120) NOT NULL,
    descricao       TEXT,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_servico_ministerio_nome UNIQUE (ministerio_id, nome)
);

CREATE INDEX idx_servico_ministerio ON servico (ministerio_id);

-- Associação Pessoa ↔ Serviço (voluntariado). Ministério é derivado via serviço.
CREATE TABLE pessoa_servico (
    id                      UUID PRIMARY KEY,
    pessoa_id               UUID NOT NULL REFERENCES pessoa (id),
    servico_id              UUID NOT NULL REFERENCES servico (id),
    data_entrada            DATE,
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes             TEXT,
    lider_responsavel_id    UUID REFERENCES pessoa (id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_pessoa_servico UNIQUE (pessoa_id, servico_id)
);

CREATE INDEX idx_pessoa_servico_pessoa ON pessoa_servico (pessoa_id);
CREATE INDEX idx_pessoa_servico_servico ON pessoa_servico (servico_id);

CREATE TABLE papel (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(50) NOT NULL UNIQUE,
    descricao   VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario (
    id              UUID PRIMARY KEY,
    pessoa_id       UUID UNIQUE REFERENCES pessoa (id),
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usuario_username ON usuario (username);
CREATE INDEX idx_usuario_email ON usuario (email);

CREATE TABLE usuario_papel (
    usuario_id  UUID NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    papel_id    UUID NOT NULL REFERENCES papel (id),
    PRIMARY KEY (usuario_id, papel_id)
);

CREATE TABLE disponibilidade_semanal (
    id              UUID PRIMARY KEY,
    pessoa_id       UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    dia_semana      VARCHAR(20) NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fim        TIME NOT NULL,
    disponivel      BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_disponibilidade_horario CHECK (hora_fim > hora_inicio)
);

CREATE INDEX idx_disponibilidade_pessoa ON disponibilidade_semanal (pessoa_id);

CREATE TABLE indisponibilidade (
    id              UUID PRIMARY KEY,
    pessoa_id       UUID NOT NULL REFERENCES pessoa (id) ON DELETE CASCADE,
    data_inicio     DATE NOT NULL,
    data_fim        DATE NOT NULL,
    hora_inicio     TIME,
    hora_fim        TIME,
    motivo          TEXT,
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_indisponibilidade_periodo CHECK (data_fim >= data_inicio)
);

CREATE INDEX idx_indisponibilidade_pessoa ON indisponibilidade (pessoa_id);
CREATE INDEX idx_indisponibilidade_periodo ON indisponibilidade (data_inicio, data_fim);

CREATE TABLE escala (
    id              UUID PRIMARY KEY,
    titulo          VARCHAR(200) NOT NULL,
    descricao       TEXT,
    data_evento     DATE NOT NULL,
    hora_inicio     TIME NOT NULL,
    hora_fim        TIME,
    local_evento    VARCHAR(200),
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_escala_data ON escala (data_evento);

CREATE TABLE escala_servico (
    id                      UUID PRIMARY KEY,
    escala_id               UUID NOT NULL REFERENCES escala (id) ON DELETE CASCADE,
    servico_id              UUID NOT NULL REFERENCES servico (id),
    quantidade_necessaria   INTEGER NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_escala_servico UNIQUE (escala_id, servico_id),
    CONSTRAINT chk_quantidade_necessaria CHECK (quantidade_necessaria > 0)
);

CREATE INDEX idx_escala_servico_escala ON escala_servico (escala_id);

CREATE TABLE escala_voluntario (
    id              UUID PRIMARY KEY,
    escala_id       UUID NOT NULL REFERENCES escala (id) ON DELETE CASCADE,
    pessoa_id       UUID NOT NULL REFERENCES pessoa (id),
    servico_id      UUID NOT NULL REFERENCES servico (id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    observacoes     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_escala_pessoa_servico UNIQUE (escala_id, pessoa_id, servico_id),
    CONSTRAINT chk_escala_voluntario_status CHECK (
        status IN ('PENDENTE', 'CONFIRMADO', 'RECUSADO', 'SUBSTITUIDO')
    )
);

CREATE INDEX idx_escala_voluntario_escala ON escala_voluntario (escala_id);
CREATE INDEX idx_escala_voluntario_pessoa ON escala_voluntario (pessoa_id);
CREATE INDEX idx_escala_voluntario_status ON escala_voluntario (status);

-- Papéis iniciais (RBAC)
INSERT INTO papel (id, nome, descricao) VALUES
    ('a1000000-0000-4000-8000-000000000001', 'ADMIN', 'Administrador do sistema'),
    ('a1000000-0000-4000-8000-000000000002', 'PASTOR', 'Pastor'),
    ('a1000000-0000-4000-8000-000000000003', 'LIDER', 'Líder de ministério'),
    ('a1000000-0000-4000-8000-000000000004', 'VOLUNTARIO', 'Voluntário'),
    ('a1000000-0000-4000-8000-000000000005', 'MEMBRO', 'Membro');

-- Etapas iniciais do catálogo de jornada
INSERT INTO jornada_etapa_catalogo (id, codigo, nome, descricao, ordem) VALUES
    ('b1000000-0000-4000-8000-000000000001', 'VISITANTE', 'Visitante', 'Primeiro contato com a igreja', 1),
    ('b1000000-0000-4000-8000-000000000002', 'ACEITOU_JESUS', 'Aceitou Jesus', 'Decisão por Jesus', 2),
    ('b1000000-0000-4000-8000-000000000003', 'BATIZADO', 'Batizado', 'Batismo nas águas', 3),
    ('b1000000-0000-4000-8000-000000000004', 'TRILHA_BASE', 'Trilha da Base', 'Conclusão da trilha da base', 4),
    ('b1000000-0000-4000-8000-000000000005', 'MEMBRO', 'Membro', 'Tornou-se membro', 5),
    ('b1000000-0000-4000-8000-000000000006', 'VOLUNTARIO', 'Voluntário', 'Início do voluntariado', 6),
    ('b1000000-0000-4000-8000-000000000007', 'LIDER', 'Líder', 'Assumiu liderança', 7);
