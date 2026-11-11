package com.dsa.documentsupportassistant.util;

import com.dsa.documentsupportassistant.dto.DocumentChunk;
import com.dsa.documentsupportassistant.dto.DocumentPage;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {
    private static final int TARGET_WORDS = 80;

    public List<DocumentChunk> split(DocumentPage page) {
        List<DocumentChunk> chunks = new ArrayList<>();
        List<String> pending = new ArrayList<>();
        int wordCount = 0;

        for (String block : page.text().strip().split("\\R\\h*\\R+")) {
            String paragraph = block.strip();
            if (paragraph.isEmpty()) {
                continue;
            }

            String[] words = paragraph.split("\\s+");

            if (wordCount > 0 && wordCount + words.length > TARGET_WORDS) {
                addChunk(chunks, page, String.join("\n\n", pending));
                pending.clear();
                wordCount = 0;
            }

            if (words.length > TARGET_WORDS) {
                for (int start = 0; start < words.length; start += TARGET_WORDS) {
                    int end = Math.min(start + TARGET_WORDS, words.length);
                    String text = String.join(" ", List.of(words).subList(start, end));
                    addChunk(chunks, page, text);
                }
            } else {
                pending.add(paragraph);
                wordCount += words.length;
            }
        }

        if (!pending.isEmpty()) {
            addChunk(chunks, page, String.join("\n\n", pending));
        }
        return chunks;
    }

    private void addChunk(List<DocumentChunk> chunks, DocumentPage page, String text) {
        chunks.add(new DocumentChunk(
            page.documentId(), page.filename(), page.pageNumber(), chunks.size() + 1, text
        ));
    }
}