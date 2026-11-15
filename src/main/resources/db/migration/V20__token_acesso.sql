-- Fase 2: tokens de uso único (convite e redefinição de senha). Só o hash SHA-256 é gravado.
CREATE TABLE token_acesso (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo        VARCHAR(20) NOT NULL,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expira_em   TIMESTAMPTZ NOT NULL,
    usado_em    TIMESTAMPTZ,
    criado_por  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_token_acesso_tipo CHECK (tipo IN ('CONVITE', 'REDEFINICAO'))
);

CREATE INDEX idx_token_acesso_usuario_tipo ON token_acesso (usuario_id, tipo);
