package com.dsa.documentsupportassistant;

import com.dsa.documentsupportassistant.service.DocumentIndexingService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentIndexingDemo {
    public static void main(String[] args) throws IOException {
        Path pdfPath = args.length == 0
                ? Path.of("docs.samples", "import-guide.pdf")
                : Path.of(args[0]);

        try (var context = new SpringApplicationBuilder(DocumentSupportAssistantApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {
            String documentId = context.getBean(DocumentIndexingService.class).index(pdfPath);
            System.out.println("Document ID: " + documentId);
        }
    }
}
