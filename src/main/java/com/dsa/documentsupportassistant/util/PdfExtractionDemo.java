package com.dsa.documentsupportassistant.util;

import com.dsa.documentsupportassistant.dto.DocumentPage;

import java.io.IOException;
import java.nio.file.Path;

public class PdfExtractionDemo {
    public static void main(String[] args) throws IOException {
        Path pdfPath = Path.of("docs.samples", "sample-input-file.pdf");
        var pages = new PdfTextExtractor().extract(pdfPath, "sample-001");
        var chunker = new TextChunker();

        for (var page : pages) {
            for (var chunk : chunker.split(page)) {
                System.out.printf("%s | %s | page %d | chunk %d%n",
                        chunk.documentId(), chunk.filename(),
                        chunk.pageNumber(), chunk.chunkIndex());
                System.out.println(chunk.text());
            }
        }

        var longPage = new DocumentPage(
                "sample-002", "long-page.pdf", 3, "word ".repeat(170)
        );

        for (var chunk : chunker.split(longPage)) {
            System.out.printf("Page %d, chunk %d: %d words%n",
                    chunk.pageNumber(),
                    chunk.chunkIndex(),
                    chunk.text().strip().split("\\s+").length);
        }
    }
}
