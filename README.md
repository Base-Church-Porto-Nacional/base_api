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

- Pessoas (sem exigir login)
- Jornada de membros
- Ministérios e serviços
- Voluntários (pessoa ↔ serviço)
- Disponibilidade
- Escalas e confirmação/recusa
- Usuários e papéis (RBAC)

A geração automática de escalas está preparada via `ScheduleGenerationService`, sem algoritmo implementado ainda.

## Decisões arquiteturais

1. **Pessoa ≠ Usuário** — relação 1:0..1. Cadastro pastoral independente de acesso ao sistema.
2. **Voluntário** — modelado como `pessoa_servico` (não há entidade `Voluntario` separada). Uma pessoa pode atuar em vários ministérios/serviços.
3. **Jornada** — campos principais em `jornada_membro` + catálogo extensível (`jornada_etapa_catalogo` / `pessoa_jornada_etapa`) para novas etapas sem obrigar sequência rígida.
4. **Escalas** — tabelas separadas: `escala`, `escala_servico`, `escala_voluntario`.
5. **RBAC** — papéis em tabela (`ADMIN`, `PASTOR`, `LIDER`, `VOLUNTARIO`, `MEMBRO`); usuário pode ter múltiplos papéis.

## Estrutura do projeto

```
src/main/java/com/example/base_api/
├── config/
├── security/
├── common/
├── pessoa/
├── jornada/
├── ministerio/
├── servico/
├── voluntario/
├── disponibilidade/
├── escala/
│   └── generation/   # ScheduleGenerationService (stub)
└── usuario/
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

### Usuário inicial

Na primeira execução é criado:

- **username:** `admin`
- **senha:** `admin123`

Altere em produção.

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
| POST | `/api/auth/login` | Autenticação JWT |
| GET/POST | `/api/pessoas` | Pessoas |
| GET/POST | `/api/pessoas/{id}/historico` | Histórico da pessoa |
| GET/PUT | `/api/pessoas/{id}/jornada` | Jornada |
| CRUD | `/api/jornada-etapas` | Catálogo de etapas |
| GET/POST | `/api/ministerios` | Ministérios |
| GET/POST | `/api/ministerios/{id}/servicos` | Serviços |
| CRUD | `/api/voluntarios` | Voluntários (serviço, cargo, etc.) |
| POST | `/api/escalas` | Escalas |
| CRUD | `/api/escalas/{id}/servicos` | Serviços da escala |
| CRUD | `/api/escalas/{id}/voluntarios` | Designações |
| CRUD | `/api/disponibilidades/...` | Disponibilidade |
| CRUD | `/api/usuarios` | Usuários |
| CRUD | `/api/papeis` | Papéis |
| CRUD | `/api/cronograma` | Cultos/eventos (cronograma) |
| CRUD | `/api/pessoas/{id}/pausas` | Pausas no perfil |
| GET/PUT | `/api/me` | Meu perfil (usuário + pessoa) |
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
