-- Campo dedicado de WhatsApp do membro, separado do telefone (que pode ser fixo).
-- Usado pelo canal de mensagens; aceita máscara e é normalizado no envio.

ALTER TABLE pessoa
    ADD COLUMN whatsapp VARCHAR(30);

CREATE INDEX idx_pessoa_whatsapp ON pessoa (whatsapp) WHERE whatsapp IS NOT NULL;
