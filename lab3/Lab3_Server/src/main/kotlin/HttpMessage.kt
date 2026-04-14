package org.example

import java.io.BufferedReader
import java.io.File
import java.io.OutputStream

class HttpRequestMessage(clientReader: BufferedReader) {
    var requestedFile: String
    var headers: Map<String, String>

    init {
        val readStartLine = clientReader.readLine().split(" ")
        if (readStartLine[0] != "GET") {
            throw IllegalArgumentException("Unsupported request type")
        }
        requestedFile = readStartLine[1]

        val readingHeaders = mutableMapOf<String, String>()
        while (true) {
            val header = clientReader.readLine()
            if (header.isEmpty()) {
                break
            }
            val splitHeader = header.split(":")
            if (splitHeader.isEmpty()) {
                throw IllegalArgumentException("Header line $header is malformed")
            }
            readingHeaders[splitHeader[0].trim()] = splitHeader[1].trim()
        }
        headers = readingHeaders.toMap()
    }

    fun respond(outputStream: OutputStream) {
        val path = File("./src/www" + requestedFile)
        val response = HttpResponseMessage(path, outputStream)
        response.send()
    }
}

class HttpResponseMessage(val filePath: File, val outputStream: OutputStream) {
    private var headers = mutableMapOf<String, String>()
    private lateinit var responseData: ByteArray

    fun send() {
        val httpMessage = makeResponseMessage()
        logger.info { "Formed response: $httpMessage" }
        outputStream.write(httpMessage.toByteArray())
        outputStream.write(responseData)
        outputStream.flush()
    }

    private fun makeResponseMessage(): String {
        val bytes = getFile()
        if (bytes == null) {
            val body = "<html><body><h1>404 Not Found</h1></body></html>"
            responseData = body.toByteArray(Charsets.UTF_8)
            headers["Content-Type"] = "text/html; charset=utf-8"
            headers["Content-Length"]= responseData.size.toString()
            return buildString {
                append("HTTP/1.1 404 Not Found\r\n")
                headers.forEach { (key, value) ->
                    append("$key: $value\r\n")
                }
                append("\r\n")
            }
        } else {
            responseData = bytes
            getContentType()
            headers["Content-Length"] = responseData.size.toString()
            return buildString {
                append("HTTP/1.1 200 OK\r\n")
                headers.forEach { (key, value) ->
                    append("$key: $value\r\n")
                }
                append("\r\n")
            }
        }

    }

    private fun getFile(): ByteArray? {
        logger.info { "Trying to reach $filePath" }
        if (filePath.exists()) {
            return filePath.readBytes()
        }
        logger.info { "No such file found" }
        return null
    }

    private fun getContentType() {
        val contentType = when (filePath.extension) {
            "txt" -> "text/plain; charset=utf-8"
            "html" -> "text/html; charset=utf-8"
            "css" -> "text/css"
            "json" -> "application/json"
            "jpeg" -> "image/jpeg"
            "jpg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            else -> "application/octet-stream"
        }
        headers["Content-Type"] = contentType
    }
}