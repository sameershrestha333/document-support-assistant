package com.dsa.documentsupportassistant.repository;

import com.dsa.documentsupportassistant.dto.DocumentChunk;
import com.dsa.documentsupportassistant.dto.RetrievedChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class DocumentChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean registerIndex(String documentId, int indexVersion) {
        return jdbcTemplate.update("""
                INSERT INTO indexed_documents (document_id, index_version)
                VALUES (?, ?)
                ON CONFLICT (document_id, index_version) DO NOTHING
                """, documentId, indexVersion) == 1;
    }

    public void grantAccess(String documentId, int indexVersion, String owner) {
        jdbcTemplate.update("""
                INSERT INTO document_access (document_id, index_version, owner_username)
                VALUES (?, ?, ?) ON CONFLICT DO NOTHING
                """, documentId, indexVersion, owner);
    }

    public boolean hasAccess(String documentId, int indexVersion, String owner) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM document_access
                    WHERE document_id = ? AND index_version = ? AND owner_username = ?)
                """, Boolean.class, documentId, indexVersion, owner));
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


    public List<RetrievedChunk> findSimilar(
            String documentId, int indexVersion, float[] questionEmbedding, int limit, String owner) {

        String sql = """
            SELECT filename, page_number, chunk_index, chunk_text,
                   embedding <=> CAST(? AS vector) AS distance
            FROM document_chunks
            WHERE document_id = ? AND index_version = ?
              AND EXISTS (
                  SELECT 1 FROM indexed_documents d
                  WHERE d.document_id = document_chunks.document_id
                    AND d.index_version = document_chunks.index_version
              )
              AND (CAST(? AS text) IS NULL OR EXISTS (
                  SELECT 1 FROM document_access a
                  WHERE a.document_id = document_chunks.document_id
                    AND a.index_version = document_chunks.index_version
                    AND a.owner_username = ?
              ))
            ORDER BY distance ASC, page_number ASC, chunk_index ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RetrievedChunk(
                rs.getString("filename"),
                rs.getInt("page_number"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                rs.getDouble("distance")
        ), Arrays.toString(questionEmbedding), documentId, indexVersion, owner, owner, limit);
    }

}
