package com.dsa.documentsupportassistant.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentUploadService {
    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);
    private final DocumentIndexingService indexer;
    private final ConcurrentHashMap<UUID, Job> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8));

    public DocumentUploadService(DocumentIndexingService indexer) {
        this.indexer = indexer;
    }

    public UploadStatus submit(MultipartFile file, String owner, String replacesDocumentId) throws IOException {
        if (replacesDocumentId != null && !indexer.canReplace(replacesDocumentId, owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a nonempty PDF.");
        }
        String name = file.getOriginalFilename();
        name = name == null ? "" : name.replace((char) 92, '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF uploads are supported.");
        }
        UUID id = UUID.randomUUID();
        Path staged = Files.createTempFile("document-upload-", ".pdf");
        var pending = new UploadStatus(id, Status.PENDING, null, null);
        String displayName = name;
        try {
            file.transferTo(staged);
            jobs.put(id, new Job(owner, pending));
            worker.execute(() -> index(id, staged, owner, displayName, replacesDocumentId));
        } catch (RejectedExecutionException ex) {
            jobs.remove(id);
            cleanup(staged);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Upload queue is full.");
        } catch (IOException | RuntimeException ex) {
            jobs.remove(id);
            cleanup(staged);
            throw ex;
        }
        return pending;
    }

    public UploadStatus status(UUID id, String owner) {
        Job job = jobs.get(id);
        if (job == null || !job.owner().equals(owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload job not found.");
        }
        return job.status();
    }

    private void index(UUID id, Path staged, String owner, String displayName, String replacesDocumentId) {
        jobs.put(id, new Job(owner, new UploadStatus(id, Status.PROCESSING, null, null)));
        try {
            String documentId = replacesDocumentId == null
                    ? indexer.index(staged, owner, displayName)
                    : indexer.replace(staged, owner, displayName, replacesDocumentId);
            jobs.put(id, new Job(owner, new UploadStatus(id, Status.READY, documentId, null)));
        } catch (IOException | RuntimeException ex) {
            log.warn("Upload job {} failed ({})", id, ex.getClass().getSimpleName());
            jobs.put(id, new Job(owner, new UploadStatus(id, Status.FAILED, null,
                    "Could not index the PDF.")));
        } finally {
            cleanup(staged);
        }
    }

    private void cleanup(Path staged) {
        try {
            Files.deleteIfExists(staged);
        } catch (IOException ex) {
            log.warn("Could not remove staged upload", ex);
        }
    }

    @PreDestroy
    void stopAcceptingUploads() {
        worker.shutdown();
    }

    private record Job(String owner, UploadStatus status) {}
    public enum Status { PENDING, PROCESSING, READY, FAILED }
    public record UploadStatus(UUID jobId, Status status, String documentId, String error) {}
}
