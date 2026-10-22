# Budgeting — Controle de gastos por voz com Spring AI

Projeto final do **Bootcamp CI&T – Java com IA (DIO)**, no módulo de Spring AI.

Uma API de controle de gastos em que dá para registrar e consultar transações **falando**. O áudio é transcrito, o modelo de linguagem decide qual caso de uso chamar (tool calling) e a resposta volta em áudio. Tudo isso mantendo a mesma arquitetura em camadas: a IA usa os mesmos casos de uso da API REST.

## Fluxo do endpoint de IA

```text
áudio (.m4a/.mp3)
   │  TranscriptionModel (whisper-1)
   ▼
texto: "gastei 80 reais no mercado"
   │  ChatClient (gpt-4o-mini) + tools
   ▼
persist-transaction / list-transactions-by-category  ──► MySQL
   │
   ▼
texto de resposta
   │  TextToSpeechModel (gpt-4o-mini-tts)
   ▼
audio.mp3
```

## Endpoints

| Método | Endpoint                   | Descrição                                              |
|--------|----------------------------|--------------------------------------------------------|
| `POST` | `/transactions`            | Registra uma transação (JSON)                          |
| `GET`  | `/transactions/{category}` | Lista transações por categoria (`GROCERIES`, `PHARMA`, `AUTO`) |
| `POST` | `/transactions/ai`         | Recebe um áudio (`multipart/form-data`, campo `file`) e responde com um MP3 |

## Tecnologias

- Java 25 + Spring Boot 4
- Spring AI 2 (OpenAI): `ChatClient`, tool calling, transcrição e text-to-speech
- Spring Data JPA + MySQL
- Docker Compose
- Lombok

## Estrutura

```text
src/main/java/dio/budgeting
├── domain/          -> Transaction, TransactionId, Category, TransactionRepository
├── application/     -> PersistTransactionUseCase e ListTransactionsByCategoryUseCase (também expostos como @Tool)
└── infrastructure/
    ├── http/        -> TransactionController (REST + endpoint de IA)
    └── persistence/ -> entidade e repositórios JPA
src/main/resources/prompts/system-message.st  -> prompt de sistema do assistente financeiro
```

## O que aprendi

- **ChatModel vs ChatClient**: o `ChatModel` é a API de baixo nível do provedor; o `ChatClient` é uma API fluente com prompt de sistema padrão, tools e chamadas encadeadas.
- **Tool calling**: anotando os casos de uso com `@Tool` e os parâmetros com `@ToolParam`, o modelo decide sozinho quando registrar ou listar transações. A regra de negócio continua no caso de uso, e a IA é só mais um "cliente" da camada de aplicação.
- **Speech-to-text**: `TranscriptionModel` com Whisper, idioma `pt` e um prompt de contexto para melhorar a transcrição de valores em reais.
- **Text-to-speech**: `TextToSpeechModel` gerando MP3 com a resposta final.
- **Prompt de sistema em arquivo** (`.st`), fora do código Java.
- **Testes de integração com IA**: os testes `*IT` chamam a OpenAI de verdade e só rodam quando a variável `OPENAI_API_KEY` está definida (`@EnabledIfEnvironmentVariable`).
- **Valores monetários em centavos** (`long`) no domínio, para evitar problemas de arredondamento.

## Como rodar

Pré-requisitos: Docker em execução (o MySQL do `compose.yml` sobe automaticamente no `bootRun`) e uma chave da OpenAI.

```bash
export OPENAI_API_KEY="sua_chave_aqui"
./gradlew bootRun
```

Exemplos:

```bash
curl -i -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Compra no mercado","category":"GROCERIES","amount":8000}'

curl http://localhost:8080/transactions/GROCERIES

curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```

Testes de integração com a OpenAI (usam os áudios de `src/test/resources/audio`):

```bash
./gradlew test --tests "*IT"
```

> O teste `contextLoads` sobe o contexto completo e precisa de MySQL e de `OPENAI_API_KEY` configurados.
