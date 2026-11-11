package com.dsa.documentsupportassistant.util;

import java.io.IOException;
import java.nio.file.Path;

public class PdfExtractionDemo {
    public static void main(String[] args) throws IOException {
        Path pdfPath = Path.of("docs.samples", "sample-input-file.pdf");
        var pages = new PdfTextExtractor().extract(pdfPath, "sample-001");

        for (var page : pages) {
            System.out.println(page.documentId() + " | "
                    + page.filename() + " | page " + page.pageNumber());
            System.out.println(page.text());
        }
    }
}
