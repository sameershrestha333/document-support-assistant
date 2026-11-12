package com.dsa.documentsupportassistant.tool;

import com.dsa.documentsupportassistant.dto.ImportJob;
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

    public ImportJobTools(ImportJobService importJobService) {
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
                .map(job -> "File: " + job.filename()
                        + "; status: " + job.status()
                        + (job.errorDetails() == null
                        ? ""
                        : "; error: " + job.errorDetails()))
                .orElse("No import job found for that ID.");

        int maxLength = 500;
        return result.length() <= maxLength
                ? result
                : result.substring(0, maxLength - 3) + "...";
    }
}
