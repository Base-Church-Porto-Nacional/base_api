# Fase 3 — Filial e Pastor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar filial passa a ser "nome + pastor": o pastor recebe o papel PASTOR na filial e um convite se ainda não tem conta; acaba o admin provisório; trocar o pastor de uma igreja vira uma ação explícita que move o papel.

**Architecture:** `IgrejaService.criarFilial` aceita um pastor existente (transferido para a filial com regras de segurança) ou uma pessoa nova, atribui `PASTOR` e reaproveita `AcessoService.convidar` (Fase 2) para o convite. A coluna `igreja.admin_usuario_id` sai. Um endpoint `PUT /api/igrejas/{id}/pastor` troca o titular e move o papel. O Flutter troca o diálogo de filial e ganha "Trocar pastor".

**Tech Stack:** Spring Boot 4.1 / Java 25, Flyway, Mockito (`base_api`); Flutter/Dart (`../base_interface`, branch `feat/papeis-capacidades`).

**Spec:** `docs/superpowers/specs/2026-11-02-papeis-e-acesso-design.md` (seção "Fase 3 — Filial"). Fases 1 e 2 já implementadas.

## Global Constraints

- Português; `/api/{recurso}`; regras nos services via `AutorizacaoService`; schema só por migração nova (próxima: `V23`).
- Criar filial: só PASTOR da matriz (`assertPodeCriarFilial`). Trocar pastor: pastor da matriz (cascata) ou o pastor titular da própria igreja.
- Cada pessoa pertence a uma igreja só; pessoa existente escolhida como pastor de filial é **transferida** (pessoa e usuário mudam `igrejaId`; papéis na igreja antiga removidos).
- Pastor sem conta recebe convite (Fase 2: `AcessoService.convidar`); a resposta traz `convite: { link, enviadoWhatsapp }` ou `null`.
- Nenhuma operação deixa filial criada pela metade: validações (contato, transferência) antes de salvar; tudo numa transação.
- Auditar `CRIAR_FILIAL`, `TRANSFERIR_PESSOA`, `TROCAR_PASTOR`.
- `./mvnw test`, `flutter test` verdes e `flutter analyze` 0 erros ao fim de cada task do respectivo repo. Nunca commitar `deploy/stack.yml` (tem alteração do usuário).

## Review Focus

1. Pessoa escolhida como pastor da filial lidera/coordena ministério, base ou grupo na igreja atual → 409 "Esta pessoa lidera ministérios ou grupos na igreja atual. Troque a liderança antes de transferir." e nada é criado (teste na Task 2).
2. Pastor titular da matriz escolhido como pastor da filial (inclusive ele mesmo) → 409 "Esta pessoa é o pastor titular da igreja atual." (teste na Task 2).
3. Pastor novo (ou existente sem conta) sem e-mail e sem WhatsApp → 422 antes de criar a filial (teste na Task 2).
4. Trocar o pastor pela mesma pessoa → 200 sem mudança nem auditoria (teste na Task 3).
5. Pastor de filial tentando criar filial ou trocar o pastor de outra igreja → 403 (teste na Task 3).

---

### Task 1: Remover o admin provisório e fixar o pastor titular da matriz

**Files:**
- Create: `src/main/resources/db/migration/V23__remove_admin_usuario_igreja.sql` (`ALTER TABLE igreja DROP COLUMN admin_usuario_id;`)
- Modify: `igreja/entity/Igreja.java`, `igreja/dto/IgrejaResponse.java` (remove `adminUsuarioId`, `adminNome`), `igreja/service/IgrejaService.java` (`toResponses`), `config/AdminBootstrap.java`, `config/DataInitializer.java`, `export/service/ExportService.java` (se exporta admin)
- Test: `config/AdminBootstrapTest.java`, `igreja/service/IgrejaServiceTest.java`

**Interfaces:**
- Produces: `IgrejaResponse(id, nome, tipo, igrejaMatrizId, pastorPessoaId, pastorNome, ativo, createdAt, updatedAt)`.
- `AdminBootstrap`: se a matriz não tem `pastorPessoaId`, define a pessoa do primeiro usuário como pastor titular.

- [ ] **Step 1: Testes que falham** — `bootstrapDefinePastorTitularDaMatriz`; `bootstrapNaoSobrescrevePastorExistente`; `respostaDaIgrejaNaoTemAdmin` (compilação/estrutura).
- [ ] **Step 2:** `./mvnw test -Dtest='AdminBootstrapTest,IgrejaServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar. `criarFilial` continua compilando provisoriamente sem `setAdminUsuarioId` (é reescrito na Task 2).
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Remove o admin provisório da igreja e fixa o pastor titular da matriz no bootstrap`.

---

### Task 2: Criar filial com pastor e convite

**Files:**
- Create: `igreja/dto/PastorFilialRequest.java`, `igreja/dto/FilialCriadaResponse.java`, `igreja/service/TransferenciaPessoaService.java`
- Modify: `igreja/dto/IgrejaFilialCreateRequest.java`, `igreja/service/IgrejaService.java` (`criarFilial`), `igreja/controller/IgrejaController.java`
- Test: `igreja/service/IgrejaServiceTest.java`, `igreja/service/TransferenciaPessoaServiceTest.java`

**Interfaces:**
- Consumes: `AcessoService.convidar(UUID pessoaId): LinkAcessoResponse` (Fase 2), `PermissaoService`/repositórios de ministério, base e pequeno grupo, `UsuarioRepository.findByPessoaId`, `UsuarioPapelRepository`, `PessoaServicoRepository`.
- Produces:
  - `record PastorFilialRequest(UUID pessoaId, @Size(max=150) String nome, @Email String email, @Size(max=30) String whatsapp)` — exatamente um dos modos: `pessoaId` **ou** `nome` (com e-mail ou WhatsApp); senão 400 "Informe uma pessoa existente ou os dados de uma nova pessoa".
  - `record IgrejaFilialCreateRequest(@NotBlank @Size(max=150) String nome, @NotNull @Valid PastorFilialRequest pastor)`.
  - `record FilialCriadaResponse(IgrejaResponse igreja, LinkAcessoResponse convite)` (`convite` nulo quando o pastor já tem conta).
  - `TransferenciaPessoaService.transferir(Pessoa pessoa, UUID igrejaDestinoId): void` — valida e move:
    - pessoa precisa estar numa igreja visível ao usuário (`assertAcesso`);
    - é `pastorPessoaId` de alguma igreja → 409 "Esta pessoa é o pastor titular da igreja atual." (Review Focus 2);
    - lidera/coordena ministério ou base, ou lidera pequeno grupo na igreja atual → 409 (texto do Review Focus 1);
    - move `pessoa.igrejaId`; se tem usuário: move `usuario.igrejaId`, apaga os `usuario_papel` da igreja antiga e incrementa `tokenVersion`;
    - desativa (`ativo=false`) os `pessoa_servico` de serviços da igreja antiga;
    - audita `TRANSFERIR_PESSOA` com origem e destino.
  - `IgrejaService.criarFilial(UUID matrizId, IgrejaFilialCreateRequest): FilialCriadaResponse`, em uma transação:
    1. `assertPodeCriarFilial`, matriz válida;
    2. resolve o pastor: existente (`pessoaId`) ou nova (sem salvar ainda); se o pastor não tem conta e não tem e-mail nem WhatsApp → `EntidadeNaoProcessavelException("Cadastre e-mail ou WhatsApp antes de convidar")` (Review Focus 3); se tem conta `DESATIVADO` → 409 "A conta desta pessoa está desativada. Reative antes de torná-la pastor.";
    3. valida a transferência (pessoa existente) **antes** de salvar a filial;
    4. salva a filial com `pastorPessoaId`; salva/transfere a pessoa (nova: cria `Pessoa` + jornada inicial na filial);
    5. se tem conta: atribui `PASTOR` de sistema na filial; se não: `acessoService.convidar(pessoaId)` e depois atribui `PASTOR` ao usuário criado;
    6. audita `CRIAR_FILIAL`.
  - Controller: `POST /api/igrejas/{matrizId}/filiais` → 201 `FilialCriadaResponse`.

- [ ] **Step 1: Testes que falham**
  - `criaFilialComPessoaNovaEConvite` — filial salva, pessoa na filial, usuário `CONVIDADO` com `PASTOR` na filial, `convite.link` presente.
  - `criaFilialComPessoaExistenteComContaSemConvite` — `convite == null`, `usuario.igrejaId == filialId`, papéis da igreja antiga removidos, `tokenVersion` incrementado.
  - `pessoaQueLideraMinisterioNaoETransferida` (Review Focus 1) — 409 e `igrejaRepository.save` nunca chamado.
  - `pastorTitularDaMatrizNaoETransferido` (Review Focus 2).
  - `pastorSemContatoLanca422SemCriarFilial` (Review Focus 3).
  - `pastorComContaDesativadaLanca409`.
  - `transferenciaDesativaVinculosDeServicoDaIgrejaAntiga`.
  - `pedidoComPessoaIdENomeLanca400`.
  - `pastorDeFilialNaoCriaFilial` (403).
- [ ] **Step 2:** `./mvnw test -Dtest='IgrejaServiceTest,TransferenciaPessoaServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Cria filial com pastor e convite, transferindo a pessoa com segurança`.

---

### Task 3: Trocar o pastor de uma igreja

**Files:**
- Create: `igreja/dto/TrocarPastorRequest.java`
- Modify: `igreja/dto/IgrejaUpdateRequest.java` (remove `pastorPessoaId`), `igreja/service/IgrejaService.java` (`atualizar` sem troca de pastor; novo `trocarPastor`), `igreja/controller/IgrejaController.java`, `security/AutorizacaoService.java`
- Test: `igreja/service/IgrejaServiceTest.java`, `security/AutorizacaoServiceTest.java`

**Interfaces:**
- Consumes: `AcessoService.convidar`, `UsuarioService` (atribuição de papéis/guardas da Fase 1), `Papeis.PASTOR`.
- Produces:
  - `record TrocarPastorRequest(@NotNull UUID pessoaId)`.
  - `AutorizacaoService.assertPodeTrocarPastor(Igreja igreja)`: `assertAcesso(igreja.id)` e (a igreja atual do usuário é a matriz da rede e ele tem `FILIAL_CRIAR`) **ou** (`igreja.pastorPessoaId` == pessoa do usuário); senão 403 "Somente o pastor da matriz ou o pastor titular podem trocar o pastor".
  - `IgrejaService.trocarPastor(UUID igrejaId, TrocarPastorRequest): FilialCriadaResponse` (mesmo formato: igreja + convite opcional):
    - mesma pessoa → retorna sem mudança e sem auditoria (Review Focus 4);
    - a pessoa precisa pertencer à igreja (`BusinessException("A pessoa não pertence a esta igreja")`);
    - sem conta → convite (mesmas regras de contato/422 da Task 2); conta `DESATIVADO` → 409 como na Task 2;
    - novo recebe `PASTOR` na igreja; o anterior (se tem conta) perde `PASTOR` naquela igreja, mantendo os demais papéis;
    - `igreja.pastorPessoaId = novo`; audita `TROCAR_PASTOR` com anterior e novo.
  - Controller: `PUT /api/igrejas/{id}/pastor` → `FilialCriadaResponse`.
  - `PUT /api/igrejas/{id}` passa a alterar só `nome` e `ativo`.

- [ ] **Step 1: Testes que falham** — `pastorDaMatrizTrocaPastorDaFilial`; `pastorTitularTrocaOProprio`; `pastorDeFilialNaoTrocaPastorDeOutraFilial` (403, Review Focus 5); `secretariaNaoTrocaPastor` (403); `trocarPelaMesmaPessoaNaoMudaNada` (Review Focus 4); `novoSemContaRecebeConvite`; `anteriorPerdePastorMasMantemSecretaria`; `pessoaDeOutraIgrejaLanca400`.
- [ ] **Step 2:** `./mvnw test -Dtest='IgrejaServiceTest,AutorizacaoServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS. Boot em banco descartável (`createdb igreja_db_verif_f3`, perfil dev, `SERVER_PORT=18089`): criar filial com pessoa nova → 201 com `convite.link`; trocar pastor → 200; `dropdb`.
- [ ] **Step 5:** Commit `Adiciona troca explícita do pastor da igreja`.

---

### Task 4: Endurecimentos pendentes das Fases 1–2

**Files:**
- Modify: `pessoa/service/PessoaService.java` (`sincronizarLoginDaConta`), `docker-compose.yml`
- Test: `pessoa/service/PessoaServiceTest.java`

**Interfaces:**
- `sincronizarLoginDaConta` passa a sincronizar **só** o identificador cujo valor na pessoa mudou nesta requisição (compare antes/depois do update): e-mail mudou → sincroniza e-mail; WhatsApp/telefone mudou → sincroniza telefone. Assim um número plantado antes por um líder não vai para o login num save posterior da secretaria.
- `docker-compose.yml`: repassar `FRONTEND_URL_BASE` ao serviço da API e documentar que o stack local completo precisa dele com `https://…` ou `SPRING_PROFILES_ACTIVE=dev`.

- [ ] **Step 1: Testes que falham** — `saveDaSecretariaSemMudarTelefoneNaoSincronizaNumeroPlantado`; `secretariaMudandoTelefoneSincroniza` (já deve passar; manter).
- [ ] **Step 2:** `./mvnw test -Dtest=PessoaServiceTest` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Sincroniza o login só com o contato alterado e repassa FRONTEND_URL_BASE no compose`.

---

### Task 5 (Flutter): nova criação de filial e troca de pastor

Repo: `../base_interface`.

**Files:**
- Modify: `lib/widgets/form_dialogs.dart` (diálogo de filial), `lib/services/igreja_service.dart`, `lib/screens/igrejas/igrejas_screen.dart`, `lib/screens/igrejas/igreja_detail_screen.dart`, `lib/models/models.dart` (`Igreja` sem `adminNome`/`adminUsuarioId`)
- Test: `test/services/contrato_api_test.dart`, `test/widgets/filial_form_dialog_test.dart`, `test/screens/igreja_detail_screen_test.dart`

**Interfaces:**
- Consumes: `LinkAcessoDialog(link, enviadoWhatsapp)` (Fase 2), `Permissoes` (Fase 1).
- Produces:
  - `IgrejaService.criarFilial({required String matrizId, required String nome, String? pastorPessoaId, String? pastorNome, String? pastorEmail, String? pastorWhatsapp}): Future<({Igreja igreja, ({String link, bool enviadoWhatsapp})? convite})>` → `POST /igrejas/{matrizId}/filiais` com `{"nome", "pastor": {...}}`.
  - `IgrejaService.trocarPastor(String igrejaId, String pessoaId)` → `PUT /igrejas/{id}/pastor`, mesmo retorno.
  - Diálogo de filial: campo nome; escolha "Pessoa já cadastrada" (seletor com as pessoas da rede, como hoje) ou "Nova pessoa" (nome, e-mail, WhatsApp; exige e-mail ou WhatsApp). Sem campos de admin/senha. Após criar, se houver convite, abre `LinkAcessoDialog`.
  - Detalhe da igreja: mostra o pastor; ação "Trocar pastor" visível se `filialCriar` (usuário da matriz) ou se o usuário é o pastor titular; seletor de pessoas da própria igreja; convite → `LinkAcessoDialog`. Remove "Admin" da lista e do detalhe.
  - Erros 409/422/403 do servidor em SnackBar.

- [ ] **Step 1: Testes que falham** — contrato: `criarFilialComPessoaExistente`, `criarFilialComPessoaNova`, `trocarPastorFazPut`; widget: `novaPessoaExigeEmailOuWhatsapp`, `semCamposDeAdmin`, `trocarPastorSoParaQuemPode`.
- [ ] **Step 2:** `flutter test test/services/contrato_api_test.dart test/widgets test/screens` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `flutter test` → PASS; `flutter analyze` 0 erros.
- [ ] **Step 5:** Commit `Cria filial escolhendo o pastor e adiciona troca de pastor`.

---

## Fora deste plano

- Uma pessoa em várias igrejas (decidido: não).
- Excluir filial.
- Pendências antigas: `VoluntarioService.vincular` sem bloqueio de serviço fictício no servidor; líder derivado ignora `ativo` do ministério/base/grupo; trocar os próprios papéis força novo login.
