package com.dsa.documentsupportassistant;

import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import com.dsa.documentsupportassistant.service.EmbeddingService;
import com.dsa.documentsupportassistant.util.PdfTextExtractor;
import com.dsa.documentsupportassistant.util.TextChunker;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentIndexingDemo {

    public static void main(String[] args) throws IOException {
        try (var context = new SpringApplicationBuilder(
                DocumentSupportAssistantApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            var embeddingService = context.getBean(EmbeddingService.class);
            var repository = context.getBean(DocumentChunkRepository.class);


            var pdfPath = Path.of("docs.samples", "sample-input-file.pdf");
            var pages = new PdfTextExtractor().extract(pdfPath, "sample-001");
            var chunker = new TextChunker();

            for (var page : pages) {
                for (var chunk : chunker.split(page)) {
                    float[] embedding = embeddingService.embed(chunk.text());
                    repository.save(chunk, 1, embedding);
                    System.out.printf("Saved page %d, chunk %d%n",
                            chunk.pageNumber(), chunk.chunkIndex());
                }
            }
        }
    }
}
