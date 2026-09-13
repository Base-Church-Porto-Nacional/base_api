-- Eventos do cronograma com propriedades de culto (tema, status, competência)
-- e ministérios participantes, com sugestão de escala por evento.

ALTER TABLE cronograma_evento
    ADD COLUMN tema        VARCHAR(300),
    ADD COLUMN status      VARCHAR(20) NOT NULL DEFAULT 'AGENDADO',
    ADD COLUMN competencia VARCHAR(7);

UPDATE cronograma_evento
SET competencia = TO_CHAR(data_evento, 'YYYY-MM')
WHERE competencia IS NULL;

ALTER TABLE cronograma_evento
    ALTER COLUMN competencia SET NOT NULL,
    ADD CONSTRAINT chk_cronograma_status CHECK (
        status IN ('AGENDADO', 'ABERTO', 'REALIZADO', 'CANCELADO')
    ),
    ADD CONSTRAINT chk_cronograma_competencia CHECK (competencia ~ '^\d{4}-\d{2}$');

CREATE INDEX idx_cronograma_competencia ON cronograma_evento (competencia);

CREATE TABLE evento_ministerio (
    id              UUID PRIMARY KEY,
    evento_id       UUID NOT NULL REFERENCES cronograma_evento (id) ON DELETE CASCADE,
    ministerio_id   UUID NOT NULL REFERENCES ministerio (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evento_ministerio UNIQUE (evento_id, ministerio_id)
);

CREATE INDEX idx_evento_ministerio_evento ON evento_ministerio (evento_id);
CREATE INDEX idx_evento_ministerio_ministerio ON evento_ministerio (ministerio_id);