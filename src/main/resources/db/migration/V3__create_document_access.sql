CREATE TABLE document_access (
    document_id text NOT NULL,
    index_version integer NOT NULL,
    owner_username text NOT NULL,
    PRIMARY KEY (document_id, index_version, owner_username),
    FOREIGN KEY (document_id, index_version)
        REFERENCES indexed_documents (document_id, index_version) ON DELETE CASCADE
);
