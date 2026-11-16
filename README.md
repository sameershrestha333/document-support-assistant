# Document Support Assistant

A Java and Spring Boot project for building a document Q&A assistant with Spring AI. It returns source-based answers with evidence and conversation context.

## About the project

Document Support Assistant is a Java and Spring Boot project that will answer questions from uploaded troubleshooting guides and runbooks. It will cite the source document and page, and tell users when the documents don't contain enough information.

**Example:** Upload a sample vehicle-data import guide and ask, “Why did my XML import fail with `INVALID_SCHEMA`?” The assistant will explain what to check and point to the relevant page.

## Why this project uses RAG

The assistant retrieves relevant passages from an indexed PDF and provides
them to the model with the question. Before returning a page citation, the
app checks that the quoted evidence appears in the cited passage.

When a guide changes, we index the revised PDF and query its new document ID.
The same model can answer from the updated evidence. Fine-tuning uses training
examples to change model behavior, such as making responses follow a preferred
format more consistently.

## Architecture

### PDF ingestion

```mermaid
flowchart LR
    U["Signed-in user"] --> A["Upload API"]
    A -->|"202 Accepted + job ID"| U
    A -->|"Stage PDF and enqueue"| W["Background indexing worker"]
    W --> P["Extract pages and split text"]
    P --> E["Generate embeddings"]
    E --> D["PostgreSQL + pgvector<br/>Chunks, page metadata, access grants"]
    D -->|"Transaction commits"| R["Job becomes READY"]
    U -->|"Poll job status"| A
    R -->|"Document ID for Q&A"| U
```

### Document Q&A and import-job chat

```mermaid
flowchart TD
    B["Browser or API client"] --> S["Spring Security authenticates user"]
    S --> Q["Document Q&A"]
    S --> C["Import-job chat"]
    Q --> A["Check document access"]
    A --> R["Embed question and retrieve passages"]
    D[("PostgreSQL + pgvector")] -->|"Retrieved passages"| R
    R --> M["Spring AI sends question and passages to model"]
    M --> V["Check quoted evidence against the cited passage"]
    V --> F["Final answer with page citation or insufficient evidence"]
    C --> T["Model requests Java job-status tool"]
    T --> O["Java checks job ownership"]
    O --> J["Return permitted job details and guide excerpt if available"]
    J --> H["Model writes the chat reply"]
    X["Separate MCP demo client"] -->|"MCP over stdio"| Y["Day 28 MCP server<br/>Synthetic job lookup"]
```

- **Search modes** — Document Q&A uses vector retrieval. The retrieval API also exposes hybrid search for comparison.
- **Execution limits** — Chat has a workflow timeout and a tool-call limit. Job and guide lookups have their own timeouts, and the SSE stream has a timeout.
- **MCP boundary** — The deployed app calls its Java tool directly. The Day 28 MCP server runs as a separate demo over standard input and output.


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
