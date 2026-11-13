const form = document.querySelector("#ask-form");
const ask = form.querySelector('button[type="submit"]');
const stop = document.querySelector("#stop");
const statusLine = document.querySelector("#status");
const answer = document.querySelector("#answer");
const citation = document.querySelector("#citation");
let activeRequest;

stop.addEventListener("click", () => activeRequest?.abort());

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (activeRequest) return;

    const documentId = document.querySelector("#document-id").value.trim();
    const question = document.querySelector("#question").value.trim();
    if (!documentId || !question) {
        statusLine.textContent = "Enter a document ID and a question.";
        return;
    }

    const controller = new AbortController();
    activeRequest = controller;
    ask.disabled = true;
    stop.disabled = false;
    statusLine.textContent = "Asking…";
    answer.textContent = "";
    citation.textContent = "";

    try {
        const response = await fetch("/api/documents/ask/stream", {
            method: "POST",
            headers: { "Content-Type": "application/json", "Accept": "text/event-stream" },
            body: JSON.stringify({ documentId, question }),
            signal: controller.signal
        });

        if (response.status === 404) {
            throw new Error("Document not found or unavailable to this account.");
        }
        if (!response.ok) throw new Error("Request failed (HTTP " + response.status + ").");
        if (!response.body || !response.headers.get("content-type")?.includes("text/event-stream")) {
            throw new Error("The assistant did not return an answer stream.");
        }

        const result = await readAnswer(response.body);
        answer.textContent = result.answer;
        if (result.citation) {
            const { fileName, pageNumber } = result.citation;
            citation.textContent = "Source: " + fileName + ", page " + pageNumber;
        }
        statusLine.textContent = result.status === "INSUFFICIENT_EVIDENCE"
            ? "The document does not contain enough information."
            : "";
    } catch (error) {
        answer.textContent = "";
        citation.textContent = "";
        statusLine.textContent = controller.signal.aborted ? "Stopped." : error.message;
    } finally {
        activeRequest = undefined;
        ask.disabled = false;
        stop.disabled = true;
    }
});

async function readAnswer(body) {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    try {
        while (true) {
            const { value, done } = await reader.read();
            if (done) throw new Error("The stream ended before the final answer arrived.");

            buffer += decoder.decode(value, { stream: true });
            let boundary;
            while ((boundary = /\r?\n\r?\n/.exec(buffer))) {
                const lines = buffer.slice(0, boundary.index).split(/\r?\n/);
                buffer = buffer.slice(boundary.index + boundary[0].length);
                const event = lines.find(line => line.startsWith("event:"))?.slice(6).trim();
                if (event !== "delta" && event !== "complete" && event !== "error") continue;
                const data = JSON.parse(lines.filter(line => line.startsWith("data:"))
                    .map(line => line.slice(5).replace(/^ /, "")).join("\n"));

                if (event === "complete") return data;
                if (event === "error") throw new Error(data.message || "Could not complete the answer.");
                statusLine.textContent = "Draft answer. Checking the citation…";
                answer.textContent += data.text;
            }
        }
    } finally {
        await reader.cancel().catch(() => {});
        reader.releaseLock();
    }
}

const uploadForm = document.querySelector("#upload-form");
const uploadButton = uploadForm.querySelector("button");
const uploadStatus = document.querySelector("#upload-status");

uploadForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (uploadButton.disabled) return;

    const file = document.querySelector("#pdf-file").files[0];
    if (!file) return;
    uploadButton.disabled = true;
    uploadStatus.textContent = "Uploading…";
    try {
        const data = new FormData();
        data.append("file", file);
        const response = await fetch("/api/documents/uploads", { method: "POST", body: data });
        if (!response.ok) throw new Error("Upload failed (HTTP " + response.status + ").");

        let job = await response.json();
        const statusUrl = "/api/documents/uploads/" + encodeURIComponent(job.jobId);
        while (job.status === "PENDING" || job.status === "PROCESSING") {
            uploadStatus.textContent = job.status === "PENDING" ? "Pending…" : "Processing…";
            await new Promise(resolve => setTimeout(resolve, 1000));
            const poll = await fetch(statusUrl, { cache: "no-store" });
            if (!poll.ok) throw new Error("Could not check upload status (HTTP " + poll.status + ").");
            job = await poll.json();
        }
        if (job.status !== "READY" || !job.documentId) {
            throw new Error(job.error || "The PDF could not be indexed.");
        }
        document.querySelector("#document-id").value = job.documentId;
        uploadStatus.textContent = "Ready. You can ask a question about this PDF.";
    } catch (error) {
        uploadStatus.textContent = error.message;
    } finally {
        uploadButton.disabled = false;
    }
});
