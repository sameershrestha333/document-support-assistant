# Document Support Assistant

A Java and Spring Boot project for building a document Q&A assistant with Spring AI. It returns source-based answers with evidence and conversation context.

## About the project

Document Support Assistant is a Java and Spring Boot project that will answer questions from uploaded troubleshooting guides and runbooks. It will cite the source document and page, and tell users when the documents don't contain enough information.

**Example:** Upload a sample vehicle-data import guide and ask, “Why did my XML import fail with `INVALID_SCHEMA`?” The assistant will explain what to check and point to the relevant page.


## Run locally

Use JDK 21. Set OPENAI_API_KEY and OPENAI_CHAT_MODEL as local environment variables; never commit the actual API key. From the project root, run the Maven wrapper in PowerShell.

```powershell
.\mvnw.cmd spring-boot:run
```

Open [Actuator health](http://localhost:8080/actuator/health) and check for `"status":"UP"`.

## Try the chat API in Bruno

Send a **POST** request to `http://localhost:8080/api/chat`. Choose **JSON** in the Body tab and enter the following.

```json
{"conversationId":"demo-1","message":"Source text: The maximum upload size is 10 MB.\nQuestion: What is the upload limit?"}
```

A valid message returns **200 OK**.

```json
{"answer":"The maximum upload size is 10 MB.","evidence":"The maximum upload size is 10 MB.","supported":true}
```

A missing or blank message, or a message longer than 2,000 characters, returns **400 Bad Request**.

## Run tests

```powershell
.\mvnw.cmd test
```
## Application Flow

```text
ChatController → validate → AssistantService
  → instructions + history → model → DocumentAnswer JSON

Provider failure → ModelProviderException
  → ChatExceptionHandler → HTTP 502 + ChatError
```
## Bruno Regression Tests

Start the app, open `bruno/documentsupportassistant` in Bruno, and run `regressionTesting` in sequence. The supported-answer request seeds the same-ID follow-up. Restart the app before a clean run to clear history.

Coverage: supported answers, missing evidence, blank and oversized messages, same-ID follow-ups, and fresh-ID isolation. Live model calls incur API usage.

## Current Limitations

- Source text is supplied manually; document upload and retrieval are planned.
- Conversation history is in memory and resets on restart.
- Conversation IDs are not yet tied to authenticated users.
