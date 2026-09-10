# Instalação do Ambiente

### 1. Pré-requisitos

Antes de iniciar, instale:

- Git
- Java 25
- PGADMIN4
- PostgreSQL 18.3
- Flutter
- VSCode
- Intellij

### 2. Instalação do Git

O Git é utilizado para controle de versão e gerenciamento do código-fonte
do projeto.

### Windows

1. Acesse o site oficial do Git e baixe o instalador para Windows.
2. Execute o instalador.
3. Durante a instalação, as opções padrão podem ser mantidas.
4. Após a instalação, abra o **Git Bash**, PowerShell ou Prompt de Comando.

Verifique se o Git foi instalado corretamente:

```bash
git --version
```

### macOS

No macOS, o Git pode ser instalado utilizando o Homebrew:

```bash
brew install git
``` 

Caso o Homebrew não esteja instalado, instale-o antes de executar o comando acima. Depois verifique a instalação:

```bash
git --version
```

Caso o Homebrew não esteja instalado, instale-o antes de executar o comando acima

### Linux

Acesse o site oficial do Git e utilize o comando de instalação adequado para a sua distribuição linux, considere Archlinux como referência:

```bash
pacman -S git
```

E verifique a instalação:

```bash
git --version
```
### JAVA

O JAVA será usado para construção do backend usando o SpringBoot para construção da API.

## Windows 

Acesse o site oficial da Oracle e faça o download do Java LTS 25 e execute o instalador.

## macOS

A forma recomendada é utilizar o HomeBrew, caso já o tenha instalado execute:

```bash
brew install openjdk@25
```

E verifique a instalação:

```bash
java --version
```

A saída deverá indicar uma versão do Java 25

## Linux

No Linux, a instalação pode ser diferente a depender da sua distribuição Linux, considere o Archlinux como referência:

```bash
sudo pacman -S jdk25-openjdk
```

Verifique a versão:

```bash
pacman -Si jdk-openjdk
```

### PGADMIN4

Será usado para administração e desenvolvimento do PostgreSQL.


## Windows

Para fazer a sua instalação no windows basta acessar o site de downloads oficial e seguir os passos:

https://www.pgadmin.org/download/pgadmin-4-windows/

1. Acesse o site oficial do pgAdmin: pgAdmin Download.
2. Selecione a versão do Windows e baixe o instalador.
3. Localize o arquivo baixado e clique duas vezes para executar o instalador.
4. Siga as instruções do assistente de instalação.
5. Aceite os termos de licença, escolha o diretório de instalação e selecione os componentes que deseja instalar.
6. Após a instalação, abra o pgAdmin e defina uma senha mestre para proteger suas configurações.
7. Conecte o pgAdmin ao seu servidor PostgreSQL, criando um novo servidor local e inserindo as credenciais necessárias.

## macOS

O jeito mais simples é instalar o pgAdmin4 pelo seu instalador oficial. Basta seguir os passos seguintes:

1. Acesse o site de downloads oficiais para macOS e baixe o .dmg correspondente ao seu Mac.
Caso não saiba qual baixar vá em **Sobre este Mac** e veja o seu processador, se for Chip Apple M... você deve fazer o download para Apple Silicon, caso seja intel faça o download para Intel.
2. Abra o arquivo .dmg, vai aparecer a janela de instalação, arraste pgAdmin 4 para a pasta Applications/Aplicativos, ejete o .dmg e abra Aplicativos → pgAdmin 4.
3. Após instalar o PostgreSQL e o pgAdmin 4, abra o pgAdmin e registre um novo servidor em Servers → Register → Server.

## Linux

A melhor instalação tendo como referência o Archlinux é fazer a instalação pelo docker. Para isso siga estes passos (caso não tenha o docker instalado, instale-o):

1. Execute o comando:

```bash
sudo docker pull dpage/pgadmin4
```

2. Depois exeute, alterando os campos de e-mail e senha para suas credenciais:

```bash
sudo docker run -d \
  --name pgadmin4 \
  -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=seu_email \
  -e PGADMIN_DEFAULT_PASSWORD=sua_senha \
  dpage/pgadmin4
```
3. Acesse no navegador: http://localhost:5050
4. Faça login com as suas credenciais definidas no passo 2
5. Para verificar se o conteiner está funcionando: 

```bash
sudo docker ps
```















