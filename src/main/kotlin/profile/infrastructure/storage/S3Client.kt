package profile.infrastructure.storage

import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.ktor.server.config.*
import java.io.InputStream

class S3Client(config: ApplicationConfig) {
    private val endpoint = config.property("s3.endpoint").getString()
    private val accessKey = config.property("s3.access_key").getString()
    private val secretKey = config.property("s3.secret_key").getString()
    private val bucket = config.property("s3.bucket").getString()

    private val client = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    fun uploadAvatar(userId: String, inputStream: InputStream, contentType: String): String {
        val fileName = "avatars/$userId.jpg"
        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(fileName)
                .stream(inputStream, -1, 10485760) // 10MB limit
                .contentType(contentType)
                .build()
        )
        return "$endpoint/$bucket/$fileName"
    }
}
