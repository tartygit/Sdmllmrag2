package com.cth.sdm.util;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Component
public class StorageService {

    private final String localUploadDir;
    private final String s3Endpoint;
    private final String s3AccessKey;
    private final String s3SecretKey;
    private final String s3Bucket;
    private final boolean s3Enabled;
    private MinioClient minioClient;

    public StorageService(
            @Value("${app.upload.dir:/tmp/sdde_uploads}") String uploadDir,
            @Value("${app.s3.endpoint:http://localhost:9000}") String s3Endpoint,
            @Value("${app.s3.access-key:minioadmin}") String s3AccessKey,
            @Value("${app.s3.secret-key:minioadminpassword}") String s3SecretKey,
            @Value("${app.s3.bucket:sdde-bucket}") String s3Bucket,
            @Value("${app.s3.enabled:false}") boolean s3Enabled) {

        this.localUploadDir = uploadDir;
        this.s3Endpoint = s3Endpoint;
        this.s3AccessKey = s3AccessKey;
        this.s3SecretKey = s3SecretKey;
        this.s3Bucket = s3Bucket;
        this.s3Enabled = s3Enabled;

        File dir = new File(localUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (s3Enabled) {
            try {
                this.minioClient = MinioClient.builder()
                        .endpoint(s3Endpoint)
                        .credentials(s3AccessKey, s3SecretKey)
                        .build();

                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(s3Bucket).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(s3Bucket).build());
                }
            } catch (Exception e) {
                // Fail-secure / local fallback logging
                this.minioClient = null;
            }
        }
    }

    public String storeFile(InputStream inputStream, String originalFilename) {
        try {
            String fileExtension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".txt";
            String storageFilename = UUID.randomUUID().toString() + fileExtension;

            // Store to local fallback folder
            File targetFile = new File(localUploadDir + File.separator + storageFilename);
            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // S3 object storage upload if enabled and connected
            if (s3Enabled && minioClient != null) {
                try (InputStream uploadStream = new java.io.FileInputStream(targetFile)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(s3Bucket)
                                    .object(storageFilename)
                                    .stream(uploadStream, targetFile.length(), -1)
                                    .contentType("application/octet-stream")
                                    .build()
                    );
                    return "s3://" + s3Bucket + "/" + storageFilename;
                } catch (Exception e) {
                    // fall back to returning local filepath path
                    return targetFile.getAbsolutePath();
                }
            }
            return targetFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }
}
