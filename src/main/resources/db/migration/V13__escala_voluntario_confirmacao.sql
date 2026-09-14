-- Marca se a confirmação da designação foi registrada em nome do voluntário
-- (aprovado pelo voluntário), quando o líder/coordenador confirma por ele.

ALTER TABLE escala_voluntario
    ADD COLUMN confirmado_pelo_voluntario BOOLEAN NOT NULL DEFAULT FALSE;