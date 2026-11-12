package com.dsa.documentsupportassistant;

import com.dsa.documentsupportassistant.service.DocumentIndexingService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentIndexingDemo {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: DocumentIndexingDemo <pdf-path> <owner>");
        }
        Path pdfPath = Path.of(args[0]);

        try (var context = new SpringApplicationBuilder(DocumentSupportAssistantApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {
            String documentId = context.getBean(DocumentIndexingService.class).index(pdfPath, args[1]);
            System.out.println("Document ID: " + documentId);
        }
    }
}
