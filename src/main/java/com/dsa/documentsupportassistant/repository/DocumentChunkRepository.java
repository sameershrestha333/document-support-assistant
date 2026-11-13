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

    public List<RetrievedChunk> findHybrid(String documentId, int indexVersion,
                                          float[] embedding, String question, String owner) {
        return jdbcTemplate.query("""
                WITH scope AS NOT MATERIALIZED (
                    SELECT c.*
                    FROM document_chunks c
                    JOIN indexed_documents d USING (document_id, index_version)
                    JOIN document_access a USING (document_id, index_version)
                    WHERE c.document_id = ? AND c.index_version = ? AND a.owner_username = ?
                ),
                query AS (
                    SELECT CAST(? AS vector) AS embedding,
                           COALESCE((
                               SELECT to_tsquery('simple', string_agg(quote_literal(term), ' | '))
                               FROM unnest(tsvector_to_array(to_tsvector('simple', ?))) AS terms(term)
                           ), CAST('' AS tsquery)) AS keywords
                ),
                vector_hits AS (
                    SELECT page_number, chunk_index, row_number() OVER (
                        ORDER BY s.embedding <=> q.embedding, page_number, chunk_index
                    ) AS rank
                    FROM scope s CROSS JOIN query q
                    ORDER BY rank LIMIT 20
                ),
                keyword_hits AS (
                    SELECT page_number, chunk_index, row_number() OVER (
                        ORDER BY ts_rank(to_tsvector('simple', chunk_text), q.keywords) DESC,
                                 page_number, chunk_index
                    ) AS rank
                    FROM scope s CROSS JOIN query q
                    WHERE to_tsvector('simple', chunk_text) @@ q.keywords
                    ORDER BY rank LIMIT 20
                ),
                scores AS (
                    SELECT page_number, chunk_index, sum(1.0 / (60 + rank)) AS score
                    FROM (SELECT * FROM vector_hits UNION ALL SELECT * FROM keyword_hits) hits
                    GROUP BY page_number, chunk_index
                )
                SELECT filename, page_number, chunk_index, chunk_text,
                       s.embedding <=> q.embedding AS distance
                FROM scope s JOIN scores USING (page_number, chunk_index)
                CROSS JOIN query q
                ORDER BY score DESC, distance, page_number, chunk_index
                LIMIT 5
                """, (rs, rowNum) -> new RetrievedChunk(
                rs.getString("filename"), rs.getInt("page_number"), rs.getInt("chunk_index"),
                rs.getString("chunk_text"), rs.getDouble("distance")),
                documentId, indexVersion, owner, Arrays.toString(embedding), question);
    }
}
