-- Papéis customizados que colidem com os nomes de gestão não podem virar gestão por nome.
UPDATE papel SET nome = nome || '_IGREJA' WHERE igreja_id IS NOT NULL AND nome IN ('PASTOR', 'SECRETARIA');

-- Papéis de sistema passam a ser apenas PASTOR e SECRETARIA (ADMIN vira SECRETARIA).
UPDATE papel SET nome = 'SECRETARIA' WHERE nome = 'ADMIN' AND igreja_id IS NULL;

DELETE FROM usuario_papel WHERE papel_id IN
  (SELECT id FROM papel WHERE igreja_id IS NULL AND nome IN ('LIDER', 'VOLUNTARIO', 'MEMBRO'));

DELETE FROM papel WHERE igreja_id IS NULL AND nome IN ('LIDER', 'VOLUNTARIO', 'MEMBRO');
