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



























