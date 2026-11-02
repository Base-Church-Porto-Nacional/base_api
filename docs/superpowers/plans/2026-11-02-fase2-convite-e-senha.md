# Fase 2 — Convite e Senha Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ninguém mais inventa senha para ninguém: a pessoa recebe um link de convite (WhatsApp ou copiado), cria a própria senha e entra com e-mail ou telefone; o mesmo link serve para "esqueci minha senha".

**Architecture:** `Usuario` perde `username`, ganha `telefone` e `status` (`CONVIDADO`/`ATIVO`/`DESATIVADO`) e passa a exigir pessoa. Uma tabela `token_acesso` guarda só o hash de tokens de uso único. `AcessoService` cria convites/redefinições e envia pelo `CanalMensagem` (WhatsApp) existente; `AuthController` ganha os endpoints públicos. O JWT passa a ter o id do usuário como `sub`. O Flutter troca o login, ganha telas públicas de definir senha e esqueci a senha, e o bloco "Acesso ao app" no detalhe da pessoa substitui a tela de usuários.

**Tech Stack:** Spring Boot 4.1 / Java 25, Flyway, JJWT, Mockito (`base_api`); Flutter/Dart, go_router, provider (`../base_interface`, branch `feat/papeis-capacidades`).

**Spec:** `docs/superpowers/specs/2026-11-02-papeis-e-acesso-design.md` (seção "Fase 2 — Ciclo de vida da conta" e tabela "Erros"). A Fase 1 já está implementada; `PUT /api/usuarios/{id}/papeis` já existe e não muda.

## Global Constraints

- Português em código, mensagens e paths; `/api/{recurso}` sem versão; regras de autorização nos services via `AutorizacaoService`.
- Schema só por migrações novas (`V19__…`, `V20__…`); nunca editar migração aplicada (V18 já está no branch e não deve mudar).
- Login por `identificador` = e-mail (contém `@`, comparado em minúsculas) ou telefone (normalizado por `NormalizadorTelefoneWhatsapp.normalizar`). `username` deixa de existir em todo o backend e no app.
- Senha: mínimo 8, máximo 100 caracteres (definir senha e troca em `/api/me`).
- Token: 32 bytes de `SecureRandom`, base64url sem padding; só o SHA-256 (hex) é gravado. Convite vale 7 dias; redefinição vale 1 hora. Gerar um token novo invalida os pendentes do mesmo tipo do mesmo usuário.
- Link: `${app.frontend.url-base}/acesso/{token}`, com `app.frontend.url-base: ${FRONTEND_URL_BASE:http://localhost:3000}`.
- Erros: link expirado/usado/desconhecido → 410 "Este link expirou ou já foi usado. Peça um novo convite."; e-mail/telefone de outra conta → 409; convite sem e-mail e sem WhatsApp → 422 "Cadastre e-mail ou WhatsApp antes de convidar"; login de conta `CONVIDADO`/`DESATIVADO` ou credencial errada → 401 "Credenciais inválidas" (mesma mensagem).
- `POST /api/auth/esqueci-senha` responde sempre 204.
- Regras da Fase 1 continuam: alterar senha/status ou excluir conta com PASTOR/SECRETARIA de sistema exige `PAPEIS_GESTAO_ATRIBUIR`; o último pastor ativo não pode ser desativado.
- Contrato pode quebrar (sem produção); backend e app mudam juntos. `./mvnw test`, `flutter test` verdes e `flutter analyze` 0 erros ao fim de cada task do respectivo repo.
- Nunca commitar `deploy/stack.yml` (nos dois repos) nem `.playwright-mcp/`, `assets/`, `docs/` soltos do `base_interface`.

## Review Focus

1. Duas pessoas com o mesmo WhatsApp (família) → o segundo convite falha com 409 "Telefone já usado por outra conta", sem criar usuário pela metade (teste na Task 3).
2. Telefone digitado em formatos diferentes no login ("(63) 99999-0000", "63999990000", "+55 63 99999-0000") → mesma conta (teste na Task 1).
3. `esqueci-senha` para identificador desconhecido, conta `CONVIDADO` ou `DESATIVADO` → 204 e nada enviado (teste na Task 4).
4. Link usado uma vez e aberto de novo, ou duas abas enviando a senha → o segundo uso recebe 410 (teste na Task 4).
5. Conta desativada com convite pendente → o link morre (410) e reativar não ressuscita o link antigo (teste na Task 5).

---

### Task 1: Usuário sem username, com status e login por e-mail ou telefone

**Files:**
- Create: `src/main/resources/db/migration/V19__usuario_identificadores_status.sql`, `usuario/entity/StatusUsuario.java`, `security/Identificador.java`
- Modify: `usuario/entity/Usuario.java`, `usuario/repository/UsuarioRepository.java`, `security/{CustomUserDetailsService,JwtService,JwtAuthenticationFilter,UsuarioAutenticado,LoginRateLimiter}.java`, `igreja/service/IgrejaContextoService.java:44-45`, `audit/service/AuditService.java:51`, `usuario/dto/{LoginRequest,LoginResponse,UsuarioResponse,MeResponse,LiderResponse,UsuarioCreateRequest,PessoaUsuarioCreateRequest}.java`, `usuario/service/{UsuarioService,MeService,LiderService}.java`, `usuario/controller/AuthController.java`, `config/{AdminBootstrap,DataInitializer}.java`, `igreja/dto/IgrejaFilialCreateRequest.java`, `igreja/service/IgrejaService.java` (`criarFilial`), `export/service/ExportService.java`, `pessoa/service/PessoaService.java` (funções: MEMBRO = status `ATIVO`), `security/PermissaoService.java` se usar `ativo`
- Test: `security/IdentificadorTest.java`, `security/CustomUserDetailsServiceTest.java`, `security/JwtServiceTest.java`, `security/JwtAuthenticationFilterTest.java`, `usuario/service/UsuarioServiceTest.java`, `config/AdminBootstrapTest.java`

**Interfaces:**
- Produces:
  - `enum StatusUsuario { CONVIDADO, ATIVO, DESATIVADO }`; `Usuario`: remove `username` e `ativo`; adiciona `String telefone`, `StatusUsuario status`, `boolean isAtivo()` (= `status == ATIVO`); `senhaHash` pode ser nulo.
  - `final class Identificador` com `static Optional<String> email(String bruto)` (trim + minúsculas se contém `@`) e `static Optional<String> telefone(String bruto)` (via `NormalizadorTelefoneWhatsapp.normalizar`, só se não contém `@`).
  - `UsuarioRepository.findByEmailIgnoreCase`, `findByTelefone(String)`, `existsByTelefone(String)`, `Optional<Usuario> findByIdentificador(String bruto)` (default method usando `Identificador`); remove `findByUsername`, `existsByUsername`.
  - JWT: `JwtService.gerarToken(UUID usuarioId, UUID pessoaId, UUID igrejaId, String igrejaTipo, int tokenVersion)`; `sub` = `usuarioId.toString()`; `TokenValidado(UUID usuarioId, int tokenVersion)`. Filtro carrega por id e exige `isAtivo()`.
  - `LoginRequest(@NotBlank String identificador, @NotBlank String senha)`; `LoginResponse` sem `username`. Rate limit por `identificador` normalizado + IP.
  - `UsuarioResponse(id, pessoaId, pessoaNome, email, telefone, igrejaId, igrejaNome, StatusUsuario status, Set<String> papeis, createdAt, updatedAt)`; `MeResponse` e `LiderResponse` sem `username`.
  - `IgrejaFilialCreateRequest` sem `adminUsername`; `criarFilial` cria uma `Pessoa` (nome `adminNome`, e-mail `adminEmail`) na filial para o admin e o usuário `ATIVO` com login pelo e-mail (provisório até a Fase 3).
  - `AdminBootstrap`: login por `ADMIN_EMAIL`; `ADMIN_USERNAME` ignorado (log informativo se presente). `DataInitializer`: logins `pastor@base.local`, `secretaria@base.local`, `lider@base.local`, `membro@base.local` (ajuste os nomes ao seed existente), senha `admin123`.
  - `UsuarioService.criar`/`criarAPartirDaPessoa` (removidos na Task 5) perdem `username` e criam `ATIVO`.
- **V19:**
  ```sql
  -- pessoa para usuários sem pessoa (nome = username antigo), na igreja do usuário
  INSERT INTO pessoa (id, nome, email, igreja_id, data_cadastro, ativo, created_at, updated_at)
    SELECT gen_random_uuid(), u.username, u.email, u.igreja_id, CURRENT_DATE, TRUE, now(), now()
    FROM usuario u WHERE u.pessoa_id IS NULL AND u.igreja_id IS NOT NULL;
  UPDATE usuario u SET pessoa_id = p.id FROM pessoa p
    WHERE u.pessoa_id IS NULL AND p.igreja_id = u.igreja_id AND p.nome = u.username;
  ALTER TABLE usuario ALTER COLUMN pessoa_id SET NOT NULL;
  ALTER TABLE usuario ADD COLUMN telefone VARCHAR(30);
  CREATE UNIQUE INDEX ux_usuario_telefone ON usuario (telefone) WHERE telefone IS NOT NULL;
  ALTER TABLE usuario ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
  UPDATE usuario SET status = CASE WHEN ativo THEN 'ATIVO' ELSE 'DESATIVADO' END;
  ALTER TABLE usuario DROP COLUMN ativo;
  ALTER TABLE usuario ALTER COLUMN senha_hash DROP NOT NULL;
  ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL;
  ALTER TABLE usuario DROP COLUMN username;
  ALTER TABLE usuario ADD CONSTRAINT ck_usuario_identificador CHECK (email IS NOT NULL OR telefone IS NOT NULL);
  ```
  (Confira colunas reais de `pessoa` em V1/V2/V15 e ajuste o INSERT; se `gen_random_uuid()` não estiver disponível na versão do Postgres usada, use `uuid_generate_v4()` conforme V1.)

- [ ] **Step 1: Testes que falham**
  - `IdentificadorTest`: `emailNormalizaParaMinusculas` (`" Maria@Base.COM "` → `"maria@base.com"`); `telefoneEmFormatosDiferentesNormalizaIgual` (Review Focus 2: os três formatos do Review Focus dão o mesmo valor); `emailNaoViraTelefone`.
  - `CustomUserDetailsServiceTest`: `carregaPorEmail`, `carregaPorTelefoneFormatado`, `contaConvidadaNaoAutentica` e `contaDesativadaNaoAutentica` (→ `UsernameNotFoundException`/conta desabilitada, ambas viram 401 "Credenciais inválidas").
  - `JwtServiceTest.subEhIdDoUsuario`; `JwtAuthenticationFilterTest.carregaUsuarioPorIdDoToken` e `recusaUsuarioNaoAtivo`.
  - `AdminBootstrapTest.loginPeloEmail` (sem `ADMIN_USERNAME`).
- [ ] **Step 2:** `./mvnw test -Dtest='IdentificadorTest,CustomUserDetailsServiceTest,JwtServiceTest,JwtAuthenticationFilterTest,AdminBootstrapTest'` → FAIL.
- [ ] **Step 3:** Implementar migração, entidade, repositório e todas as trocas de `username`. Verificação: `grep -rn "username\|Username" src/main` só pode sobrar em `AdminBootstrap` (aviso de variável ignorada).
- [ ] **Step 4:** `./mvnw test` → PASS. Boot em banco descartável: `createdb igreja_db_verif_f2 && SPRING_PROFILES_ACTIVE=dev DB_NAME=igreja_db_verif_f2 SERVER_PORT=18089 ./mvnw spring-boot:run` sobe sem erro; `POST /api/auth/login {"identificador":"pastor@base.local","senha":"admin123"}` → 200; parar e `dropdb igreja_db_verif_f2`.
- [ ] **Step 5:** Commit `Troca username por login com e-mail ou telefone e status da conta`.

---

### Task 2: Tokens de acesso de uso único

**Files:**
- Create: `src/main/resources/db/migration/V20__token_acesso.sql`, `usuario/entity/{TokenAcesso,TipoTokenAcesso}.java`, `usuario/repository/TokenAcessoRepository.java`, `usuario/service/TokenAcessoService.java`, `common/exception/LinkExpiradoException.java`
- Modify: `common/exception/GlobalExceptionHandler.java` (410 `LINK_EXPIRADO`), `src/main/resources/application.yml` (`app.frontend.url-base`), `.env.example` (`FRONTEND_URL_BASE`)
- Test: `usuario/service/TokenAcessoServiceTest.java`, `common/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces:
  - `enum TipoTokenAcesso { CONVITE, REDEFINICAO }` com `Duration validade()` (7 dias / 1 hora).
  - `TokenAcesso` entity: `id`, `usuarioId`, `tipo`, `tokenHash`, `expiraEm` (Instant), `usadoEm`, `criadoPor` (UUID, nulo em autoatendimento), `createdAt`.
  - `TokenAcessoService.gerar(UUID usuarioId, TipoTokenAcesso tipo, UUID criadoPor): String` — apaga pendentes (não usados) do mesmo usuário e tipo, grava o hash, devolve o token bruto.
  - `TokenAcessoService.validar(String bruto): TokenAcesso` — desconhecido, expirado ou usado → `LinkExpiradoException("Este link expirou ou já foi usado. Peça um novo convite.")`.
  - `TokenAcessoService.consumir(TokenAcesso)`: marca `usadoEm` com update condicional (`UPDATE … SET usado_em = now() WHERE id = :id AND usado_em IS NULL`); 0 linhas → `LinkExpiradoException` (Review Focus 4).
  - `TokenAcessoService.revogarPendentes(UUID usuarioId)`: apaga todos os não usados do usuário.
  - `TokenAcessoService.link(String bruto): String` → `urlBase + "/acesso/" + bruto` (sem barra dupla).
- V20: tabela `token_acesso` com FK para `usuario(id) ON DELETE CASCADE`, `token_hash VARCHAR(64) UNIQUE NOT NULL`, índice em `(usuario_id, tipo)`.

- [ ] **Step 1: Testes que falham** — `gravaSoOHash` (o valor salvo ≠ bruto e tem 64 hex); `tokenTem43CaracteresBase64Url`; `conviteValeSeteDias` / `redefinicaoValeUmaHora` (Clock fixo injetado); `novoTokenInvalidaPendenteDoMesmoTipo`; `naoInvalidaTipoDiferente`; `expiradoLancaLinkExpirado`; `desconhecidoLancaLinkExpirado`; `consumirDuasVezesLancaLinkExpirado`; `linkSemBarraDupla` (url-base com `/` final); handler: `linkExpiradoVira410`.
- [ ] **Step 2:** `./mvnw test -Dtest='TokenAcessoServiceTest,GlobalExceptionHandlerTest'` → FAIL.
- [ ] **Step 3:** Implementar (injete `java.time.Clock` como bean se ainda não houver; senão crie um `@Bean Clock` em `config`).
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Adiciona tokens de acesso de uso único`.

---

### Task 3: Convite e redefinição gerados por quem administra

**Files:**
- Create: `usuario/service/AcessoService.java`, `usuario/dto/LinkAcessoResponse.java`, `common/exception/EntidadeNaoProcessavelException.java`
- Modify: `pessoa/controller/PessoaController.java` (+ `POST /{id}/convite`), `usuario/controller/UsuarioController.java` (+ `POST /{id}/redefinicao-senha`), `security/AutorizacaoService.java`, `common/exception/GlobalExceptionHandler.java` (422)
- Test: `usuario/service/AcessoServiceTest.java`, `security/AutorizacaoServiceTest.java`

**Interfaces:**
- Consumes: `TokenAcessoService` (Task 2), `UsuarioRepository.findByPessoaId/existsByTelefone/findByEmailIgnoreCase`, `Identificador` (Task 1), `CanalMensagem.enviar(EnvioMensagem)`, `AuditService.registrar`.
- Produces:
  - `record LinkAcessoResponse(String link, boolean enviadoWhatsapp)`.
  - `AutorizacaoService.assertPodeConvidar(Pessoa)`: `CONTAS_GERIR` na igreja da pessoa, ou a pessoa está no escopo de liderança do usuário (mesma regra de `assertPodeEditarContato`, sem o "a própria pessoa").
  - `AcessoService.convidar(UUID pessoaId): LinkAcessoResponse`:
    - sem e-mail e sem WhatsApp/telefone → `EntidadeNaoProcessavelException("Cadastre e-mail ou WhatsApp antes de convidar")` (422);
    - usuário existente `ATIVO` → `ConflictException("Esta pessoa já tem acesso ao app")`; `DESATIVADO` → `ConflictException("A conta está desativada. Reative antes de convidar")`; `CONVIDADO` → reenvia (novo token);
    - novo usuário `CONVIDADO`: `email` = e-mail da pessoa normalizado, `telefone` = `Identificador.telefone(whatsapp ?? telefone)`, `igrejaId` = da pessoa, sem senha, sem papéis; e-mail/telefone já de outra conta → `ConflictException("E-mail já usado por outra conta")` / `("Telefone já usado por outra conta")` antes de salvar (Review Focus 1);
    - token `CONVITE`; se houver telefone, envia pelo canal: `"Olá, {primeiroNome}! Você foi convidado para usar o app da {nomeIgreja}. Crie sua senha neste link (vale por 7 dias): {link}"`; exceção do canal → `enviadoWhatsapp=false` (loga, não propaga);
    - audita `CONVIDAR` com `pessoaId` e `usuarioId`.
  - `AcessoService.gerarRedefinicao(UUID usuarioId): LinkAcessoResponse`: exige `CONTAS_GERIR`; se o alvo tem PASTOR/SECRETARIA de sistema, exige `PAPEIS_GESTAO_ATRIBUIR` (reuse `assertPodeAlterarContaDeGestao` da Fase 1); só para `ATIVO` (senão `ConflictException("Só é possível redefinir a senha de uma conta ativa")`); mensagem `"Olá, {primeiroNome}! Para criar uma nova senha do app da {nomeIgreja}, use este link (vale por 1 hora): {link}"`; audita `GERAR_REDEFINICAO`.

- [ ] **Step 1: Testes que falham** (`AcessoServiceTest`, canal e repositórios mockados)
  - `conviteCriaUsuarioConvidadoSemSenhaEEnviaWhatsapp` — `status == CONVIDADO`, `senhaHash == null`, `enviadoWhatsapp == true`, mensagem contém o link.
  - `conviteComCanalFalhandoDevolveLinkComEnviadoFalse`.
  - `conviteSemContatoLanca422`.
  - `conviteComTelefoneDeOutraContaLanca409ENaoSalva` (Review Focus 1) — `verify(usuarioRepository, never()).save(any())`.
  - `reenvioParaConvidadoGeraNovoTokenSemNovoUsuario`.
  - `conviteParaAtivoLanca409` / `conviteParaDesativadoLanca409`.
  - `liderConvidaPessoaDoSeuMinisterio` / `liderNaoConvidaPessoaForaDoEscopo` (403).
  - `secretariaNaoGeraRedefinicaoDoPastor` (403) / `pastorGeraRedefinicaoDaSecretaria`.
  - `redefinicaoDeContaConvidadaLanca409`.
- [ ] **Step 2:** `./mvnw test -Dtest='AcessoServiceTest,AutorizacaoServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar service, endpoints e handler 422.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Permite convidar pessoas e gerar link de redefinição de senha`.

---

### Task 4: Endpoints públicos — validar link, definir senha, esqueci a senha

**Files:**
- Create: `usuario/dto/{TokenInfoResponse,DefinirSenhaRequest,EsqueciSenhaRequest}.java`
- Modify: `usuario/controller/AuthController.java`, `usuario/service/AcessoService.java`, `usuario/service/UsuarioService.java` (extrair `LoginResponse emitirLogin(Usuario)` reaproveitado por `autenticar`)
- Test: `usuario/service/AcessoServiceTest.java`, `usuario/controller/AuthControllerTest.java` (se não existir, testar pela camada de service + `LoginRateLimiterTest`)

**Interfaces:**
- Consumes: `TokenAcessoService.validar/consumir/revogarPendentes/gerar/link`, `LoginRateLimiter.assertPermitido/registrarFalha/limpar`, `CanalMensagem`.
- Produces (todos sob `/api/auth`, já liberado em `SecurityConfig`):
  - `GET /token/{token}` → `TokenInfoResponse(TipoTokenAcesso tipo, String primeiroNome)`; token inválido → 410 e `registrarFalha("token", ip)`.
  - `POST /definir-senha` `DefinirSenhaRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 100) String senha)` → `LoginResponse`: valida, consome, grava hash, `status = ATIVO` (só se `CONVIDADO` ou `ATIVO`; `DESATIVADO` → 410), `invalidarTokens()`, `revogarPendentes`, audita `DEFINIR_SENHA`, devolve login já autenticado.
  - `POST /esqueci-senha` `EsqueciSenhaRequest(@NotBlank String identificador)` → 204 sempre; com `assertPermitido("esqueci:" + identificador normalizado, ip)` e `registrarFalha` em toda chamada (limita tentativas); só envia se a conta existe, está `ATIVO` e tem telefone: token `REDEFINICAO`, mesma mensagem de `gerarRedefinicao`, `criadoPor = null`.
  - `AcessoService.definirSenha(DefinirSenhaRequest): LoginResponse`, `AcessoService.solicitarRedefinicao(String identificador): void`, `AcessoService.info(String token): TokenInfoResponse`.

- [ ] **Step 1: Testes que falham**
  - `definirSenhaAtivaContaConvidadaEDevolveLogin` — `status == ATIVO`, hash gravado, `tokenVersion` incrementado, `LoginResponse.token` não vazio.
  - `definirSenhaComTokenJaUsadoLanca410` (Review Focus 4).
  - `definirSenhaComSenhaCurtaFalhaValidacao` (7 caracteres → 400).
  - `redefinicaoInvalidaSessoesAnteriores` — `tokenVersion` muda.
  - `esqueciSenhaContaAtivaComTelefoneEnvia`.
  - `esqueciSenhaIdentificadorDesconhecidoNaoEnviaNada` / `…ContaConvidadaNaoEnvia` / `…ContaDesativadaNaoEnvia` (Review Focus 3) — `verify(canal, never()).enviar(any())`, sem exceção.
  - `esqueciSenhaRespeitaRateLimit` — após o limite, `TooManyRequestsException`.
  - `infoDevolvePrimeiroNomeETipo`.
- [ ] **Step 2:** `./mvnw test -Dtest=AcessoServiceTest` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Adiciona endpoints públicos de definir senha e esqueci a senha`.

---

### Task 5: Desativar/reativar e remoção dos caminhos antigos

**Files:**
- Modify: `usuario/controller/UsuarioController.java` (remove `POST /`, `PUT /{id}`, `DELETE /{id}`; adiciona `POST /{id}/desativar`, `POST /{id}/reativar`), `pessoa/controller/PessoaController.java` (remove `POST /{id}/usuario`), `usuario/service/UsuarioService.java`, `usuario/service/MeService.java`, `usuario/dto/MeUpdateRequest.java`, `pessoa/dto/PessoaResponse.java` (+ `StatusUsuario acessoStatus`), `pessoa/service/PessoaService.java`
- Delete: `usuario/dto/{UsuarioCreateRequest,PessoaUsuarioCreateRequest,UsuarioUpdateRequest}.java` e testes que só cobriam esses caminhos
- Test: `usuario/service/UsuarioServiceTest.java`, `usuario/service/MeServiceTest.java`, `pessoa/service/PessoaServiceTest.java`

**Interfaces:**
- Consumes: `TokenAcessoService.revogarPendentes`, guardas da Fase 1 (`assertPodeAlterarContaDeGestao`, `assertRestaPastorAtivoSemEle`).
- Produces:
  - `UsuarioService.desativar(UUID id): UsuarioResponse` — `CONTAS_GERIR` + guardas da Fase 1; `status = DESATIVADO`, `invalidarTokens()`, `revogarPendentes`; audita `DESATIVAR_USUARIO`. Já desativado → idempotente.
  - `UsuarioService.reativar(UUID id): UsuarioResponse` — mesmas permissões; `status = senhaHash == null ? CONVIDADO : ATIVO`; não cria token; audita `REATIVAR_USUARIO`.
  - Sem exclusão física de usuário (o `DELETE` sai).
  - `MeService.atualizarPerfil`: e-mail novo atualiza também `usuario.email` (409 "E-mail já usado por outra conta"); WhatsApp/telefone novo da própria pessoa atualiza `usuario.telefone` normalizado (409 se de outra conta); `novaSenha` com `@Size(min = 8, max = 100)` e `senhaAtual` obrigatória.
  - `PessoaResponse.acessoStatus`: status do usuário da pessoa ou `null` (calculado em lote junto com `funcoes`).
  - Fecha a pendência da Fase 1 "secretaria troca o e-mail do pastor": não existe mais endpoint de gestão que altere identificadores de login de outra conta.

- [ ] **Step 1: Testes que falham**
  - `desativarRevogaLinksEInvalidaSessao` (Review Focus 5) — `revogarPendentes` chamado, `tokenVersion` muda.
  - `reativarContaSemSenhaVoltaConvidada` / `reativarContaComSenhaVoltaAtiva` e `reativarNaoGeraToken`.
  - `secretariaNaoDesativaPastor` (403); `naoDesativaUltimoPastorAtivo` (400 "A igreja precisa de ao menos um pastor").
  - `meTrocaEmailAtualizaLogin` / `meEmailDeOutraContaLanca409`; `meSenhaCurtaFalha`.
  - `pessoaResponseTrazAcessoStatus` (sem N+1: uma consulta de usuários por lote).
- [ ] **Step 2:** `./mvnw test -Dtest='UsuarioServiceTest,MeServiceTest,PessoaServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar e remover os caminhos antigos; `grep -rn "UsuarioCreateRequest\|PessoaUsuarioCreateRequest\|UsuarioUpdateRequest\|criarAPartirDaPessoa" src/` vazio.
- [ ] **Step 4:** `./mvnw test` → PASS. Boot no banco descartável (como na Task 1): `POST /api/pessoas/{id}/convite` como pastor devolve `link`; `GET /api/auth/token/{token}` → 200; `POST /api/auth/definir-senha` → 200; login com o e-mail da pessoa → 200; depois `dropdb`.
- [ ] **Step 5:** Commit `Adiciona desativar/reativar conta e remove criação manual de usuário e senha`.

---

### Task 6 (Flutter): login por e-mail ou telefone e telas públicas de acesso

Repo: `../base_interface`.

**Files:**
- Create: `lib/screens/acesso/definir_senha_screen.dart`, `lib/screens/acesso/esqueci_senha_screen.dart`, `lib/services/acesso_service.dart`
- Modify: `lib/services/auth_service.dart` (`login(String identificador, String senha)` envia `{"identificador","senha"}`; `entrarComLogin(Map loginResponse)` reaproveitado após definir senha), `lib/providers/auth_provider.dart`, `lib/screens/login/login_screen.dart` (rótulo "E-mail ou telefone"; link "Esqueci minha senha"), `lib/router/app_router.dart` (rotas públicas `/acesso/:token` e `/esqueci-senha` fora do shell e liberadas no `redirect` mesmo sem sessão), `lib/utils/route_access.dart`, `lib/models/models.dart` (remover `username` de `AuthUser`/`UsuarioConta`)
- Test: `test/services/contrato_api_test.dart`, `test/utils/route_access_test.dart`, `test/screens/definir_senha_screen_test.dart`

**Interfaces:**
- Produces:
  - `AcessoService.info(String token): Future<({String tipo, String primeiroNome})>` → `GET /auth/token/{token}`.
  - `AcessoService.definirSenha(String token, String senha): Future<Map<String, dynamic>>` → `POST /auth/definir-senha`.
  - `AcessoService.esqueciSenha(String identificador): Future<void>` → `POST /auth/esqueci-senha`.
  - Tela de definir senha: carrega `info`; título "Olá, {primeiroNome}" e subtítulo "Crie sua senha" (CONVITE) ou "Crie uma nova senha" (REDEFINICAO); campos senha e confirmar (mín. 8, iguais); sucesso → entra no app e vai para `/dashboard`. 410 em qualquer chamada → mensagem "Este link expirou ou já foi usado. Peça um novo convite." sem o formulário.
  - Tela esqueci a senha: campo "E-mail ou telefone"; após enviar sempre mostra "Se houver uma conta com esse contato, enviamos um link pelo WhatsApp." (mesma mensagem em qualquer resposta 204).

- [ ] **Step 1: Testes que falham** — contrato: `loginEnviaIdentificador`, `infoFazGetToken`, `definirSenhaFazPost`, `esqueciSenhaFazPost`; rota: `acessoTokenEhPublico`, `esqueciSenhaEhPublico`; widget: `linkExpiradoMostraMensagemSemFormulario`, `senhasDiferentesNaoEnviam`.
- [ ] **Step 2:** `flutter test test/services/contrato_api_test.dart test/utils/route_access_test.dart test/screens` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `flutter test` → PASS; `flutter analyze` 0 erros.
- [ ] **Step 5:** Commit `Login por e-mail ou telefone e telas de definir senha e esqueci a senha`.

---

### Task 7 (Flutter): bloco "Acesso ao app" e fim da tela de usuários

**Files:**
- Create: `lib/widgets/acesso_app_card.dart`, `lib/widgets/link_acesso_dialog.dart`
- Modify: `lib/screens/pessoas/pessoa_detail_screen.dart` (substitui "Criar usuário" pelo card), `lib/services/usuario_service.dart` (remove `createFromPessoa`; adiciona `convidar(pessoaId)`, `gerarRedefinicao(usuarioId)`, `desativar(usuarioId)`, `reativar(usuarioId)`), `lib/models/models.dart` (`Pessoa.acessoStatus`, `UsuarioConta.status/telefone`), `lib/services/pessoa_service.dart`, `lib/screens/pessoas/pessoas_screen.dart` e `lib/screens/membros/membros_screen.dart` (filtro "Com acesso"), `lib/navigation/nav_config.dart` e `lib/router/app_router.dart` (remove `/usuarios`), `lib/widgets/form_dialogs.dart` (remove `UsuarioFormDialog`)
- Delete: `lib/screens/usuarios/usuarios_screen.dart`
- Test: `test/services/contrato_api_test.dart`, `test/widgets/acesso_app_card_test.dart`

**Interfaces:**
- Consumes: `Permissoes` (Fase 1), endpoints das Tasks 3 e 5.
- Produces:
  - `AcessoAppCard(pessoa, onAlterado)`: mostra o status ("Sem acesso", "Convite enviado", "Ativo", "Desativado") e ações conforme permissão — Convidar / Reenviar convite (`contasGerir` ou pessoa no escopo de liderança), Redefinir senha (`contasGerir`, só `ATIVO`), Desativar / Reativar (`contasGerir`), Papéis (diálogo da Fase 1).
  - `LinkAcessoDialog(link, enviadoWhatsapp)`: texto "Enviamos pelo WhatsApp." ou "Não foi possível enviar pelo WhatsApp. Copie o link e envie para a pessoa."; botão "Copiar link" (`Clipboard.setData`) com SnackBar "Link copiado".
  - Erros 409/422 do servidor aparecem em SnackBar com a mensagem do servidor.

- [ ] **Step 1: Testes que falham** — contrato: `convidarFazPost`, `gerarRedefinicaoFazPost`, `desativarFazPost`, `reativarFazPost`, `pessoaMapeiaAcessoStatus`; widget: `semAcessoMostraConvidar`, `convidadoMostraReenviar`, `ativoMostraRedefinirEDesativar`, `membroNaoVeAcoes`, `linkNaoEnviadoMostraCopiar`.
- [ ] **Step 2:** `flutter test test/services/contrato_api_test.dart test/widgets` → FAIL.
- [ ] **Step 3:** Implementar; `grep -rn "username\|UsuarioFormDialog\|createFromPessoa\|/usuarios'" lib` vazio (exceto `/usuarios/{id}/...` nos services).
- [ ] **Step 4:** `flutter test` → PASS; `flutter analyze` 0 erros.
- [ ] **Step 5:** Commit `Adiciona bloco de acesso ao app no detalhe da pessoa e remove tela de usuários`.

---

## Fora deste plano

- Fase 3 (filial com pastor e convite; remoção do admin provisório criado em `criarFilial`).
- Deep link nativo iOS/Android e envio de e-mail.
- Pendências da Fase 1 ainda abertas: `VoluntarioService.vincular` sem bloqueio de serviço fictício no servidor; derivação de líder ignora `ativo` do ministério/base/grupo; trocar os próprios papéis força novo login.
