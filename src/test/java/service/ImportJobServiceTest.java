package service;

import com.dsa.documentsupportassistant.dto.ImportJobStatus;
import com.dsa.documentsupportassistant.service.ImportJobService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportJobServiceTest {
    private final ImportJobService service = new ImportJobService();
    String user = "user";
    String user2 = "user2";

    @Test
    void findsSuccessfulJob() {
        var job = service.findByIdForOwner("JOB-101", user).orElseThrow();

        assertEquals(ImportJobStatus.SUCCEEDED, job.status());
        assertEquals("products.csv", job.filename());
    }

    @Test
    void findsFailedJobWithErrorDetails() {
        assertTrue(service.findByIdForOwner("JOB-102", "user").isEmpty());
    }

    @Test
    void findsRunningJob() {
        var job = service.findByIdForOwner("JOB-103", user).orElseThrow();

        assertEquals(ImportJobStatus.RUNNING, job.status());
    }

    @Test
    void returnsEmptyForUnknownJob() {
        assertTrue(service.findByIdForOwner("JOB-999", "xyz").isEmpty());
    }

    @Test
    void ownerCanReadFailedJobWithErrorDetails() {
        var job = service.findByIdForOwner("JOB-102", "user2")
                .orElseThrow();

        assertEquals(ImportJobStatus.FAILED, job.status());
        assertEquals("Missing required column: sku", job.errorDetails());
    }
}
