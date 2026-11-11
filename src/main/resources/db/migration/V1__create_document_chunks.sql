CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE document_chunks (
    document_id   text         NOT NULL,
    filename      text         NOT NULL,
    page_number   integer      NOT NULL,
    chunk_index   integer      NOT NULL,
    index_version integer      NOT NULL,
    chunk_text    text         NOT NULL,
    embedding     vector(1536) NOT NULL,
    PRIMARY KEY (document_id, index_version, page_number, chunk_index)
);