# Variáveis de Ambiente

O projeto utiliza variáveis de ambiente para armazenar configurações que podem variar de acordo com o ambiente de execução da aplicação.

As variáveis relacionadas ao banco de dados PostgreSQL devem ser configuradas pelo desenvolvedor de acordo com sua própria instalação e configuração local.

## 1. Variáveis necessárias - (Para um Postgre SQL local)

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_HOST` | Endereço do servidor PostgreSQL | `localhost` |
| `DB_PORT` | Porta utilizada pelo PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco de dados utilizado pela aplicação | `nome_do_banco` |
| `DB_USERNAME` | Usuário utilizado para acessar o banco de dados | `seu_usuario` |
| `DB_PASSWORD` | Senha do usuário do banco de dados | `sua_senha` |

## 2. Configuração

Cada desenvolvedor deve configurar as variáveis de acordo com seu próprio ambiente.

Exemplo:

`DB_HOST=localhost`
`DB_PORT=5432`
`DB_NAME=nome_do_banco`
`DB_USERNAME=seu_usuario`
`DB_PASSWORD=sua_senha`

Os valores apresentados acima são apenas exemplos e devem ser substituídos pelas configurações utilizadas no ambiente local.

## 3. Segurança

Não devem ser adicionadas ao repositório informações sensíveis, como:

- Senhas;
- Tokens de autenticação;
- Chaves secretas;
- Credenciais de serviços externos.

Caso seja utilizado um arquivo `.env`, ele deve ser incluído no `.gitignore` para evitar que suas informações sejam enviadas ao repositório.

### Exemplo de `.env.example`

O projeto pode disponibilizar um arquivo `.env.example` contendo apenas a estrutura das variáveis:

`DB_HOST=`
`DB_PORT=`
`DB_NAME=`
`DB_USERNAME=`
`DB_PASSWORD=`

Esse arquivo serve como modelo para que cada desenvolvedor crie sua própria configuração.