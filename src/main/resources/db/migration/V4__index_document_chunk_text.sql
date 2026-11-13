CREATE INDEX document_chunks_full_text_idx
    ON document_chunks USING GIN (to_tsvector('simple', chunk_text));
