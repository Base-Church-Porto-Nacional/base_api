-- Fase 2: usuário sem username, com telefone e status, sempre ligado a uma pessoa.

-- 1. Toda conta passa a ter pessoa. Para cada usuário sem pessoa, gera o id da pessoa uma única vez
--    e, no mesmo comando, cria a pessoa (nome = username antigo) e liga o usuário a ela.
--    Não casa por nome: homônimos seriam ligados à pessoa errada.
WITH novos AS (
    SELECT u.id                AS usuario_id,
           gen_random_uuid()   AS pessoa_id,
           u.username          AS nome,
           u.email             AS email,
           COALESCE(u.igreja_id,
                    (SELECT i.id FROM igreja i WHERE i.tipo = 'MATRIZ' ORDER BY i.created_at LIMIT 1)) AS igreja_id
    FROM usuario u
    WHERE u.pessoa_id IS NULL
), ins_pessoa AS (
    INSERT INTO pessoa (id, nome, email, igreja_id, data_cadastro, ativo, created_at, updated_at)
    SELECT pessoa_id, nome, email, igreja_id, CURRENT_DATE, TRUE, now(), now()
    FROM novos
    RETURNING id
), ins_jornada AS (
    -- Espelha JornadaService.criarJornadaInicial: a tela da pessoa exige a jornada.
    INSERT INTO jornada_membro (id, pessoa_id, created_at, updated_at)
    SELECT gen_random_uuid(), id, now(), now()
    FROM ins_pessoa
)
UPDATE usuario u
SET pessoa_id  = n.pessoa_id,
    igreja_id  = COALESCE(u.igreja_id, n.igreja_id)
FROM novos n
WHERE u.id = n.usuario_id;

ALTER TABLE usuario ALTER COLUMN pessoa_id SET NOT NULL;

-- 2. Telefone (normalizado: DDI 55 + DDD + número) como segundo identificador de login.
ALTER TABLE usuario ADD COLUMN telefone VARCHAR(30);
CREATE UNIQUE INDEX ux_usuario_telefone ON usuario (telefone) WHERE telefone IS NOT NULL;

-- 3. Status da conta substitui o booleano ativo.
ALTER TABLE usuario ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
UPDATE usuario SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'DESATIVADO' END;
ALTER TABLE usuario DROP COLUMN ativo;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_status CHECK (status IN ('CONVIDADO', 'ATIVO', 'DESATIVADO'));

-- 4. Conta convidada ainda não tem senha; e-mail deixa de ser obrigatório (telefone basta).
ALTER TABLE usuario ALTER COLUMN senha_hash DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL;

-- 5. Username deixa de existir (o índice idx_usuario_username cai junto com a coluna).
ALTER TABLE usuario DROP COLUMN username;

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_identificador CHECK (email IS NOT NULL OR telefone IS NOT NULL);
