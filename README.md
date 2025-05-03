=======
# REDE SOCIAL DA FURIA, O FURIAFAN

**FuriaFan** é uma plataforma interativa feita para fãs da equipe de e-sports **FURIA**, com foco inicial no cenário de **Counter-Strike**. A aplicação permite que o usuário acompanhe conteúdos autênticos da equipe via redes sociais, com acesso personalizado e seguro por meio de cadastro tradicional ou autenticação via Twitter.

##Sumário

- [Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [Funcionalidades Principais](#-funcionalidades-principais)
- [Autenticação e Segurança](#-autenticação-e-segurança)
- [Timeline de Tweets](#-timeline-de-tweets)
- [Validações e Regras de Negócio](#-validações-e-regras-de-negócio)
- [Limitações das APIs do Twitter](#-limitações-das-apis-do-twitter)
- [Estrutura de Projeto](#-estrutura-de-projeto)
- [Demonstração em Vídeo](#-demonstração-em-vídeo)
- [Contato](#-contato)

---

## Tecnologias Utilizadas

### Backend - Java Spring Boot

- **Spring Boot** – estrutura principal do backend.
- **Spring Security + JWT** – controle de autenticação e autorização.
- **Flyway** – migração e controle de versão do banco de dados.
- **Google Cloud Vision API** – reconhecimento óptico de caracteres (OCR) para autenticação de documentos.
- **Regex** – validação de campos como CPF.
- **OAuth 2.0** – autenticação com Twitter.
- **API XV2 do Twitter** – obtenção de tweets e imagens públicas.
- **PostgreSQL** – banco de dados relacional.

### Frontend - Angular

- Interface responsiva em Angular.
- Comunicação com backend via HTTP (REST APIs).
- Autenticação baseada em token JWT.

---

## Funcionalidades Principais

### Cadastro de Usuário

- Campos obrigatórios:
  - Nome completo
  - E-mail (único)
  - CPF (único e válido)
  - Endereço completo
  - Gênero
- Validação automática com regex e Flyway.
- Após cadastro, o sistema recomenda realizar **autenticação documental via OCR** para garantir a validade do CPF.

### Login

- Duas opções:
  - **Login com e-mail + senha** (após cadastro)
  - **Login com conta do Twitter** (OAuth 2.0)

---

## Autenticação e Segurança

- Autenticação com **JWT (JSON Web Token)**.
- Tokens gerados após login permitem:
  - Acesso autenticado à timeline.
  - Identificação única do usuário no sistema.
- Controle de permissões com Spring Security.
- Política de CORS configurada para comunicação com o frontend Angular.

---

## Timeline de Tweets

### O que é?

- Interface que exibe **tweets públicos da equipe FURIA** relacionados ao jogo selecionado durante o cadastro (ex: **Counter-Strike**).

### Como funciona?

- Uso da **API XV2 do Twitter** para coletar e renderizar tweets com imagens.
- Tweets extraídos conforme o filtro do e-sport definido.

### Limitação Técnica

- A API gratuita permite apenas **1 tweet a cada 15 minutos**.
  - Exceder isso gera erro `429 - Too Many Requests`.
- Alternativa paga permite 5 tweets por 15 minutos, mas custa **$200/mês**.

### Estratégia adotada:

- Coleta de apenas **1 tweet recente e relevante** por sessão/intervalo de tempo.
- Atualização da timeline feita com base nessa limitação para evitar bloqueios.

---

## Validações e Regras de Negócio

### Regras de Cadastro

- Cada CPF e e-mail deve ser **único**.
- Apenas **uma conta por CPF**.
- CPF é validado por regex e também por comparação com OCR do documento.
- Se o CPF do documento **não corresponder** ao cadastrado, o usuário:
  - Será notificado da inconsistência.
  - Deverá criar um novo cadastro com CPF válido.

### Autenticação de Documento

- Feita após cadastro tradicional.
- O usuário envia uma imagem de um documento com CPF.
- A imagem é processada com a **Google Vision API**.
- O texto extraído é comparado com o CPF cadastrado.
- Se corresponder → o usuário torna-se **"verificado"**

## Contato

Caso tenha dúvidas ou sugestões, entre em contato:

- **Developer:** [Lucas Fend Ribeiro]
- **E-mail:** [lucasribeiro.developer@outlook.com]
- **LinkedIn:** [https://www.linkedin.com/in/lucasribfend/](#https://www.linkedin.com/in/lucasribfend/)

---
