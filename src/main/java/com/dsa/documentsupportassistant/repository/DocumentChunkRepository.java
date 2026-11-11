package com.dsa.documentsupportassistant.repository;

import com.dsa.documentsupportassistant.dto.DocumentChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(DocumentChunk chunk, int indexVersion, float[] embedding) {
        jdbcTemplate.update("""
                INSERT INTO document_chunks (
                    document_id, filename, page_number, chunk_index,
                    index_version, chunk_text, embedding
                )
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS vector))
                ON CONFLICT (document_id, index_version, page_number, chunk_index)
                DO UPDATE SET filename = EXCLUDED.filename,
                              chunk_text = EXCLUDED.chunk_text,
                              embedding = EXCLUDED.embedding
                """,
                chunk.documentId(),
                chunk.filename(),
                chunk.pageNumber(),
                chunk.chunkIndex(),
                indexVersion,
                chunk.text(),
                Arrays.toString(embedding));
    }
}
