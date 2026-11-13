package com.dsa.documentsupportassistant.eval;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class EvaluationRunner {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException("Usage: EvaluationRunner <documentId> <appRevision> [tuning|held-out]");
        }
        String split = args.length == 3 ? args[2] : "tuning";
        if (!split.equals("tuning") && !split.equals("held-out")) {
            throw new IllegalArgumentException("Split must be tuning or held-out.");
        }
        String model = requireEnv("OPENAI_CHAT_MODEL");
        Map<String, String> passwords = Map.of(
                "user", requireEnv("APP_TEST_PASSWORD"), "user2", requireEnv("APP_TEST_PASSWORD_2"));
        JsonNode cases = JSON.readTree(Files.readString(Path.of("evals/day31-cases.json"))).get("cases");
        Path report = Path.of("target", "day36-" + split + "-" + UUID.randomUUID() + ".jsonl");
        Files.createDirectories(report.getParent());
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        int completed = 0;
        int failures = 0;
        try (BufferedWriter output = Files.newBufferedWriter(report, StandardOpenOption.CREATE_NEW)) {
            write(output, Map.of("runAt", Instant.now().toString(), "split", split,
                    "model", model, "appRevision", args[1], "promptRevision", args[1],
                    "documentId", args[0], "indexVersion", 1));
            for (JsonNode test : cases) {
                if (!split.equals(test.path("split").asText())) {
                    continue;
                }
                Map<String, Object> result = run(client, test, args[0], passwords);
                write(output, result);
                completed++;
                if (!Boolean.TRUE.equals(result.get("contractPass"))) {
                    failures++;
                }
                System.out.println(result.get("caseId") + " " + result.get("reviewStatus"));
            }
            write(output, Map.of("cases", completed, "contractFailures", failures));
        }
        System.out.printf("%d cases, %d contract failures. Report: %s%n", completed, failures, report);
    }

    private static Map<String, Object> run(HttpClient client, JsonNode test, String documentId,
                                           Map<String, String> passwords) throws InterruptedException {
        String endpoint = test.path("endpoint").asText();
        String user = test.path("user").asText();
        String question = test.path("question").asText();
        String conversationId = UUID.randomUUID().toString();
        boolean document = endpoint.equals("/api/documents/ask");
        if (!document && !endpoint.equals("/api/chat")) {
            throw new IllegalArgumentException("Unknown evaluation endpoint: " + endpoint);
        }
        Map<String, String> payload = document ? Map.of("documentId", documentId, "question", question)
                : Map.of("conversationId", conversationId, "message", question);
        String auth = Base64.getEncoder().encodeToString(
                (user + ":" + passwords.get(user)).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8080" + endpoint))
                .timeout(Duration.ofSeconds(90)).header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload))).build();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("caseId", test.path("id").asText());
        result.put("category", test.path("category").asText());
        result.put("user", user);
        result.put("question", question);
        result.put("expected", test.get("expected"));
        result.put("conversationId", conversationId);
        long start = System.nanoTime();
        boolean pass = false;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("httpStatus", response.statusCode());
            result.put("latencyMs", (System.nanoTime() - start) / 1_000_000);
            if (response.statusCode() == 200) {
                JsonNode body = JSON.readTree(response.body());
                result.put("response", body);
                pass = body != null && check(body, test.path("expected"), document);
            } else {
                result.put("response", response.body());
            }
        } catch (java.io.IOException | RuntimeException ex) {
            result.putIfAbsent("latencyMs", (System.nanoTime() - start) / 1_000_000);
            result.put("error", ex.getClass().getSimpleName());
        }
        result.put("contractPass", pass);
        result.put("reviewStatus", pass ? "REVIEW_CONTENT" : "CONTRACT_FAIL");
        return result;
    }

    private static boolean check(JsonNode body, JsonNode expected, boolean document) {
        if (!body.path("answer").isString() || body.path("answer").asText().isBlank()
                || !body.path("supported").isBoolean() || !body.path("evidence").isString()) {
            return false;
        }
        boolean supported = body.path("supported").asBoolean();
        if (expected.has("supported") && expected.get("supported").asBoolean() != supported) {
            return false;
        }
        if (!document) {
            return !(expected.has("clarification") || expected.has("discloseJobDetails")) || !supported;
        }
        if (!supported) {
            return "INSUFFICIENT_EVIDENCE".equals(body.path("status").asText())
                    && body.path("citation").isNull();
        }
        JsonNode citation = body.path("citation");
        return !body.path("evidence").asText().isBlank()
                && "sample-input-file.pdf".equals(citation.path("fileName").asText())
                && citation.path("pageNumber").asInt() > 0
                && !citation.path("passageId").asText().isBlank();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name + " in the runner environment.");
        }
        return value;
    }

    private static void write(BufferedWriter output, Object value) throws java.io.IOException {
        output.write(JSON.writeValueAsString(value));
        output.newLine();
        output.flush();
    }
}
