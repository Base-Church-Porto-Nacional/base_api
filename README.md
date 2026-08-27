# Sistema de Gestão da Igreja — Backend

API REST para gestão de pessoas, jornada de membros, ministérios, voluntários, disponibilidade e escalas.

O frontend (React + TypeScript) será desenvolvido em repositório separado.

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
5. **RBAC** — papéis em tabela (`ADMIN`, `PASTOR`, `LIDER`, `VOLUNTARIO`, `MEMBRO`); usuário pode ter múltiplos papéis, sempre no contexto da igreja.
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

Na primeira execução são criados (senha `admin123`):

| Username | Perfil |
|----------|--------|
| `admin` | Pastor/admin da matriz (Ana Costa) — vê a rede |
| `filial` | Admin da filial Zona Sul |
| `joao` | Líder do Louvor |
| `maria` | Coordenadora do Louvor |
| `midia` | Líder de Mídia |
| `membro` | Voluntária |
| `pedro` | Pastor da filial |

Altere as senhas em produção.

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","senha":"admin123"}'
```

Use o token: `Authorization: Bearer <token>`.

## Migrations (Flyway)

As migrations ficam em `src/main/resources/db/migration`.

Rodam automaticamente na subida da aplicação.

`spring.jpa.hibernate.ddl-auto=validate` — o schema é criado apenas pelo Flyway.

## Principais endpoints

| Método | Caminho | Descrição |
|--------|---------|-----------|
| POST | `/api/auth/login` | Autenticação JWT (claims: `pessoaId`, `igrejaId`, `igrejaTipo`, `papeis`) |
| GET/PUT | `/api/me` | Perfil com `pessoaId`, `igrejaId`, `igrejaTipo`, `igrejasVisiveis[]` |
| GET/POST | `/api/igrejas` | Igrejas visíveis (matriz vê a rede; filial só a própria) |
| POST | `/api/igrejas/{matrizId}/filiais` | Criar filial com pastor e admin |
| GET/POST | `/api/pessoas` | Pessoas (`?igrejaId=`) |
| POST | `/api/pessoas/{id}/usuario` | Criar usuário a partir da pessoa (papéis da igreja) |
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
| CRUD | `/api/usuarios` | Usuários (`?igrejaId=`) |
| CRUD | `/api/papeis` | Papéis |
| CRUD | `/api/cronograma` | Cultos/eventos (`supervisorPessoaId`, `?igrejaId=`) |
| GET/PUT | `/api/cronograma/{id}/conteudo` | Texto da pregação e louvor |
| CRUD | `/api/pessoas/{id}/pausas` | Pausas no perfil |
| GET | `/api/lideres` | Líderes (papel LIDER) |
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
- Frontend React não faz parte deste repositório.
