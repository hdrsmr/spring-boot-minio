# Spring Boot + MinIO File Upload/Download

Project Spring Boot untuk upload dan read (download) file menggunakan **MinIO** sebagai object storage, dengan **Swagger UI** untuk mencoba API-nya. Seluruh stack (MinIO + aplikasi) dijalankan via **Docker Compose**.

## Fitur
- Upload file (multipart) ke MinIO bucket
- Read / download file dari MinIO (stream)
- Delete file
- Dokumentasi & testing API via Swagger UI
- Auto-create bucket saat aplikasi start
- Logging setiap method via Log4j2 (Lombok `@Slf4j`)

## Struktur
```
src/main/java/com/example/minio/
├── MinioApplication.java
├── config/MinioConfig.java        # Bean MinioClient + @Slf4j
├── config/MinioProperties.java    # Config properties (@Component + @ConfigurationProperties)
├── service/MinioService.java      # Logika upload/download/delete + logging
└── controller/FileController.java # REST API + Swagger + logging
src/main/resources/
├── application.yml                # Konfigurasi (port 4444, minio)
└── log4j2.xml                     # Konfigurasi logger (format + method trace)
Dockerfile                         # Multi-stage build (maven -> jdk17)
.dockerignore
docker-compose.yml                 # Service app + minio
```

## Cara Menjalankan (Docker Compose — satu perintah)

```bash
docker compose up -d
```

Perintah di atas akan:
1. Menjalankan **MinIO** (port `9000` API, `9001` console)
2. Build & menjalankan **aplikasi Spring Boot** (port `4444`) — menunggu MinIO `healthy` dulu

Akses:
- Swagger UI: http://localhost:4444/swagger-ui.html
- MinIO Console: http://localhost:9001 (user: `minioadmin` / pass: `minioadmin`)

Perintah lain:
```bash
docker compose ps        # cek status container
docker compose logs -f app   # lihat log aplikasi
docker compose down      # hentikan & hapus container
```

## Cara Menjalankan (Tanpa Docker, lokal)

### 1. Jalankan MinIO
```bash
docker compose up -d minio
```

### 2. Jalankan Aplikasi Spring Boot
```bash
mvn spring-boot:run
```
atau build dulu:
```bash
mvn clean package
java -jar target/spring-boot-minio-1.0.0.jar
```
Aplikasi berjalan di port **4444** (lihat `server.port` di `application.yml`).

### 3. Coba API lewat Swagger
Buka: http://localhost:4444/swagger-ui.html

## Endpoint API

| Method | Path                  | Keterangan                              |
|--------|-----------------------|-----------------------------------------|
| POST   | `/api/files/upload`   | Upload file (`form-data`, key: `file`)  |
| GET    | `/api/files/{objectName}` | Read/download file                    |
| DELETE | `/api/files/{objectName}` | Hapus file                           |

### Contoh via curl
Upload:
```bash
curl -F "file=@./contoh.jpg" http://localhost:4444/api/files/upload
```
Response:
```json
{ "objectName": "uuid-contoh.jpg", "originalName": "contoh.jpg", "size": 12345 }
```

Download (pakai `objectName` dari response upload):
```bash
curl -O -J "http://localhost:4444/api/files/uuid-contoh.jpg"
```

## Konfigurasi
Edit `src/main/resources/application.yml`:
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: files
  region: us-east-1
```

Saat dijalankan lewat Docker Compose, nilai di atas di-override oleh environment variable
(`MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET_NAME`, `MINIO_REGION`)
pada service `app` di `docker-compose.yml`, di mana `MINIO_ENDPOINT` menunjuk ke
`http://minio:9000` (nama service docker).

## Teknologi
- Java 17
- Spring Boot 3.2
- MinIO Java SDK 8.5
- springdoc-openapi (Swagger UI 2.3)
- Log4j2 (via `spring-boot-starter-log4j2`, log melalui Lombok `@Slf4j`)
- Docker / Docker Compose

## Logging (Log4j2)
Setiap method di `MinioService` dan `FileController` menulis trace ke logger.
Format log (`src/main/resources/log4j2.xml`) sudah menyertakan nama method (`%M`):
```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL logger.method() - pesan
```
Level default `com.example.minio` = `debug`.
