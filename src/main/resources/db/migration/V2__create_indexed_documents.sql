CREATE TABLE indexed_documents (
    document_id text NOT NULL,
    index_version integer NOT NULL,
    PRIMARY KEY (document_id, index_version)
);
