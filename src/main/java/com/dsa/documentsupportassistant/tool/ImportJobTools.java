package com.dsa.documentsupportassistant.tool;

import com.dsa.documentsupportassistant.dto.ImportJob;
import com.dsa.documentsupportassistant.dto.ImportJobStatus;
import com.dsa.documentsupportassistant.dto.RetrievalRequest;
import com.dsa.documentsupportassistant.service.DocumentRetrievalService;
import org.springframework.beans.factory.annotation.Value;
import com.dsa.documentsupportassistant.exception.ChatLimitException;
import com.dsa.documentsupportassistant.service.ImportJobService;
import com.dsa.documentsupportassistant.service.TimeLimits;

import java.util.Optional;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ImportJobTools {

    private final ImportJobService importJobService;

    private final DocumentRetrievalService retrievalService;
    private final String guideId;

    public ImportJobTools(ImportJobService importJobService,
                          DocumentRetrievalService retrievalService,
                          @Value("${app.import-guide.document-id:}") String guideId) {
        this.retrievalService = retrievalService;
        this.guideId = guideId;
        this.importJobService = importJobService;
    }

    @Tool(description = """
        Look up the status of an import job by ID.
        Use this when the user asks whether an import completed,
        is still running, or failed and why.
        """)
    public String getImportStatus(
            @ToolParam(description = "Import job ID, such as JOB-102") String jobId,
            ToolContext context) {

        if (jobId == null || !jobId.matches("JOB-[0-9]+")) {
            return "Invalid job ID. Expected a value like JOB-102.";
        }

        Object identity = context.getContext().get("username");
        if (!(identity instanceof String username) || username.isBlank()) {
            return "Import job lookup requires authentication.";
        }

        Optional<ImportJob> ownedJob;
        try {
            ownedJob = TimeLimits.run(
                    () -> importJobService.findByIdForOwner(jobId, username),
                    1, "JOB_LOOKUP_TIMEOUT");
        } catch (ChatLimitException ex) {
            if ("JOB_LOOKUP_TIMEOUT".equals(ex.code())) {
                return "The import job lookup timed out.";
            }
            throw ex;
        }

        String result = ownedJob
                .map(job -> "Job facts: File: " + job.filename()
                        + "; status: " + job.status()
                        + (job.errorDetails() == null
                        ? ""
                        : "; error: " + job.errorDetails()))
                .orElse("No import job found for that ID.");

        if (ownedJob.isPresent()) {
            ImportJob job = ownedJob.get();
            if (job.status() == ImportJobStatus.FAILED && job.errorDetails() != null) {
                result += "\nGuide excerpt: " + findGuidance(job.errorDetails());
            }
        }

        int maxLength = 500;
        return result.length() <= maxLength
                ? result
                : result.substring(0, maxLength - 3) + "...";
    }

    private String findGuidance(String error) {
        if (guideId.isBlank()) {
            return "Unavailable.";
        }
        try {
            return TimeLimits.run(() -> retrievalService.search(new RetrievalRequest(guideId, error))
                    .stream()
                    .filter(chunk -> !chunk.text().isBlank())
                    .findFirst()
                    .map(chunk -> "Page " + chunk.pageNumber() + ": " + chunk.text().strip())
                    .orElse("Unavailable."), 2, "GUIDE_LOOKUP_TIMEOUT");
        } catch (ChatLimitException ex) {
            if ("GUIDE_LOOKUP_TIMEOUT".equals(ex.code())) {
                return "Unavailable.";
            }
            throw ex;
        }
    }
}
