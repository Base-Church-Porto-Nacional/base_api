ALTER TABLE pessoa_servico
    ADD COLUMN cargo VARCHAR(100);

COMMENT ON COLUMN pessoa_servico.cargo IS 'Cargo/função do voluntário no vínculo (ex: Líder, Auxiliar, Vocal)';
