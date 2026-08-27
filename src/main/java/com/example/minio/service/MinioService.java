package com.example.minio.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void init() {
        log.info("Cek ketersediaan bucket '{}'", bucketName);
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (exists) {
                log.info("Bucket '{}' sudah ada", bucketName);
            } else {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Bucket '{}' berhasil dibuat", bucketName);
            }
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Gagal menginisialisasi bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Gagal menginisialisasi bucket MinIO: " + bucketName, e);
        }
    }

    public String uploadFile(MultipartFile file) {
        log.info("Mulai upload file: {} (size={} bytes, type={})",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        if (file.isEmpty()) {
            log.warn("Upload dibatalkan, file kosong: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("File tidak boleh kosong");
        }
        String objectName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .contentType(file.getContentType())
                    .stream(is, file.getSize(), -1)
                    .build());
            log.info("Upload berhasil, objectName={} ke bucket '{}'", objectName, bucketName);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Gagal upload file '{}': {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Gagal mengupload file ke MinIO", e);
        }
        return objectName;
    }

    public InputStream downloadFile(String objectName) {
        log.info("Mulai download file, objectName={} dari bucket '{}'", objectName, bucketName);
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("Download berhasil, objectName={}", objectName);
            return response;
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Gagal download file '{}': {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Gagal mengambil file dari MinIO: " + objectName, e);
        }
    }

    public String getContentType(String objectName) {
        log.debug("Ambil content-type untuk objectName={}", objectName);
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.debug("Content-type '{}' = {}", objectName, stat.contentType());
            return stat.contentType();
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Gagal ambil metadata '{}': {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Gagal mendapatkan metadata file: " + objectName, e);
        }
    }

    public void deleteFile(String objectName) {
        log.info("Mulai hapus file, objectName={} dari bucket '{}'", objectName, bucketName);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("Hapus berhasil, objectName={}", objectName);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            log.error("Gagal hapus file '{}': {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Gagal menghapus file: " + objectName, e);
        }
    }
}
