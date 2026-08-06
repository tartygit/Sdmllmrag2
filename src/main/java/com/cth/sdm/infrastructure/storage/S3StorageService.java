package com.cth.sdm.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;

@Service("s3StorageService")
@Slf4j
public class S3StorageService implements StorageService {

    @Value("${app.storage.s3.bucket-name:sdde-documents}")
    private String bucketName;

    @Value("${app.storage.s3.access-key:minioadmin}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key:minioadmin}")
    private String secretKey;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    @Value("${app.storage.s3.endpoint-url:http://localhost:9000}")
    private String endpointUrl;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            S3ClientBuilder builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true) // Crucial for MinIO compatibility
                            .build());

            if (endpointUrl != null && !endpointUrl.isBlank()) {
                builder.endpointOverride(URI.create(endpointUrl));
            }

            this.s3Client = builder.build();
            log.info("Initialized S3Client with endpoint: {}", endpointUrl);

            // Ensure the bucket exists (Non-blocking warning if fails)
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Created S3 bucket: {}", bucketName);
            } catch (BucketAlreadyExistsException e) {
                log.info("S3 Bucket already exists: {}", bucketName);
            } catch (S3Exception e) {
                if (e.statusCode() == 409) {
                    log.info("S3 Bucket already exists: {}", bucketName);
                } else {
                    log.warn("S3 Bucket check warning: {}. Continuing anyway.", e.getMessage());
                }
            } catch (Exception e) {
                log.warn("S3 Bucket check failed: {}. Continuing anyway.", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to initialize S3Client: {}", e.getMessage());
        }
    }

    @Override
    public void store(String path, InputStream stream, long size, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
            log.info("Stored file to S3: bucket={}, key={}", bucketName, path);
        } catch (Exception e) {
            log.error("Failed to store file in S3: {}", e.getMessage());
            throw new RuntimeException("S3 store failed", e);
        }
    }

    @Override
    public InputStream load(String path) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getObjectRequest);
            return s3Stream;
        } catch (Exception e) {
            log.error("Failed to load file from S3: {}", e.getMessage());
            throw new RuntimeException("S3 load failed", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted file from S3: {}", path);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to check if file exists in S3: {}", e.getMessage());
            return false;
        }
    }
}
