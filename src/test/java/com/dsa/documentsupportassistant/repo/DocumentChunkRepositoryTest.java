package com.dsa.documentsupportassistant.repo;

import com.dsa.documentsupportassistant.dto.DocumentChunk;
import com.dsa.documentsupportassistant.repository.DocumentChunkRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class DocumentChunkRepositoryTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres"));

    @Test
    void vectorAndHybridSearchOnlyReturnChunksToTheOwner() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        var repository = new DocumentChunkRepository(new JdbcTemplate(dataSource));
        String documentId = "test-upload-guide";
        int version = 1;
        String text = "The maximum upload size is 10 MB.";
        float[] embedding = new float[1536];
        embedding[0] = 1.0f;

        repository.registerIndex(documentId, version);
        repository.save(
                new DocumentChunk(documentId, "upload-guide.pdf", 2, 1, text),
                version, embedding);
        repository.grantAccess(documentId, version, "user");

        var vectorMatches = repository.findSimilar(
                documentId, version, embedding, 5, "user");
        var hybridMatches = repository.findHybrid(
                documentId, version, embedding, "upload size", "user");

        assertEquals(1, vectorMatches.size());
        assertEquals(text, vectorMatches.getFirst().text());
        assertEquals(1, hybridMatches.size());
        assertEquals(text, hybridMatches.getFirst().text());

        assertTrue(repository.findSimilar(
                documentId, version, embedding, 5, "user2").isEmpty());
        assertTrue(repository.findHybrid(
                documentId, version, embedding, "upload size", "user2").isEmpty());
    }
}
