package com.example.minio.controller;

import com.example.minio.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File API", description = "Upload, Download, dan Delete file di MinIO")
public class FileController {

    private final MinioService minioService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload file ke MinIO",
            description = "Mengupload file melalui multipart/form-data dan mengembalikan object name",
            responses = {
                @ApiResponse(responseCode = "200", description = "Upload berhasil"),
                @ApiResponse(responseCode = "400", description = "File kosong")
            })
    public ResponseEntity<UploadResponse> uploadFile(
            @Parameter(description = "File yang akan diupload", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("Request upload diterima: {}", file.getOriginalFilename());
        String objectName = minioService.uploadFile(file);
        log.info("Request upload selesai, objectName={}", objectName);
        return ResponseEntity.ok(new UploadResponse(objectName, file.getOriginalFilename(), file.getSize()));
    }

    @GetMapping("/{objectName}")
    @Operation(
            summary = "Download / Read file dari MinIO",
            description = "Mengambil file berdasarkan object name dan mengembalikannya sebagai stream",
            responses = {
                @ApiResponse(responseCode = "200", description = "File ditemukan",
                        content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
                @ApiResponse(responseCode = "404", description = "File tidak ditemukan")
            })
    public void readFile(
            @Parameter(description = "Nama object hasil upload", required = true, example = "uuid-namafile.jpg")
            @PathVariable String objectName,
            HttpServletResponse response) throws IOException {
        log.info("Request read file: {}", objectName);
        InputStream stream = minioService.downloadFile(objectName);
        String contentType = minioService.getContentType(objectName);
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Disposition", "inline; filename=\"" + objectName + "\"");
        stream.transferTo(response.getOutputStream());
        response.flushBuffer();
        log.info("Request read file selesai: {}", objectName);
    }

    @DeleteMapping("/{objectName}")
    @Operation(summary = "Hapus file di MinIO", description = "Menghapus file berdasarkan object name")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Nama object yang akan dihapus", required = true)
            @PathVariable String objectName) {
        log.info("Request delete file: {}", objectName);
        minioService.deleteFile(objectName);
        log.info("Request delete file selesai: {}", objectName);
        return ResponseEntity.noContent().build();
    }

    @Schema(name = "UploadResponse", description = "Response hasil upload")
    public record UploadResponse(
            @Schema(description = "Nama object di MinIO (simpan untuk download)") String objectName,
            @Schema(description = "Nama file asli") String originalName,
            @Schema(description = "Ukuran file dalam byte") long size) {
    }
}
