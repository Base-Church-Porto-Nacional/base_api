# Papéis, responsabilidades e acesso — Design

Data: 2026-11-02 · Repos: `base_api` (backend) e `../base_interface` (Flutter)

## Contexto e problema

O fluxo de criar pastores, pessoas, usuários e senhas parece amador:

1. Papéis atribuídos (`LIDER`, `VOLUNTARIO`, `MEMBRO`) não refletem a realidade — quem lidera é definido por `ministerio.liderPessoaId`, quem é voluntário por `pessoa_servico`. Os papéis só controlam o menu do app.
2. `PASTOR` e `ADMIN` são idênticos na prática.
3. Criar filial gera um usuário ADMIN sem pessoa ("admin fantasma"), e o pastor indicado não ganha conta nem papel.
4. A senha é inventada por quem cadastra e repassada; não há convite, primeiro acesso nem "esqueci a senha". Login por `username` (mais um identificador).
5. Caminhos duplicados/incompletos: `POST /usuarios` (pessoa opcional) e `POST /pessoas/{id}/usuario`; promoção sem rebaixamento; `PUT /usuarios` faz tudo; exclusão física.
6. A regra "quem pode o quê" existe duas vezes (backend `AutorizacaoService` e Flutter `permissions.dart`) e diverge.

## Quem opera o sistema

- **Pastor** acompanha e decide.
- **Secretaria** opera o cadastro geral.
- **Líderes** cadastram o próprio pessoal.

## Decisões

- O papel vem da **função** (derivado), não de marcação manual. Papéis atribuídos são só `PASTOR`, `SECRETARIA` e customizados (ex.: `CONTADOR`).
- Fonte única de permissões no servidor (`PermissaoService`), exposta em `/api/me`; o app só obedece.
- Acesso por **link de convite** de uso único (WhatsApp, fallback "copiar link"); mesmo mecanismo para redefinição de senha.
- Login por **e-mail ou telefone**; `username` deixa de existir.
- Cada pessoa pertence a **uma** igreja; a matriz enxerga filiais por cascata.
- Sem usuários reais em produção: contrato da API pode quebrar; backend e Flutter atualizados juntos. Dados existentes migrados via Flyway.
- Entrega em 3 fases independentes: (1) papéis e capacidades, (2) convite e senha, (3) filial.

## Matriz de responsabilidades

### Pastor × Secretaria

| Ação | Pastor | Secretaria |
|---|---|---|
| Ver tudo da igreja (pessoas, escalas, cultos, relatórios) | ✅ | ✅ |
| Cadastrar/editar pessoas, criar contas, convidar | ✅ | ✅ |
| Criar ministérios, bases, pequenos grupos, categorias de culto | ✅ | ✅ |
| Nomear líderes e coordenadores | ✅ | ✅ |
| Escala de supervisor e pregador | ✅ | ✅ |
| Conteúdo do culto (texto/pregação) | ✅ | ❌ |
| Dar/tirar papel PASTOR ou SECRETARIA | ✅ | ❌ |
| Criar papéis customizados | ✅ | ✅ |
| Exportar dados, configurar mensagens | ✅ | ✅ |
| Criar filial (na matriz) / trocar pastor de filial | ✅ | ❌ |

Pastor e secretaria da matriz têm os mesmos poderes em todas as filiais (cascata).

### Líder (de ministério; o mesmo vale para líder/coordenador de base sobre os ministérios da base, e líder de pequeno grupo sobre seus participantes)

| Ação | Permitido |
|---|---|
| Ver pessoas do próprio escopo | ✅ |
| Cadastrar pessoa nova já vinculada a um serviço do seu ministério | ✅ |
| Vincular pessoa existente da igreja ao seu ministério (busca por nome, sem ficha completa) | ✅ |
| Editar dados de contato das pessoas do escopo | ✅ |
| Editar dados pastorais (jornada, histórico, observações) | ❌ |
| Convidar alguém do escopo para ter conta (nasce só como membro) | ✅ |
| Desativar conta / excluir pessoa | ❌ |
| Montar escalas do próprio ministério | ✅ |

Demais regras atuais permanecem (supervisor do mês cria escala de culto; pregador edita o texto do seu culto; líder de música edita louvor; mídia vê conteúdo; papel customizado com `podeLancarContagem` lança contagem). Todo usuário ativo é membro de base (vê a própria ficha, escalas, disponibilidade, cronograma).

## Fase 1 — Papéis e capacidades

**Papéis atribuídos** (`papel`, por igreja):
- Sistema: `PASTOR`, `SECRETARIA` (migração renomeia `ADMIN` → `SECRETARIA`; remove `LIDER`, `VOLUNTARIO`, `MEMBRO` e seus vínculos em `usuario_papel`).
- Customizados: inalterados (inclui `podeLancarContagem`).
- Nomes reservados: `ADMIN`, `LIDER`, `VOLUNTARIO`, `MEMBRO`, `COORDENADOR`, `SUPERVISOR`, `PREGADOR`.

**Funções derivadas** (nunca persistidas): líder/coordenador de ministério, base ou pequeno grupo; voluntário (vínculo ativo em `pessoa_servico`); supervisor do mês (escala SUPERVISOR da competência).

**`security/PermissaoService`** constrói, por requisição (bean `@RequestScope`, cache por igreja), `Permissoes`:
- `igrejaId`
- `capacidades: Set<Capacidade>` (enum) — derivadas dos papéis atribuídos conforme a matriz. Exclusivas de PASTOR: `CULTO_CONTEUDO_EDITAR`, `PAPEIS_GESTAO_ATRIBUIR`, `FILIAL_CRIAR`. Papel customizado com contagem: `CULTO_CONTAGEM`.
- `ministeriosLiderados`, `basesLideradas`, `gruposLiderados: Set<UUID>`, `ehVoluntario: boolean`.

**`AutorizacaoService`** permanece como API dos services, reescrito sobre `PermissaoService`:
- `assertPodeMutarPessoas` → `assertPodeGerirPessoa(pessoa)` (pastor/secretaria: tudo; líder: contato e vínculo dentro do escopo) e `assertPodeEditarDadosPastorais(pessoa)` (só pastor/secretaria).
- Todo "pastor ou admin" vira "pastor ou secretaria", exceto as ações exclusivas do pastor.
- `IgrejaContextoService` mantém a cascata matriz → filiais com `PASTOR`/`SECRETARIA`.

**Contrato com o app:**
- `GET /api/me` inclui `permissoes { capacidades[], ministeriosLiderados[], basesLideradas[], gruposLiderados[], ehVoluntario }` e `papeis[]` (atribuídos, para exibição).
- JWT deixa de carregar `papeis`; mantém identidade (`sub`, `pessoaId`, `igrejaId`, `igrejaTipo`, `tokenVersion`).
- Flutter remove `_rolePermissions`; menu/botões consultam `permissoes`. `/me` é recarregado ao abrir o app e após ações que mudem funções.

## Fase 2 — Ciclo de vida da conta

**`Usuario`:**
- `pessoa_id` obrigatório (1:1). Migração cria `Pessoa` para usuários sem pessoa (nome = username antigo).
- Remove `username`. Identificadores de login: `email` (único, opcional) e `telefone` (único, opcional, normalizado por `NormalizadorTelefoneWhatsapp`); ao menos um. Copiados da pessoa no convite.
- `ativo` → `status`: `CONVIDADO` (sem senha, `senha_hash` nulo) | `ATIVO` | `DESATIVADO`.
- Sem exclusão física. Desativar incrementa `tokenVersion` e revoga tokens de acesso pendentes.

**`token_acesso`:** `id`, `usuario_id`, `tipo` (`CONVITE`|`REDEFINICAO`), `token_hash` (SHA-256 de 32 bytes aleatórios, base64url), `expira_em` (convite 7 dias, redefinição 1 hora), `usado_em`, `criado_por`, `created_at`. Gerar novo token invalida o anterior do mesmo tipo/usuário.

**Endpoints autenticados:**
- `POST /api/pessoas/{id}/convite` → cria usuário `CONVIDADO` se não existir, gera link, tenta WhatsApp. Resposta `{ link, enviadoWhatsapp }`. Reenvio = mesma chamada. Pastor/secretaria: qualquer pessoa; líder: pessoas do escopo.
- `POST /api/usuarios/{id}/redefinicao-senha` → `{ link, enviadoWhatsapp }` (pastor/secretaria).
- `PUT /api/usuarios/{id}/papeis { papeis[] }` → PASTOR/SECRETARIA só pelo pastor; customizados por pastor/secretaria. Auditado. Incrementa `tokenVersion` não é necessário (capacidades não estão no JWT).
- `POST /api/usuarios/{id}/desativar`, `/reativar` (pastor/secretaria). Auditado.
- `GET /api/usuarios` permanece (listagem com status/papéis) para alimentar o filtro "com acesso".
- Removidos: `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `POST /api/usuarios/{id}/gestao`, `DELETE /api/usuarios/{id}`, `POST /api/pessoas/{id}/usuario`.
- `/api/me`: troca de senha com `senhaAtual` (mín. 8), atualização de dados pessoais.

**Endpoints públicos** (rate limit):
- `POST /api/auth/login { identificador, senha }` — identificador = e-mail ou telefone.
- `GET /api/auth/token/{token}` → `{ tipo, primeiroNome }` se válido.
- `POST /api/auth/definir-senha { token, senha }` (mín. 8) → status `ATIVO`, consome o token, retorna `LoginResponse`.
- `POST /api/auth/esqueci-senha { identificador }` → sempre 204; se conta `ATIVO` com WhatsApp, envia link de redefinição.

**Link:** `${app.frontend.url-base}/acesso/{token}` → rota do Flutter Web. Deep link nativo fora do escopo.

**App:** detalhe da pessoa ganha bloco "Acesso ao app" (status + Convidar/Reenviar, Redefinir senha, Desativar/Reativar, Papéis). Tela "Usuários" separada vira filtro "com acesso" em Pessoas. Telas públicas: definir senha (via link), esqueci a senha. Login com campo "E-mail ou telefone".

## Fase 3 — Filial

- `POST /api/igrejas/{matrizId}/filiais { nome, pastor: { pessoaId } | { nome, email?, whatsapp? } }` — só PASTOR da matriz.
- Pessoa existente de outra igreja é **transferida** para a filial (pessoa e usuário mudam `igrejaId`; papéis na igreja antiga removidos).
- O pastor recebe `PASTOR` na filial; se não tem conta, convite é gerado. Resposta `{ igreja, convite?: { link, enviadoWhatsapp } }`.
- Remove `igreja.admin_usuario_id`; `pastor_pessoa_id` = pastor titular.
- `PUT /api/igrejas/{id}/pastor { pessoaId }` (pastor da matriz ou titular da filial): novo recebe `PASTOR`; anterior perde `PASTOR` naquela igreja. Auditado.
- `AdminBootstrap`: primeiro usuário vira pastor titular da matriz com `PASTOR` + `SECRETARIA`, login por `ADMIN_EMAIL`; `ADMIN_USERNAME` deixa de ser usado.
- `DataInitializer` (dev) atualizado ao novo modelo (logins por e-mail, ex.: `pastor@…`, `secretaria@…`, senha `admin123`).

## Migrações (Flyway, novas)

- Fase 1: papéis (`ADMIN`→`SECRETARIA`, remoção de `LIDER`/`VOLUNTARIO`/`MEMBRO` e vínculos).
- Fase 2: usuário (pessoa obrigatória, `telefone`, `status`, `senha_hash` nulo, remove `username`) + `token_acesso`.
- Fase 3: remove `igreja.admin_usuario_id`.

## Erros

| Situação | Resposta |
|---|---|
| Link expirado ou já usado | 410 — "Peça um novo convite" |
| E-mail/telefone já usado por outra conta | 409 |
| Convite para pessoa sem e-mail e sem WhatsApp | 422 — "Cadastre e-mail ou WhatsApp antes de convidar" |
| Login de conta `CONVIDADO`/`DESATIVADO` ou credencial errada | 401 genérico |
| Ação fora da permissão | 403 |

## Testes

- `PermissaoServiceTest`: um caso por linha das matrizes, cascata matriz→filial, funções derivadas.
- Testes de convite/token: expiração, uso único, invalidação do anterior, hash, WhatsApp desabilitado → `enviadoWhatsapp=false`.
- `security/`: endpoints públicos com rate limit, JWT sem papéis, desativação invalida token, login por e-mail e por telefone.
- Migrações rodando em `igreja_db_test`.
- Flutter: telas de acesso/convite/papéis; substituição de `permissions.dart`.

## Fora do escopo

Deep link nativo iOS/Android, envio de e-mail, pessoa em várias igrejas, autocadastro.
