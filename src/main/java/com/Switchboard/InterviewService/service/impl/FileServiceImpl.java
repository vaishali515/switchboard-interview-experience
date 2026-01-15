package com.Switchboard.InterviewService.service.impl;

import com.Switchboard.InterviewService.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public FileServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        log.info("FileServiceImpl :: uploadImage :: uploading image: {}", file.getOriginalFilename());

        // Random file name for uniqueness
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String key = path + "/" + fileName;

        // Create put request with public-read ACL
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        log.info("FileServiceImpl :: uploadImage :: uploading to S3 with key: {}", key);
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        // Generate permanent public URL
        String publicUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

        log.info("FileServiceImpl :: uploadImage :: completed. Public URL: {}", publicUrl);
        return publicUrl;
    }

    @Override
    public java.io.InputStream getResource(String path, String fileName) {
        log.warn("FileServiceImpl :: getResource :: not implemented");
        throw new UnsupportedOperationException("Not needed for now");
    }

    @Override
    public void deleteImage(String fileUrl) {
        log.info("FileServiceImpl :: deleteImage :: deleting image: {}", fileUrl);

        // Extract key from URL
        String key = fileUrl.substring(fileUrl.indexOf(".com/") + 5);

        s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
        log.info("FileServiceImpl :: deleteImage :: deleted image from S3: {}", key);
    }
}
