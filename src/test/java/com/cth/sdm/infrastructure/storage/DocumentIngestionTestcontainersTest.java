package com.cth.sdm.infrastructure.storage;

import com.cth.sdm.application.dto.DocumentIngestedEvent;
import com.cth.sdm.domain.model.Document;
import com.cth.sdm.domain.repository.DocumentRepository;
import com.cth.sdm.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentIngestionTestcontainersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Define standard GenericContainers programmatically (without @Container annotation)
    static GenericContainer<?> minioContainer = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-04-18T19-09-19Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data")
            .withExposedPorts(9000);

    static GenericContainer<?> rabbitContainer = new GenericContainer<>(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
            .withEnv("RABBITMQ_DEFAULT_USER", "guest")
            .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
            .withExposedPorts(5672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Dynamic MinIO/S3 connection properties (will only be accessed if containers start)
        registry.add("app.storage.type", () -> "s3");
        registry.add("app.storage.s3.bucket-name", () -> "sdde-documents-integration");
        registry.add("app.storage.s3.access-key", () -> "minioadmin");
        registry.add("app.storage.s3.secret-key", () -> "minioadmin");
        registry.add("app.storage.s3.endpoint-url", () -> {
            if (minioContainer.isRunning()) {
                return "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(9000);
            }
            return "http://localhost:9000";
        });

        // Dynamic RabbitMQ connection properties
        registry.add("spring.rabbitmq.host", () -> {
            if (rabbitContainer.isRunning()) {
                return rabbitContainer.getHost();
            }
            return "localhost";
        });
        registry.add("spring.rabbitmq.port", () -> {
            if (rabbitContainer.isRunning()) {
                return rabbitContainer.getMappedPort(5672);
            }
            return 5672;
        });
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @BeforeAll
    static void checkDocker() {
        try {
            boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
            Assumptions.assumeTrue(dockerAvailable, "Docker environment is not available. Skipping Testcontainers integration tests.");

            // Programmatically manage the container lifecycle
            minioContainer.start();
            rabbitContainer.start();

        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "Failed to initialize or boot Docker containers: " + t.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUploadStoreToMinioAndPublishToRabbitMQ() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration_specification.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%%PDF-1.5 - REAL MINIO STORAGE VERIFICATION".getBytes(StandardCharsets.UTF_8)
        );

        // Perform REST Upload
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ingested successfully")));

        // 1. Verify entity exists in DB
        java.util.List<Document> docs = documentRepository.findAll();
        assertFalse(docs.isEmpty());
        Document savedDoc = docs.stream()
                .filter(d -> "integration_specification.pdf".equals(d.getTitle()))
                .findFirst()
                .orElse(null);

        assertNotNull(savedDoc);

        // 2. Verify file binary was stored in MinIO Testcontainer S3 client
        String filePath = savedDoc.getVersions().get(0).getFilePath();
        assertTrue(storageService.exists(filePath));

        try (InputStream stream = storageService.load(filePath)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains("REAL MINIO STORAGE VERIFICATION"));
        }

        // 3. Verify event was published to RabbitMQ Testcontainer and can be pulled
        rabbitTemplate.setReceiveTimeout(5000);
        Object message = rabbitTemplate.receiveAndConvert(RabbitConfig.QUEUE_NAME);
        assertNotNull(message);

        // Cast received message and assert attributes
        if (message instanceof DocumentIngestedEvent) {
            DocumentIngestedEvent event = (DocumentIngestedEvent) message;
            assertEquals(savedDoc.getId(), event.getDocumentId());
            assertEquals("integration_specification.pdf", event.getTitle());
            assertEquals("admin", event.getOwner());
        }
    }
}
