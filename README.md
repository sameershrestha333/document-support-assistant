# Document Support Assistant

A Java and Spring Boot project for building a document Q&A assistant with Spring AI. The first milestone provides a chat API with input validation and a fixed response.

## Run locally

Use JDK 21 or later. From the project root, run the Maven wrapper in PowerShell.

```powershell
.\mvnw.cmd spring-boot:run
```

Open [Actuator health](http://localhost:8080/actuator/health) and check for `"status":"UP"`.

## Try the chat API in Bruno

Send a **POST** request to `http://localhost:8080/api/chat`. Choose **JSON** in the Body tab and enter the following.

```json
{"message":"Hello"}
```

A valid message returns **200 OK**.

```json
{"answer":"The document support assistant is ready."}
```

A missing or blank message, or a message longer than 2,000 characters, returns **400 Bad Request**.

## Run tests

```powershell
.\mvnw.cmd test
```
