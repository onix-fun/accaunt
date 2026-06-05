package profile.infrastructure.jwt

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object RsaKeyLoader {
    fun loadPrivateKey(path: String): RSAPrivateKey {
        val bytes = decodePem(path, "PRIVATE KEY")
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes)) as RSAPrivateKey
    }

    fun loadPublicKey(path: String): RSAPublicKey {
        val bytes = decodePem(path, "PUBLIC KEY")
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes)) as RSAPublicKey
    }

    private fun decodePem(path: String, type: String): ByteArray {
        val pem = Files.readString(Path.of(path))
        val encoded = pem
            .replace("-----BEGIN $type-----", "")
            .replace("-----END $type-----", "")
            .replace(Regex("\\s"), "")
        require(encoded.isNotBlank()) { "RSA $type PEM is empty: $path" }
        return Base64.getDecoder().decode(encoded)
    }
}
