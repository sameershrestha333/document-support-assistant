package service;

import com.dsa.documentsupportassistant.dto.ImportJobStatus;
import com.dsa.documentsupportassistant.service.ImportJobService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportJobServiceTest {
    private final ImportJobService service = new ImportJobService();

    @Test
    void findsSuccessfulJob() {
        var job = service.findById("JOB-101").orElseThrow();

        assertEquals(ImportJobStatus.SUCCEEDED, job.status());
        assertEquals("products.csv", job.filename());
    }

    @Test
    void findsFailedJobWithErrorDetails() {
        var job = service.findById("JOB-102").orElseThrow();

        assertEquals(ImportJobStatus.FAILED, job.status());
        assertEquals("Missing required column: sku", job.errorDetails());
    }

    @Test
    void findsRunningJob() {
        var job = service.findById("JOB-103").orElseThrow();

        assertEquals(ImportJobStatus.RUNNING, job.status());
    }

    @Test
    void returnsEmptyForUnknownJob() {
        assertTrue(service.findById("JOB-999").isEmpty());
    }
}
