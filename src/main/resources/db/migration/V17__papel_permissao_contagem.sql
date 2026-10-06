-- Permissão de lançar a contagem do culto em papéis customizados (ex.: CONTADOR).
-- PASTOR e ADMIN já podem lançar; esta flag estende a permissão a papéis da igreja.

ALTER TABLE papel
    ADD COLUMN pode_lancar_contagem BOOLEAN NOT NULL DEFAULT FALSE;
