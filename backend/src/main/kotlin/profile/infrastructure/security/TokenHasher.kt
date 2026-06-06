package profile.infrastructure.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

object TokenHasher {
    fun hash(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun challenge(secret: String, purpose: String, subjectId: String, code: String): String {
        val derived = hkdfSha256(secret.toByteArray(), purpose.toByteArray(), 32)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(derived, "HmacSHA256"))
        return mac.doFinal("$subjectId:$code".toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun hkdfSha256(ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = hmacSha256("TokenHasher-derive".toByteArray(), ikm)
        val result = mutableListOf<Byte>()
        var t = ByteArray(0)
        val n = ceil(length / 32.0).toInt()
        for (i in 1..n) {
            t = hmacSha256(prk, t + info + byteArrayOf(i.toByte()))
            result.addAll(t.toList())
        }
        return result.take(length).toByteArray()
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
