-- Rede de igrejas (matriz + filiais), escalas mensais e conteúdo do culto

CREATE TABLE igreja (
    id                  UUID PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    tipo                VARCHAR(20) NOT NULL,
    igreja_matriz_id    UUID REFERENCES igreja (id),
    pastor_pessoa_id    UUID,
    admin_usuario_id    UUID,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_igreja_tipo CHECK (tipo IN ('MATRIZ', 'FILIAL')),
    CONSTRAINT chk_igreja_matriz_filial CHECK (
        (tipo = 'MATRIZ' AND igreja_matriz_id IS NULL)
        OR (tipo = 'FILIAL' AND igreja_matriz_id IS NOT NULL)
    )
);

CREATE INDEX idx_igreja_tipo ON igreja (tipo);
CREATE INDEX idx_igreja_matriz ON igreja (igreja_matriz_id);

INSERT INTO igreja (id, nome, tipo, ativo)
VALUES ('c1000000-0000-4000-8000-000000000001', 'Base Church', 'MATRIZ', TRUE);

ALTER TABLE pessoa
    ADD COLUMN igreja_id UUID REFERENCES igreja (id);

UPDATE pessoa
SET igreja_id = 'c1000000-0000-4000-8000-000000000001'
WHERE igreja_id IS NULL;

ALTER TABLE pessoa
    ALTER COLUMN igreja_id SET NOT NULL;

CREATE INDEX idx_pessoa_igreja ON pessoa (igreja_id);

ALTER TABLE igreja
    ADD CONSTRAINT fk_igreja_pastor FOREIGN KEY (pastor_pessoa_id) REFERENCES pessoa (id);

ALTER TABLE usuario
    ADD COLUMN igreja_id UUID REFERENCES igreja (id);

UPDATE usuario
SET igreja_id = 'c1000000-0000-4000-8000-000000000001'
WHERE igreja_id IS NULL;

CREATE INDEX idx_usuario_igreja ON usuario (igreja_id);

ALTER TABLE igreja
    ADD CONSTRAINT fk_igreja_admin FOREIGN KEY (admin_usuario_id) REFERENCES usuario (id);

ALTER TABLE ministerio
    ADD COLUMN igreja_id UUID REFERENCES igreja (id),
    ADD COLUMN lider_pessoa_id UUID REFERENCES pessoa (id),
    ADD COLUMN coordenador_pessoa_id UUID REFERENCES pessoa (id),
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'GERAL',
    ADD COLUMN anotacoes TEXT,
    ADD COLUMN ordem TEXT;

UPDATE ministerio
SET igreja_id = 'c1000000-0000-4000-8000-000000000001'
WHERE igreja_id IS NULL;

ALTER TABLE ministerio
    ALTER COLUMN igreja_id SET NOT NULL;

ALTER TABLE ministerio DROP CONSTRAINT IF EXISTS ministerio_nome_key;

ALTER TABLE ministerio
    ADD CONSTRAINT uq_ministerio_igreja_nome UNIQUE (igreja_id, nome),
    ADD CONSTRAINT chk_ministerio_tipo CHECK (tipo IN ('GERAL', 'MUSICA', 'MIDIA'));

CREATE INDEX idx_ministerio_igreja ON ministerio (igreja_id);

ALTER TABLE servico
    ADD COLUMN ficcao BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE cronograma_evento
    ADD COLUMN igreja_id UUID REFERENCES igreja (id),
    ADD COLUMN supervisor_pessoa_id UUID REFERENCES pessoa (id);

UPDATE cronograma_evento
SET igreja_id = 'c1000000-0000-4000-8000-000000000001'
WHERE igreja_id IS NULL;

ALTER TABLE cronograma_evento
    ALTER COLUMN igreja_id SET NOT NULL;

CREATE INDEX idx_cronograma_igreja ON cronograma_evento (igreja_id);

CREATE TABLE cronograma_conteudo (
    id                  UUID PRIMARY KEY,
    evento_id           UUID NOT NULL UNIQUE REFERENCES cronograma_evento (id) ON DELETE CASCADE,
    texto_pregacao      TEXT,
    louvor              TEXT,
    pregador_pessoa_id  UUID REFERENCES pessoa (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE escala
    ADD COLUMN igreja_id UUID REFERENCES igreja (id),
    ADD COLUMN competencia VARCHAR(7),
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'CULTO',
    ADD COLUMN ministerio_id UUID REFERENCES ministerio (id),
    ADD COLUMN supervisor_pessoa_id UUID REFERENCES pessoa (id),
    ADD COLUMN pregador_pessoa_id UUID REFERENCES pessoa (id);

UPDATE escala
SET igreja_id = 'c1000000-0000-4000-8000-000000000001',
    competencia = TO_CHAR(data_evento, 'YYYY-MM')
WHERE igreja_id IS NULL;

ALTER TABLE escala
    ALTER COLUMN igreja_id SET NOT NULL,
    ALTER COLUMN competencia SET NOT NULL,
    ADD CONSTRAINT chk_escala_tipo CHECK (tipo IN ('SUPERVISOR', 'PREGADOR', 'CULTO', 'MINISTERIO')),
    ADD CONSTRAINT chk_escala_competencia CHECK (competencia ~ '^\d{4}-\d{2}$');

CREATE INDEX idx_escala_igreja ON escala (igreja_id);
CREATE INDEX idx_escala_competencia ON escala (competencia);
CREATE INDEX idx_escala_tipo ON escala (tipo);

ALTER TABLE escala_voluntario
    ALTER COLUMN servico_id DROP NOT NULL,
    ADD COLUMN data_item DATE,
    ADD COLUMN evento_id UUID REFERENCES cronograma_evento (id),
    ADD COLUMN horario TIME;

ALTER TABLE escala_voluntario DROP CONSTRAINT IF EXISTS uq_escala_pessoa_servico;

CREATE UNIQUE INDEX uq_escala_item
    ON escala_voluntario (
        escala_id,
        pessoa_id,
        COALESCE(servico_id, '00000000-0000-0000-0000-000000000000'),
        COALESCE(data_item, DATE '1900-01-01')
    );
