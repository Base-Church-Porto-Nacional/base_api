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
- Docker

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

### Docker

O Docker é utilizado para executar e gerenciar containers, facilitando a configuração e execução dos serviços utilizados pelo projeto.

### Windows

1. Acesse o site oficial do Docker e baixe o Docker Desktop para Windows.
2. Execute o instalador.
3. Siga as etapas apresentadas pelo instalador.
4. Reinicie o computador, caso seja solicitado.
5. Abra o Docker Desktop.

Verifique se o Docker foi instalado corretamente:

```bash
docker --version
```

### macOS

Acesse o site oficial do Docker e baixe o Docker Desktop correspondente ao processador do Mac.
Abra o arquivo .dmg.
Arraste o Docker para a pasta Applications.
Abra o Docker Desktop.

Verifique a instalação:

```bash
docker --version
```

### Linux

No Linux, considerando o Arch Linux como referência, instale o Docker:

```bash
sudo pacman -S docker
```

1. Inicie o serviço:

```bash
sudo systemctl start docker
```

2. Para iniciar o Docker automaticamente com o sistema:

```bash
sudo systemctl enable docker
``` 

3. Verifique a instalação:

```bash
docker --version
```

### PostgreSQL 18.3

O PostgreSQL é utilizado como sistema de gerenciamento do banco de dados do projeto.

## Windows

1. Acesse o site oficial do PostgreSQL e baixe o instalador para Windows.

2. Execute o instalador.

3. Durante a instalação, defina uma senha para o usuário do PostgreSQL.

4. Mantenha a porta padrão `5432`, caso não exista necessidade de alterá-la.

5. Após a instalação, abra o PowerShell ou Prompt de Comando.

Verifique se o PostgreSQL foi instalado corretamente:

```bash
psql --version
```

### macOS

No macOS, o PostgreSQL pode ser instalado utilizando o Homebrew:

```bash
brew install postgresql@18
```

Inicie o serviço do PostgreSQL:

```bash
brew services start postgresql@18
```

Verifique a instalação:

```bash
psql --version
```

### Linux

No Linux, considerando Arch Linux como referência:

```bash
sudo pacman -S postgresql
```

Inicialize o banco de dados:

```bash
sudo -u postgres initdb --locale=C.UTF-8 --encoding=UTF8 -D /var/lib/postgres/data
```

Inicie o serviço:

```bash
sudo systemctl start postgresql
```

Para iniciar o PostgreSQL automaticamente com o sistema:

```bash
sudo systemctl enable postgresql
```

Verifique a instalação:

```bash
psql --version
```

### Flutter

O Flutter é utilizado para o desenvolvimento da aplicação.

Para utilizar o Flutter no Visual Studio Code:

1. Instale o **Flutter SDK** conforme o sistema operacional utilizado.

2. Abra o Visual Studio Code e acesse a área de extensões.

3. Pesquise por **Flutter** e instale a extensão oficial.

4. A extensão Flutter também instala a extensão **Dart**, necessária para o desenvolvimento.

5. Verifique a configuração do ambiente pelo terminal:

```bash
flutter doctor
```

### Windows

1. Acesse o site oficial do Flutter e baixe o SDK para Windows.

2. Extraia o arquivo do Flutter em um diretório de sua preferência.

3. Adicione o diretório `flutter\bin` às variáveis de ambiente do sistema.

4. Abra o PowerShell ou Prompt de Comando.

Verifique a instalação:

```bash
flutter --version
```

Também é possível verificar se existem dependências adicionais utilizando:

```bash
flutter doctor
```

### macOS

1. Acesse o site oficial do Flutter e baixe o SDK para macOS.

2. Extraia o arquivo baixado.

3. Adicione o diretório `flutter/bin` à variável `PATH`.

4. Abra o Terminal.

Verifique a instalação:

```bash
flutter --version
```

Verifique as dependências:

```bash
flutter doctor
```

### Linux

No Linux, considerando Arch Linux como referência, baixe o SDK do Flutter através do site oficial e extraia-o em um diretório apropriado.

Adicione o diretório `flutter/bin` à variável `PATH`.

Verifique a instalação:

```bash
flutter --version
```

Verifique as dependências:

```bash
flutter doctor
```

### Visual Studio Code

O Visual Studio Code é utilizado como ambiente de desenvolvimento e editor de código do projeto.

### Windows

1. Acesse o site oficial do Visual Studio Code e baixe o instalador para Windows.

2. Execute o instalador.

3. Durante a instalação, as opções padrão podem ser mantidas.

4. Após a instalação, abra o Visual Studio Code.

Verifique se o programa foi instalado corretamente.

### macOS

1. Acesse o site oficial do Visual Studio Code e baixe a versão para macOS.

2. Abra o arquivo `.dmg`.

3. Arraste o Visual Studio Code para a pasta **Applications**.

4. Abra o Visual Studio Code.

### Linux

No Linux, considerando Arch Linux como referência:

```bash
sudo pacman -S code
```

Após a instalação, execute:

```bash
code
```

para abrir o Visual Studio Code.

### IntelliJ IDEA

O IntelliJ IDEA é utilizado como ambiente de desenvolvimento integrado (IDE) para desenvolvimento do projeto.

### Windows

1. Acesse o site oficial do IntelliJ IDEA e baixe o instalador para Windows.

2. Execute o instalador.

3. Durante a instalação, as opções padrão podem ser mantidas.

4. Após a instalação, abra o IntelliJ IDEA.

## macOS

1. Acesse o site oficial do IntelliJ IDEA e baixe a versão para macOS.

2. Abra o arquivo `.dmg`.

3. Arraste o IntelliJ IDEA para a pasta **Applications**.

4. Abra o IntelliJ IDEA.

### Linux

No Linux, considerando Arch Linux como referência, o IntelliJ IDEA pode ser instalado utilizando o gerenciador de pacotes:

```bash
sudo pacman -S intellij-idea-community-edition
```

Após a instalação, o IntelliJ IDEA poderá ser iniciado pelo menu de aplicativos do sistema.