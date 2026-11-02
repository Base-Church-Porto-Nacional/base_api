# Fase 1 — Papéis e Capacidades Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trocar os papéis marcados à mão (ADMIN/LIDER/VOLUNTARIO/MEMBRO) por PASTOR/SECRETARIA + funções derivadas, com uma fonte única de permissões no servidor exposta ao app.

**Architecture:** Um `PermissaoService` calcula capacidades (dos papéis atribuídos, com cascata matriz→filial) e escopos derivados (ministérios/bases/grupos liderados, voluntário). `AutorizacaoService` continua sendo a API dos services, reescrita por cima dele. `/api/me` devolve as permissões; o JWT deixa de carregar papéis. O Flutter troca o mapa local `_rolePermissions` pelas permissões do servidor.

**Tech Stack:** Spring Boot 4.1 / Java 25, Flyway, Mockito (backend `base_api`); Flutter/Dart, provider, go_router (app `../base_interface`).

**Spec:** `docs/superpowers/specs/2026-11-02-papeis-e-acesso-design.md` (seções "Matriz de responsabilidades" e "Fase 1").

## Global Constraints

- Código, nomes, mensagens e paths em português; endpoints `/api/{recurso}` sem versão.
- Schema só via nova migração Flyway (`V18__…`); nunca editar migração aplicada.
- Regras de autorização ficam nos services via `AutorizacaoService`; controllers finos.
- Papéis de sistema: só `PASTOR` e `SECRETARIA`. Reservados (não podem ser papel customizado): `ADMIN`, `LIDER`, `VOLUNTARIO`, `MEMBRO`, `COORDENADOR`, `SUPERVISOR`, `PREGADOR`, `PASTOR`, `SECRETARIA`.
- Exclusivo de PASTOR: editar conteúdo do culto (texto e louvor por override), atribuir/remover PASTOR/SECRETARIA, criar filial. Todo o resto de gestão: PASTOR e SECRETARIA (inclusive escala de supervisor/pregador).
- Pastor/secretaria da matriz têm os mesmos poderes nas filiais (cascata).
- Contrato da API pode quebrar (sem produção); backend e Flutter mudam juntos.
- Backend: `./mvnw test` verde ao fim de cada task. Flutter: `flutter test` e `flutter analyze` verdes ao fim das tasks do app.

## Review Focus

1. Usuário cujo único papel era `LIDER` antes da migração → continua logando, vira membro de base e mantém poderes de líder pelo ministério que lidera (teste em Task 2).
2. Chamadas de `AutorizacaoService` fora de requisição HTTP (schedulers `EscalaAutomacaoService`/`CultoAutomacaoService`) → não podem quebrar pelo cache por requisição (teste em Task 2).
3. Líder tentando alterar `ativo`/`observacoes` ou editar pessoa cujo vínculo com seu ministério está inativo → 403, não alteração silenciosa (teste em Task 4).
4. Pastor removendo o próprio `PASTOR` sendo o último pastor da igreja → recusado com 422 "A igreja precisa de ao menos um pastor" (teste em Task 5).
5. Secretaria da matriz agindo numa filial (cascata) e secretaria da filial tentando agir na matriz → permitido / 403 (teste em Task 2).

---

### Task 1: Migração de papéis e constantes

**Files:**
- Create: `src/main/resources/db/migration/V18__papeis_pastor_secretaria.sql`
- Create: `src/main/java/com/example/base_api/security/Papeis.java`
- Modify: `igreja/service/IgrejaContextoService.java` (cascata e `isPastorOuAdmin`), `usuario/service/PapelService.java:22`, `config/AdminBootstrap.java:40`, `config/DataInitializer.java:100-104` e usos, `igreja/service/IgrejaService.java:102`
- Test: `src/test/.../igreja/service/IgrejaContextoServiceTest.java`, `usuario/service/PapelServiceTest.java`, `config/AdminBootstrapTest.java`

**Interfaces:**
- Produces: `Papeis.PASTOR = "PASTOR"`, `Papeis.SECRETARIA = "SECRETARIA"`, `Papeis.GESTAO: Set<String>` (os dois), `Papeis.RESERVADOS: Set<String>` (lista dos Global Constraints). `IgrejaContextoService.isGestao(UUID igrejaId): boolean` e `assertGestao(UUID)` / `assertMatrizGestao()` (renomeiam `isPastorOuAdmin`/`assertPastorOuAdmin`/`assertMatrizPastorOuAdmin`; mensagem `"Somente pastor ou secretaria desta igreja"`). `papeisNaIgreja` passa a herdar da matriz `Papeis.GESTAO`.

- [ ] **Step 1: Testes que falham**
  - `IgrejaContextoServiceTest.secretariaDaMatrizEhGestaoNaFilial` — usuário com `SECRETARIA` na matriz → `isGestao(filialId)` true.
  - `IgrejaContextoServiceTest.secretariaDaFilialNaoAcessaMatriz` — `assertAcesso(matrizId)` lança `AccessDeniedException`.
  - `PapelServiceTest.recusaNomesReservados` — parametrizado com cada nome de `Papeis.RESERVADOS` → `BusinessException`.
  - `AdminBootstrapTest`: o primeiro usuário recebe `PASTOR` e `SECRETARIA` (ajustar asserção existente de `ADMIN`).
- [ ] **Step 2:** `./mvnw test -Dtest='IgrejaContextoServiceTest,PapelServiceTest,AdminBootstrapTest'` → FAIL.
- [ ] **Step 3: Migração V18**
  ```sql
  UPDATE papel SET nome = 'SECRETARIA' WHERE nome = 'ADMIN' AND igreja_id IS NULL;
  DELETE FROM usuario_papel WHERE papel_id IN
    (SELECT id FROM papel WHERE igreja_id IS NULL AND nome IN ('LIDER','VOLUNTARIO','MEMBRO'));
  DELETE FROM papel WHERE igreja_id IS NULL AND nome IN ('LIDER','VOLUNTARIO','MEMBRO');
  ```
- [ ] **Step 4:** Criar `Papeis`; renomear os métodos de gestão em `IgrejaContextoService` e **em todos os chamadores** (refactor mecânico — `grep -rn "PastorOuAdmin"` deve ficar vazio no backend); `AutorizacaoService` mantém por ora delegações com os novos nomes (`isGestao`, `assertGestao`, `assertMatrizGestao`). `PapelService` usa `Papeis.RESERVADOS`. `AdminBootstrap.PAPEIS_ADMIN = List.of(PASTOR, SECRETARIA)`. `IgrejaService.criarFilial` busca `SECRETARIA` em vez de `ADMIN` (substituído na Fase 3). `DataInitializer`: só cria vínculos `PASTOR`/`SECRETARIA`; usuários seed que eram líder/voluntário/membro ficam sem papel (a função vem dos ministérios já semeados).
- [ ] **Step 5:** `./mvnw test` → PASS (ajuste testes existentes que montavam `ADMIN`/`LIDER` para `SECRETARIA`/sem papel).
- [ ] **Step 6:** Commit `Substitui ADMIN/LIDER/VOLUNTARIO/MEMBRO por PASTOR e SECRETARIA`.

---

### Task 2: `Capacidade`, `Permissoes` e `PermissaoService`

**Files:**
- Create: `security/Capacidade.java`, `security/Permissoes.java`, `security/PermissaoService.java`
- Test: `src/test/.../security/PermissaoServiceTest.java`

**Interfaces:**
- Consumes: `IgrejaContextoService.requireUsuario()`, `papeisNaIgreja(UUID)`; `Papeis`.
- Produces:
  - `enum Capacidade { PESSOAS_VER_TODAS, PESSOAS_GERIR, DADOS_PASTORAIS_EDITAR, CONTAS_GERIR, MINISTERIOS_GERIR, ESCALAS_GERIR, CULTO_CONTEUDO_EDITAR, CULTO_CONTAGEM, PAPEIS_GESTAO_ATRIBUIR, PAPEIS_CUSTOM_GERIR, EXPORTAR, CONFIGURAR, RELATORIOS_VER, FILIAL_CRIAR }`
  - `static Set<Capacidade> Capacidade.de(Set<String> papeis, boolean papelComContagem)` — `PASTOR` → todas; `SECRETARIA` → todas menos `CULTO_CONTEUDO_EDITAR`, `PAPEIS_GESTAO_ATRIBUIR`, `FILIAL_CRIAR`; `papelComContagem` → `CULTO_CONTAGEM`.
  - `record Permissoes(UUID igrejaId, Set<String> papeis, Set<Capacidade> capacidades, Set<UUID> ministeriosLiderados, Set<UUID> basesLideradas, Set<UUID> gruposLiderados, boolean ehVoluntario)` com `boolean tem(Capacidade c)`.
  - `PermissaoService.doUsuarioAtual(): Permissoes` (igreja do usuário).
  - `PermissaoService.capacidadesEm(UUID igrejaId): Set<Capacidade>` (com cascata; papel customizado só conta na própria igreja).
  - `PermissaoService.ministeriosLideradosEm(UUID igrejaId): Set<UUID>` — ministérios onde a pessoa é líder ou coordenador **mais** todos os ministérios das bases que lidera/coordena.
- Cache: resultados guardados em `RequestContextHolder.getRequestAttributes()` (atributo por chave `"permissoes:" + igrejaId`) quando houver requisição; sem requisição, calcula sem cache.

- [ ] **Step 1: Testes que falham** (`PermissaoServiceTest`, repositórios mockados)
  - `pastorTemTodasAsCapacidades`
  - `secretariaNaoTemExclusivasDoPastor` — não contém `CULTO_CONTEUDO_EDITAR`, `PAPEIS_GESTAO_ATRIBUIR`, `FILIAL_CRIAR`; contém `ESCALAS_GERIR`.
  - `papelCustomComContagemDaCultoContagem`
  - `semPapelLiderDeMinisterioTemEscopoMasNenhumaCapacidade` (Review Focus 1) — `capacidades` vazio, `ministeriosLiderados` contém o ministério.
  - `liderDeBaseHerdaMinisteriosDaBase`
  - `liderDeGrupoAparecemEmGruposLiderados`
  - `voluntarioAtivoEhVoluntario` / `vinculoInativoNaoEhVoluntario`
  - `pastorDaMatrizTemCapacidadesNaFilial` (Review Focus 5)
  - `funcionaSemRequisicaoHttp` (Review Focus 2) — `RequestContextHolder.resetRequestAttributes()` e chama `capacidadesEm` sem exceção.
  - `cacheiaPorRequisicao` — com `RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()))`, duas chamadas → `ministerioRepository` consultado uma vez.
- [ ] **Step 2:** `./mvnw test -Dtest=PermissaoServiceTest` → FAIL (classes não existem).
- [ ] **Step 3:** Implementar as três classes. Escopos via `MinisterioRepository`, `BaseMinisterialRepository`, `SmallGroupRepository`, `PessoaServicoRepository` (adicionar queries derivadas por `igrejaId` + `liderPessoaId`/`coordenadorPessoaId` se faltarem; não carregar todos os ministérios da igreja e filtrar em memória).
- [ ] **Step 4:** `./mvnw test -Dtest=PermissaoServiceTest` → PASS.
- [ ] **Step 5:** Commit `Adiciona PermissaoService com capacidades e funções derivadas`.

---

### Task 3: `AutorizacaoService` sobre capacidades

**Files:**
- Modify: `security/AutorizacaoService.java`; chamadores de métodos renomeados (`aniversario`, `cronograma`, `culto`, `escala`, `export`, `jornada`, `dashboard`, `igreja`, `usuario`, `ministerio`, `baseministerial`, `smallgroup`)
- Test: `src/test/.../security/AutorizacaoServiceTest.java`

**Interfaces:**
- Consumes: `PermissaoService.capacidadesEm`, `ministeriosLideradosEm`; `Capacidade`.
- Produces (assinaturas novas/alteradas):
  - `boolean tem(UUID igrejaId, Capacidade c)` e `void assertTem(UUID igrejaId, Capacidade c)` (faz `assertAcesso` antes; 403 `"Sem permissão para esta ação"`).
  - `isGestao(UUID)` passa a ser `tem(igrejaId, PESSOAS_VER_TODAS)`.
  - `podeEditarTexto(igrejaId, pregadorPessoaId)` → `CULTO_CONTEUDO_EDITAR` ou pregador.
  - `podeEditarLouvor(igrejaId)` → `CULTO_CONTEUDO_EDITAR` ou líder/coordenador de ministério `MUSICA` (via `ministeriosLideradosEm`).
  - `assertPodeVerConteudo` → `tem(PESSOAS_VER_TODAS)` (secretaria vê), texto, louvor ou mídia.
  - `assertPodeAtribuirPapeisGestao(UUID igrejaAlvoId)` → `PAPEIS_GESTAO_ATRIBUIR` na igreja alvo (substitui `assertPodePromoverGestao`).
  - `assertPodeCriarFilial()` → igreja atual é matriz e `FILIAL_CRIAR` (substitui `assertMatrizPastorOuAdmin` no `IgrejaService.criarFilial`).
  - `assertPodeCriarMinisterio/BaseMinisterial/SmallGroup` → `MINISTERIOS_GERIR`; `assertPodeLancarContagem` → `CULTO_CONTAGEM`; `assertPodeCriarEscala` → `ESCALAS_GERIR` libera todos os tipos.
  - Gerir ministério/base/grupo e visibilidade de pessoas passam a usar `ministeriosLideradosEm` (remove `pessoasDosMeusMinisterios` duplicado com o PermissaoService onde possível).
  - Remove `PAPEIS_GESTAO`, `PAPEIS_ATALHO`, `validarPapeisAtalho`.

- [ ] **Step 1: Testes que falham** (adicionar/ajustar em `AutorizacaoServiceTest`, mockando `PermissaoService`)
  - `secretariaPodeCriarEscalaDePregador`
  - `secretariaNaoEditaTextoDoCulto` / `pastorEditaTextoDoCulto` / `pregadorEditaProprioTexto`
  - `secretariaVeConteudoDoCulto`
  - `secretariaNaoAtribuiPapeisDeGestao`
  - `secretariaNaoCriaFilial` / `pastorDaFilialNaoCriaFilial`
  - `contadorLancaContagem`
- [ ] **Step 2:** `./mvnw test -Dtest=AutorizacaoServiceTest` → FAIL.
- [ ] **Step 3:** Reescrever conforme Interfaces; atualizar chamadores.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Reescreve AutorizacaoService sobre capacidades`.

---

### Task 4: Líder cadastra e edita o próprio pessoal

**Files:**
- Modify: `pessoa/dto/PessoaCreateRequest.java` (+ `UUID servicoId`), `pessoa/service/PessoaService.java`, `pessoa/controller/PessoaController.java`, `security/AutorizacaoService.java`, `pessoa/service/PessoaHistoricoService.java`, `pessoa/service/PessoaPausaService.java`, `jornada/service/JornadaService.java`
- Create: `pessoa/dto/PessoaResumoResponse.java` (`record(UUID id, String nome)`)
- Test: `src/test/.../pessoa/service/PessoaServiceTest.java`, `security/AutorizacaoServiceTest.java`

**Interfaces:**
- Consumes: `AutorizacaoService.tem`, `PermissaoService.ministeriosLideradosEm`.
- Produces:
  - `AutorizacaoService.assertPodeCadastrarPessoa(UUID igrejaId, UUID servicoId)` — `PESSOAS_GERIR` passa sem serviço; senão `servicoId` obrigatório e do ministério no escopo do líder.
  - `AutorizacaoService.assertPodeEditarContato(Pessoa)` — `PESSOAS_GERIR`, ou a pessoa está em `filtrarVisiveisNaListagem` via escopo de liderança (vínculo **ativo**), ou é a própria pessoa.
  - `AutorizacaoService.assertPodeEditarDadosPastorais(UUID igrejaId)` — `DADOS_PASTORAIS_EDITAR` (substitui `assertPodeMutarPessoas` em histórico, pausa, jornada).
  - `PessoaService.buscarPorNome(String termo): List<PessoaResumoResponse>` — `GET /api/pessoas/busca?nome=`; mín. 2 caracteres (`BusinessException` senão), máx. 20 resultados, igrejas de `idsParaListagem(null)`; permitido a gestão ou a quem lidera algo (`ministeriosLideradosEm` não vazio ou grupos/bases), senão 403.
- Regras em `PessoaService`:
  - `criar`: `assertPodeCadastrarPessoa`; com `servicoId`, cria também `PessoaServico` ativo (via `PessoaServicoRepository`, na mesma transação).
  - `atualizar`: sem `DADOS_PASTORAIS_EDITAR`, se `ativo` ou `observacoes` do request diferirem do atual → `AccessDeniedException("Somente pastor ou secretaria alteram dados pastorais")`; demais campos exigem `assertPodeEditarContato`.
  - `excluir`: `assertTem(igrejaId, PESSOAS_GERIR)`.

- [ ] **Step 1: Testes que falham** (`PessoaServiceTest`)
  - `liderCadastraPessoaNoServicoDoSeuMinisterio` — salva pessoa e `PessoaServico` ativo.
  - `liderSemServicoNaoCadastra` → 403; `liderEmServicoDeOutroMinisterio` → 403.
  - `liderEditaContatoDoSeuVoluntario`
  - `liderNaoAlteraObservacoes` / `liderNaoDesativaPessoa` (Review Focus 3) → 403.
  - `liderNaoEditaPessoaComVinculoInativo` (Review Focus 3) → 403.
  - `liderNaoExcluiPessoa` → 403.
  - `buscaPorNomeExigeDoisCaracteres`, `buscaPorNomeLimitaVinteResultados`, `membroSemLiderancaNaoBusca` → 403.
- [ ] **Step 2:** `./mvnw test -Dtest=PessoaServiceTest` → FAIL.
- [ ] **Step 3:** Implementar regras, endpoint e query `PessoaRepository.findTop20ByIgrejaIdInAndNomeContainingIgnoreCaseOrderByNomeAsc`.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Permite ao líder cadastrar e editar o contato do próprio pessoal`.

---

### Task 5: Atribuição de papéis por endpoint único

**Files:**
- Create: `usuario/dto/UsuarioPapeisRequest.java` (`record(@NotNull Set<String> papeis)`)
- Modify: `usuario/controller/UsuarioController.java` (remove `POST /{id}/gestao`, adiciona `PUT /{id}/papeis`), `usuario/service/UsuarioService.java`, `usuario/dto/UsuarioCreateRequest.java`, `PessoaUsuarioCreateRequest.java`, `UsuarioUpdateRequest.java` (remove `papeis`), `usuario/dto/UsuarioGestaoRequest.java` (apagar)
- Test: `src/test/.../usuario/service/UsuarioServiceTest.java`

**Interfaces:**
- Produces: `UsuarioService.alterarPapeis(UUID usuarioId, UsuarioPapeisRequest): UsuarioResponse`. Regras:
  - Nomes normalizados (trim/upper). Cada nome é `PASTOR`, `SECRETARIA` ou papel customizado da igreja do usuário; senão `BusinessException("Papel inválido: X")`.
  - Se o conjunto adiciona ou remove `PASTOR`/`SECRETARIA` → `assertPodeAtribuirPapeisGestao(igreja)`; demais mudanças → `assertTem(igreja, PAPEIS_CUSTOM_GERIR)`.
  - Remover o último `PASTOR` da igreja → `BusinessException("A igreja precisa de ao menos um pastor")` (Review Focus 4).
  - Audita `ALTERAR_PAPEIS` com antes/depois.
- Criação de usuário (endpoints atuais, substituídos na Fase 2) passa a nascer sem papéis.

- [ ] **Step 1: Testes que falham**
  - `secretariaAtribuiPapelCustomizado`
  - `secretariaNaoAtribuiSecretaria` → 403
  - `pastorAtribuiSecretaria` — audita `ALTERAR_PAPEIS`
  - `ultimoPastorNaoPodeSerRemovido` → `BusinessException`
  - `papelDeOutraIgrejaEhInvalido`
  - `usuarioCriadoSemPapeis`
- [ ] **Step 2:** `./mvnw test -Dtest=UsuarioServiceTest` → FAIL.
- [ ] **Step 3:** Implementar; `UsuarioRepository` ganha `countByPapelAndIgrejaId(String nome, UUID igrejaId)` para a regra do último pastor.
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Unifica atribuição de papéis em PUT /api/usuarios/{id}/papeis`.

---

### Task 6: Líderes e dashboard derivados da função

**Files:**
- Modify: `usuario/service/LiderService.java`, `dashboard/service/DashboardService.java:70`, `usuario/repository/UsuarioRepository.java` (remove `findAtivosByPapelAndIgrejaIdIn`, `countAtivosByPapelAndIgrejaIdIn`), export de líderes se usar o papel
- Test: `usuario/service/LiderServiceTest.java`, `dashboard/service/DashboardServiceTest.java`

**Interfaces:**
- Produces: `LiderService.pessoaIdsLideres(List<UUID> igrejaIds): Set<UUID>` — união de líder/coordenador de ministério, líder/coordenador de base e líder de pequeno grupo (pessoas ativas). `listar` mantém o formato de `LiderResponse`, com campos de usuário nulos quando a pessoa não tem conta. Dashboard usa `pessoaIdsLideres(ids).size()`.

- [ ] **Step 1: Testes que falham** — `liderDeGrupoSemPapelApareceNaLista`, `pessoaLiderEmDoisMinisteriosContaUmaVez`, `dashboardContaLideresDerivados`.
- [ ] **Step 2:** `./mvnw test -Dtest='LiderServiceTest,DashboardServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar com queries de ids por igreja (sem carregar entidades inteiras).
- [ ] **Step 4:** `./mvnw test` → PASS.
- [ ] **Step 5:** Commit `Deriva líderes da função em ministérios, bases e grupos`.

---

### Task 7: JWT sem papéis e permissões no `/api/me`

**Files:**
- Create: `usuario/dto/PermissoesResponse.java`
- Modify: `security/JwtService.java` (remove parâmetro/claim `papeis`), `security/UsuarioAutenticado.java` (authorities vazias), `usuario/service/UsuarioService.java` (`autenticar`), `usuario/dto/LoginResponse.java` (remove `papeis`), `usuario/dto/MeResponse.java` (+ `PermissoesResponse permissoes`), `usuario/service/MeService.java`
- Test: `security/JwtServiceTest.java`, `security/JwtAuthenticationFilterTest.java`, `usuario/service/MeServiceTest.java`

**Interfaces:**
- Produces: `record PermissoesResponse(List<String> capacidades, List<UUID> ministeriosLiderados, List<UUID> basesLideradas, List<UUID> gruposLiderados, boolean ehVoluntario)` (capacidades = nomes do enum, ordenados). `MeResponse.papeis` continua (atribuídos, para exibição). JSON do `/me`: `"permissoes": {...}`.

- [ ] **Step 1: Testes que falham** — `tokenNaoContemPapeis` (claims sem `papeis`), `meDevolvePermissoesDoPastor`, `meDeLiderSemPapelDevolveMinisteriosLiderados`.
- [ ] **Step 2:** `./mvnw test -Dtest='JwtServiceTest,MeServiceTest'` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS; `./mvnw spring-boot:run` com `SPRING_PROFILES_ACTIVE=dev`, login com usuário seed pastor, `GET /api/me` mostra `permissoes.capacidades` com 14 itens.
- [ ] **Step 5:** Commit `Expõe permissões no /api/me e remove papéis do JWT`.

---

### Task 8 (Flutter): modelo de permissões e regras do app

Repo: `../base_interface`.

**Files:**
- Modify: `lib/models/models.dart` (`Role` vira `enum Capacidade` espelhando o backend; `AuthUser.roles` → `AuthUser.permissoes`, + `papeis: List<String>`), `lib/services/auth_service.dart` (lê `permissoes`/`papeis` do `/me`; não lê mais `papeis` do login), `lib/providers/auth_provider.dart` (`can(Capacidade)`, `recarregarMe()`), `lib/utils/permissions.dart` (reescrito), `lib/utils/route_access.dart`, `lib/navigation/nav_config.dart`
- Test: `test/utils/permissions_test.dart`, `test/utils/route_access_test.dart`, `test/helpers/fixtures.dart`

**Interfaces:**
- Produces (Dart):
  - `class Permissoes { Set<Capacidade> capacidades; Set<String> ministeriosLiderados, basesLideradas, gruposLiderados; bool ehVoluntario; bool tem(Capacidade c); bool get lideraAlgo; factory fromJson }`
  - `bool isGestao(AuthUser?)` = `tem(pessoasVerTodas)` (substitui `isPastorOuAdmin`).
  - Funções existentes de `permissions.dart` mantêm nome e assinatura, mas decidem por `permissoes`: `podeGerenciarMinisterio` (gestão ou `ministeriosLiderados.contains(id)`), `podeGerenciarBaseMinisterial`, `podeGerenciarSmallGroup` (gestão ou `gruposLiderados` ou ministério liderado — remove a regra "qualquer LIDER"), `podeEditarTextoCulto` (`cultoConteudoEditar` ou pregador), `podeEditarLouvor`, `podeLancarContagem` (`cultoContagem`), `podeCriarTipoEscala` (`escalasGerir` libera tudo), `canCreateUsuario` (`contasGerir`), `podeEnviarAniversarios` (`configurar`).
  - Menu/rotas: Gestão Igreja → `filialCriar` ou gestão; Usuários → `contasGerir`; Pessoas → gestão ou `lideraAlgo`; Voluntariado/Ministérios → gestão, `lideraAlgo` ou `ehVoluntario`; Relatórios → `relatoriosVer`; Configurações → `configurar`; Meu serviço → todos.
- Remove `_rolePermissions` e `hasPermission(roles, …)`.

- [ ] **Step 1: Testes que falham** — reescrever `permissions_test.dart`/`route_access_test.dart` com fixtures por perfil: pastor, secretaria, líder sem papel (ministério X), contador, membro. Casos: `secretariaNaoEditaTextoCulto`, `secretariaCriaEscalaPregador`, `liderGerenciaSoMinisterioLiderado`, `membroNaoAcessaUsuarios`, `liderAcessaPessoas`, `contadorLancaContagem`.
- [ ] **Step 2:** `flutter test test/utils` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `flutter test test/utils` → PASS (o resto do app pode não compilar ainda; compila ao fim da Task 9).
- [ ] **Step 5:** Commit `Usa permissões do servidor no lugar do mapa de papéis local`.

---

### Task 9 (Flutter): telas e atribuição de papéis

**Files:**
- Modify: telas que usam `Role`/`roles`/`hasPermission` (`screens/shell/app_shell.dart`, `screens/escalas/*`, `screens/culto/*`, `screens/pessoas/pessoa_detail_screen.dart`, `widgets/form_dialogs.dart`, `screens/voluntariado/voluntarios_screen.dart`), `lib/services/usuario_service.dart` (`alterarPapeis(usuarioId, papeis)` → `PUT /usuarios/{id}/papeis`; remove `promoverGestao`; criação sem `papeis`), `lib/services/pessoa_service.dart` (`criar(..., servicoId)`, `buscarPorNome(termo)`)
- Test: `test/services/contrato_api_test.dart`

**Interfaces:**
- Consumes: Task 8 (`Permissoes`, `isGestao`, funções de `permissions.dart`), Tasks 4/5 (endpoints).
- UI:
  - Diálogo "Papéis" no detalhe da pessoa (para quem tem conta): checkboxes de `PASTOR`/`SECRETARIA` (visíveis só com `papeisGestaoAtribuir`) e papéis customizados da igreja; salva via `alterarPapeis`; depois chama `recarregarMe()`.
  - Formulário de pessoa para líder: campo obrigatório "Serviço" com os serviços dos `ministeriosLiderados`; campos `observações` e `ativo` ocultos sem `dadosPastoraisEditar`.
  - "Adicionar pessoa existente" no ministério usa `buscarPorNome`.
  - Filtro "gestores" de voluntários usa `papeis` contendo `PASTOR`/`SECRETARIA`.

- [ ] **Step 1: Teste que falha** — `contrato_api_test.dart`: `alterarPapeis` faz `PUT /usuarios/{id}/papeis` com `{"papeis": [...]}`; `buscarPorNome` faz `GET /pessoas/busca?nome=`; criação de pessoa envia `servicoId`.
- [ ] **Step 2:** `flutter test test/services/contrato_api_test.dart` → FAIL.
- [ ] **Step 3:** Implementar serviços e telas.
- [ ] **Step 4:** `flutter analyze` sem erros; `flutter test` → PASS.
- [ ] **Step 5:** Verificação manual (backend `dev` rodando, `flutter run -d chrome`): logar como pastor (vê tudo, atribui SECRETARIA), secretaria (não vê "Criar filial" nem edita texto do culto), líder seed (vê só pessoas do ministério e cadastra com serviço).
- [ ] **Step 6:** Commit `Adapta telas ao modelo de papéis e capacidades`.

---

## Próximos planos

- Fase 2 (convite e senha) e Fase 3 (filial) ganham planos próprios após esta fase entregue. Nota: a atribuição de papéis (`PUT /api/usuarios/{id}/papeis`), descrita na Fase 2 da spec, foi antecipada para a Task 5 por depender diretamente do novo modelo de papéis.
