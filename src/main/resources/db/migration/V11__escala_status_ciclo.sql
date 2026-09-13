-- Ciclo de vida mensal da escala: sugerida -> editando -> em andamento -> finalizada

ALTER TABLE escala
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'EDITANDO',
    ADD COLUMN data_lancamento TIMESTAMPTZ,
    ADD COLUMN data_finalizacao TIMESTAMPTZ,
    ADD CONSTRAINT chk_escala_status CHECK (
        status IN ('SUGERIDA', 'EDITANDO', 'EM_ANDAMENTO', 'FINALIZADA')
    );

CREATE INDEX idx_escala_status ON escala (status);