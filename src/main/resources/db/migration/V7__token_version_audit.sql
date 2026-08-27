-- Versão de token para revogar JWT e trilha de auditoria

ALTER TABLE usuario
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    usuario_id  UUID REFERENCES usuario (id) ON DELETE SET NULL,
    igreja_id   UUID REFERENCES igreja (id) ON DELETE SET NULL,
    acao        VARCHAR(80) NOT NULL,
    recurso     VARCHAR(80) NOT NULL,
    detalhes    TEXT
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_usuario ON audit_log (usuario_id);
