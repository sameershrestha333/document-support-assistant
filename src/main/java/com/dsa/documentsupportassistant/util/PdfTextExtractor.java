package com.dsa.documentsupportassistant.util;

import com.dsa.documentsupportassistant.dto.DocumentPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PdfTextExtractor {
    public List<DocumentPage> extract(Path pdfPath, String documentId) throws IOException {
        List<DocumentPage> pages = new ArrayList<>();
        String filename = pdfPath.getFileName().toString();

        try (var pdf = Loader.loadPDF(pdfPath.toFile())) {
            var stripper = new PDFTextStripper();

            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(pdf).strip();
                pages.add(new DocumentPage(documentId, filename, page, text));
            }
        }

        return pages;
    }
}
