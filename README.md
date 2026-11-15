# Sistema de Gestão da Igreja — Backend

API REST para gestão de pessoas, jornada de membros, ministérios, voluntários, disponibilidade e escalas.

O frontend é um app Flutter (iOS, Android e Web) em repositório separado (`base_interface`).

## Tecnologias

- Java 25
- Spring Boot 4.1
- Spring Web / Data JPA / Security / Validation
- PostgreSQL
- Flyway
- JWT (JJWT)
- OpenAPI / Swagger (springdoc)
- Maven
- Docker / Docker Compose

## Requisitos

- JDK 25+
- Maven 3.9+ (ou use o wrapper `./mvnw`)
- PostgreSQL 16+ (ou Docker)

## Objetivo

Permitir o gerenciamento de:

- Rede de igrejas (matriz e filiais)
- Pessoas (sem exigir login), vinculadas a uma igreja
- Jornada de membros
- Ministérios e serviços (incluindo coordenador como serviço ficção)
- Voluntários (pessoa ↔ serviço)
- Disponibilidade
- Escalas mensais (supervisor, pregador, culto, ministério)
- Conteúdo do culto (pregação e louvor)
- Usuários e papéis por igreja (RBAC)

A geração automática de escalas está preparada via `GeracaoEscalaService`, sem algoritmo implementado ainda.

## Decisões arquiteturais

1. **Pessoa ≠ Usuário** — relação 1:0..1. Cadastro pastoral independente de acesso ao sistema.
2. **Voluntário** — modelado como `pessoa_servico` (não há entidade `Voluntario` separada). Uma pessoa pode atuar em vários ministérios/serviços.
3. **Jornada** — campos principais em `jornada_membro` + catálogo extensível (`jornada_etapa_catalogo` / `pessoa_jornada_etapa`) para novas etapas sem obrigar sequência rígida.
4. **Escalas** — tabelas separadas: `escala`, `escala_servico`, `escala_voluntario`.
5. **RBAC** — papéis de sistema `PASTOR` e `SECRETARIA` (os da matriz cascateiam para as filiais) + papéis customizados por igreja, com capacidades; usuário pode ter múltiplos papéis, sempre no contexto da igreja.
6. **Rede de igrejas** — uma matriz vê todas as filiais; filial só acessa a própria. `igrejaId` em pessoa, ministério, usuário, cronograma e escala.
7. **Escalas mensais** — competência `YYYY-MM` e tipo `SUPERVISOR | PREGADOR | CULTO | MINISTERIO`. Supervisor/pregador padrão: pastor da igreja.
8. **Coordenador** — serviço ficção (`ficcao: true`) criado automaticamente em cada ministério; não é papel.

## Estrutura do projeto

Pacotes separados **por domínio**. Cada domínio usa as mesmas camadas:

`controller` → `service` → `repository` / `entity` / `dto`

URLs públicas são `/api/{recurso}` — sem prefixo de versão (`/v1`, `/v2`). O path `/v3/api-docs` é o contrato OpenAPI 3 do springdoc, não versionamento desta API.

```
src/main/java/com/example/base_api/
├── config/            # bootstrap, OpenAPI, seed (dev)
├── security/          # JWT, ACL, CORS
├── common/            # exceções e utilitários compartilhados
├── igreja/
├── pessoa/
├── jornada/
├── ministerio/
├── servico/
├── voluntario/
├── disponibilidade/
├── cronograma/
├── escala/
├── usuario/
├── dashboard/
├── export/
└── audit/
```

## Variáveis de ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PORT` | Porta | `5432` |
| `DB_NAME` | Nome do banco | `igreja_db` |
| `DB_USER` | Usuário | `igreja` |
| `DB_PASSWORD` | Senha | `igreja` |
| `JWT_SECRET` | Segredo JWT (≥ 32 chars) | valor de desenvolvimento |
| `JWT_EXPIRATION_MS` | Expiração do token | `86400000` |
| `SERVER_PORT` | Porta da API | `8080` |

Arquivo de exemplo: [`.env.example`](.env.example)

## Configuração do PostgreSQL

```sql
CREATE DATABASE igreja_db;
CREATE USER igreja WITH PASSWORD 'igreja';
GRANT ALL PRIVILEGES ON DATABASE igreja_db TO igreja;
```

Ou use o Docker Compose (abaixo).

## Como executar localmente

### 1. Banco com Docker

```bash
docker compose up -d postgres
```

### 2. Aplicação

```bash
cp .env.example .env
# ajuste as variáveis se necessário

export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

### Usuários iniciais

Com o perfil `dev`, na primeira execução são criados (senha `admin123`):

| Login (e-mail) | Perfil |
|----------------|--------|
| `pastor@base.local` | Pastor + secretaria da matriz (Ana Costa) — vê a rede |
| `secretaria@base.local` | Secretaria da filial Zona Sul (Luciana Rocha) |
| `lider@base.local` | Líder do Louvor (João da Silva) |
| `maria@base.local` | Coordenadora do Louvor |
| `midia@base.local` | Líder de Mídia |
| `membro@base.local` | Voluntária |
| `pastor.filial@base.local` | Pastor da filial |

O login aceita e-mail ou telefone (`identificador`); o telefone é comparado já normalizado.

### Produção: primeiro administrador

Fora do perfil `dev` não há seed. Na primeira subida com banco vazio, defina `ADMIN_EMAIL` e `ADMIN_SENHA` (mín. 8 caracteres; `ADMIN_NOME` é opcional): a API cria esse usuário (login pelo e-mail) com os papéis `PASTOR` + `SECRETARIA` na igreja matriz. `ADMIN_USERNAME` não é mais usado. Se já existir qualquer usuário, as variáveis são ignoradas — remova `ADMIN_SENHA` do ambiente após o primeiro login.

Health check para o orquestrador: `GET /actuator/health` (também `/actuator/health/liveness` e `/readiness`). Atrás de proxy reverso mantenha `FORWARD_HEADERS_STRATEGY=NATIVE`; sem proxy, use `NONE`.

`PUT /api/me` exige `senhaAtual` quando `novaSenha` é informada.

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identificador":"pastor@base.local","senha":"admin123"}'
```

Use o token: `Authorization: Bearer <token>`.

## Migrations (Flyway)

As migrations ficam em `src/main/resources/db/migration`.

Rodam automaticamente na subida da aplicação.

`spring.jpa.hibernate.ddl-auto=validate` — o schema é criado apenas pelo Flyway.

## Principais endpoints

| Método | Caminho | Descrição |
|--------|---------|-----------|
| POST | `/api/auth/login` | Autenticação JWT por e-mail ou telefone (claims: `pessoaId`, `igrejaId`, `igrejaTipo`, `papeis`) |
| GET/POST | `/api/auth/token/{token}` · `/api/auth/definir-senha` | Validar link de convite/redefinição e definir a senha |
| POST | `/api/auth/esqueci-senha` | Pedir link de nova senha por WhatsApp |
| GET/PUT | `/api/me` | Perfil com `pessoaId`, `igrejaId`, `igrejaTipo`, `igrejasVisiveis[]` |
| GET | `/api/igrejas` | Igrejas visíveis (matriz vê a rede; filial só a própria) |
| PUT | `/api/igrejas/{id}` | Atualizar nome/ativo da igreja |
| POST | `/api/igrejas/{matrizId}/filiais` | Criar filial com pastor (pessoa existente transferida ou nova) e convite, se o pastor ainda não tem conta ativa |
| PUT | `/api/igrejas/{id}/pastor` | Trocar o pastor titular (move o papel PASTOR; convida se preciso; `anteriorMantevePastor` indica que o anterior ficou com PASTOR até ser removido em Papéis) |
| GET/POST | `/api/pessoas` | Pessoas (`?igrejaId=`) |
| POST | `/api/pessoas/{id}/convite` | Convidar a pessoa para o app (link de uso único; a conta nasce `CONVIDADO`) |
| GET/POST | `/api/pessoas/{id}/historico` | Histórico da pessoa |
| GET/PUT | `/api/pessoas/{id}/jornada` | Jornada |
| CRUD | `/api/jornada-etapas` | Catálogo de etapas |
| GET/POST | `/api/ministerios` | Ministérios (`tipo`, `liderPessoaId`, serviço Coordenador ficção) |
| GET/POST | `/api/ministerios/{id}/servicos` | Serviços |
| CRUD | `/api/voluntarios` | Voluntários (serviço, cargo, etc.) |
| GET/POST | `/api/escalas` | Escalas mensais (`competencia`, `tipo`, `?igrejaId=&competencia=&pessoaId=`) |
| CRUD | `/api/escalas/{id}/servicos` | Serviços da escala |
| CRUD | `/api/escalas/{id}/voluntarios` | Designações |
| CRUD | `/api/disponibilidades/...` | Disponibilidade |
| GET | `/api/usuarios` | Usuários (`?igrejaId=`); contas nascem só por convite |
| PUT | `/api/usuarios/{id}/papeis` | Definir papéis (PASTOR/SECRETARIA só pelo pastor) |
| POST | `/api/usuarios/{id}/desativar` · `/reativar` · `/redefinicao-senha` | Ciclo de vida da conta |
| CRUD | `/api/papeis` | Papéis |
| CRUD | `/api/cronograma` | Cultos/eventos (`supervisorPessoaId`, `?igrejaId=`) |
| GET/PUT | `/api/cronograma/{id}/conteudo` | Texto da pregação e louvor |
| CRUD | `/api/pessoas/{id}/pausas` | Pausas no perfil |
| GET | `/api/lideres` | Líderes (quem lidera ou coordena ministério, base ou pequeno grupo) |
| GET | `/api/dashboard` | Totais do dashboard |
| POST | `/api/escalas/{id}/gerar-sugestao` | Sugestão automática (stub) |

## Exportação

Todos os dados principais podem ser exportados em **texto**, **Markdown** ou **Excel**:

```
GET /api/export/{recurso}?formato=txt|md|xlsx
```

Recursos:

- `pessoas`
- `historicos`
- `jornadas`
- `ministerios`
- `servicos`
- `voluntarios`
- `escalas`
- `disponibilidades`
- `usuarios`
- `completo` (tudo em um único arquivo; no Excel, uma aba por conjunto)

Exemplos:

```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/export/pessoas?formato=md" -o pessoas.md

curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/export/completo?formato=xlsx" -o completo.xlsx

curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/export/escalas?formato=txt" -o escalas.txt
```

## Testes

Testes de serviço com Mockito (não dependem de banco em memória):

```bash
./mvnw test
```

## Docker (API + PostgreSQL)

```bash
cp .env.example .env
docker compose up --build
```

## Observações

- Não implemente/espere ainda o algoritmo completo de geração automática de escalas.
- O frontend Flutter (`base_interface`) não faz parte deste repositório.
