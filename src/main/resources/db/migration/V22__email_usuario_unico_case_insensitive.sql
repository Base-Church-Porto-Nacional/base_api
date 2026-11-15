-- Fase 2: login por e-mail compara em minúsculas; a unicidade também precisa ignorar a caixa.
-- Se já houver contas cujo e-mail só difere na caixa, a migração falha de propósito: resolva os
-- duplicados (SELECT lower(email), count(*) FROM usuario GROUP BY 1 HAVING count(*) > 1) e suba de novo.
DO $$
DECLARE
    duplicados INTEGER;
BEGIN
    SELECT count(*) INTO duplicados
    FROM (SELECT lower(email) FROM usuario WHERE email IS NOT NULL GROUP BY lower(email) HAVING count(*) > 1) d;
    IF duplicados > 0 THEN
        RAISE EXCEPTION 'V22: % e-mail(s) de usuario repetido(s) ignorando maiúsculas/minúsculas; corrija antes de migrar', duplicados;
    END IF;
END $$;

CREATE UNIQUE INDEX ux_usuario_email_lower ON usuario (lower(email)) WHERE email IS NOT NULL;
