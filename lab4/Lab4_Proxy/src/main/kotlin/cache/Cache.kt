package org.example.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class Cache {
    companion object {
        private val cacheDir = File("build/cache")
        private var instance: Cache? = null

        fun getInstance(): Cache {
            if (instance != null) {
                return instance!!
            } else {
                cacheDir.mkdirs()
                return Cache().also {
                    instance = it
                }
            }
        }
    }

    fun save(url: String, bytes: ByteArray, lastModified: String?, etag: String?,
             contentType: String?, contentEncoding: String?) {

        val key = getEntryKey(url)

        val bodyFile = File(cacheDir, "$key.data")
        val metaFile = File(cacheDir, "$key.meta")

        bodyFile.writeBytes(bytes)

        val metaData = CacheEntryMetaData(
            url = url,
            lastModified = lastModified,
            etag = etag,
            contentType = contentType,
            contentEncoding = contentEncoding
        )
        val json = Json.encodeToString(metaData)
        metaFile.writeText(json)

    }

    fun load(url: String): CacheEntry? {
        val key = getEntryKey(url)

        val bodyFile = File(cacheDir, "$key.data")
        val metaFile = File(cacheDir, "$key.meta")

        if (!bodyFile.exists() || !metaFile.exists()) return null

        val metadata = Json.decodeFromString<CacheEntryMetaData>(metaFile.readText())
        val body = bodyFile.readBytes()

        return CacheEntry(
            metaData = metadata,
            bytes = body
        )
    }

    private fun getEntryKey( url: String): String {
        return url.replace("://", "_")
        .replace("/", "_")
            .replace("?", "_")
            .replace("&", "_")
    }

}

@Serializable
data class CacheEntryMetaData(
    val url: String,
    val lastModified: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val etag: String?,
    val contentType: String?,
    val contentEncoding: String?
)

@Serializable
data class CacheEntry(
    val metaData: CacheEntryMetaData,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheEntry

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}