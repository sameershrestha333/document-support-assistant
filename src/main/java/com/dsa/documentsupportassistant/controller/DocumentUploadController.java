package com.dsa.documentsupportassistant.controller;

import com.dsa.documentsupportassistant.service.DocumentUploadService;
import com.dsa.documentsupportassistant.service.DocumentUploadService.UploadStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/uploads")
public class DocumentUploadController {
    private final DocumentUploadService uploads;

    public DocumentUploadController(DocumentUploadService uploads) {
        this.uploads = uploads;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadStatus> upload(@RequestParam("file") MultipartFile file,
                                               @RequestParam(name = "replacesDocumentId", required = false) String replacesDocumentId,
                                               Principal principal) throws IOException {
        var job = uploads.submit(file, principal.getName(), replacesDocumentId);
        return ResponseEntity.accepted().location(URI.create("/api/documents/uploads/" + job.jobId())).body(job);
    }

    @GetMapping("/{jobId}")
    public UploadStatus status(@PathVariable UUID jobId, Principal principal) {
        return uploads.status(jobId, principal.getName());
    }
}
