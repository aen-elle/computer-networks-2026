package org.example

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.Base64

private const val SENDER_EMAIL = "test_sender_email@example.com"
private const val IMAGE_PATH = "img/raduga.jpg"

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        throw IllegalArgumentException("Email was not specified.")
    }
    val email = args[0]
    val type = if (args.size > 1) args[1] else "text"

    println("Connecting to localhost:1025...")
    val server = Socket("localhost", 1025)
    val toRead = BufferedReader(InputStreamReader(server.getInputStream()))
    val toSend = PrintWriter(server.getOutputStream())

    connect(toRead, toSend)
    fromToHeaders(email, toRead, toSend)
    if (type == "text") {
        sendTextData(email, toRead, toSend)
        val body = "This is a sample text message, sent from custom SMTP client!"
        sendMessage(body, toRead, toSend)
    } else if (type == "html") {
        sendHtmlData(email, toRead, toSend)
        val body = "<body> <p> This is a sample <b> HTML </b> message, sent from custom <i>SMTP</i> client! </p> </body>"
        sendMessage(body, toRead, toSend)
    } else {
        sendMultipartData(email, toRead, toSend)
    }
    server.close()
}

private fun connect(reader: BufferedReader, writer: PrintWriter) {
    val response = reader.readLine()
    println("Received: $response")
    checkResponse(response, 220)

    println("Sending HELO...")
    writer.writeAndFlush("HELO smtp-client.local")

    val ehloResponse = reader.readLine()
    println("Received: $ehloResponse")
    checkResponse(ehloResponse, 250)
}

private fun fromToHeaders(receiverEmail: String, reader: BufferedReader, writer: PrintWriter) {
    writer.writeAndFlush("MAIL FROM:<$SENDER_EMAIL>")

    val senderResponse = reader.readLine()
    println("Received: $senderResponse")
    checkResponse(senderResponse, 250)

    writer.writeAndFlush("RCPT TO:<$receiverEmail>")

    val receiverResponse = reader.readLine()
    println("Received: $receiverResponse")
    checkResponse(receiverResponse, 250)
}

private fun sendTextData(receiverEmail: String, reader: BufferedReader, writer: PrintWriter) {
    writer.writeAndFlush("DATA")

    val dataResp = reader.readLine()
    println("Received: $dataResp")
    checkResponse(dataResp, 354)

    writer.writeAndFlush("From: $SENDER_EMAIL")
    writer.writeAndFlush("To: $receiverEmail")
    writer.writeAndFlush("Subject: Test Text subject")
    writer.writeAndFlush()
}

private fun sendHtmlData(receiverEmail: String, reader: BufferedReader, writer: PrintWriter) {
    writer.writeAndFlush("DATA")

    val dataResp = reader.readLine()
    println("Received: $dataResp")
    checkResponse(dataResp, 354)


    writer.writeAndFlush("From: $SENDER_EMAIL")
    writer.writeAndFlush("To: $receiverEmail")
    writer.writeAndFlush("Subject: Test HTML subject")
    writer.writeAndFlush("Content-Type: text/html")
    writer.writeAndFlush()
}

private fun sendMultipartData(receiverEmail: String, reader: BufferedReader, writer: PrintWriter) {
    val boundary = "ts_${System.currentTimeMillis()}"
    val imgCid = "rainbow_${System.currentTimeMillis()}@example.com"

    writer.writeAndFlush("DATA")

    writer.writeAndFlush("From: $SENDER_EMAIL")
    writer.writeAndFlush("To: $receiverEmail")
    writer.writeAndFlush("Subject: Test HTML with image subject")
    writer.writeAndFlush("MIME-Version: 1.0")
    writer.writeAndFlush("Content-Type: multipart/related; boundary=\"$boundary\"")
    writer.writeAndFlush()

    sendTextPart(writer, boundary, imgCid)
    sendImagePart(writer, boundary, imgCid)
}

private fun sendTextPart(writer: PrintWriter, boundary: String, imgCid: String) {
    writer.writeAndFlush("--$boundary")
    writer.writeAndFlush("Content-Type: text/html; charset=utf-8")
    writer.writeAndFlush()
    writer.writeAndFlush("""
        <html>
        <body>
            <h1>This... is a rainbow!</h1>
            <img src="cid:$imgCid" alt="image">
        </body>
        </html>
    """.trimIndent())
    writer.writeAndFlush()
}

private fun sendImagePart(writer: PrintWriter, boundary: String, imgCid: String) {
    writer.writeAndFlush("--$boundary")

    val imgName = File(IMAGE_PATH).name

    writer.writeAndFlush("Content-Type: image/jpeg; name=\"$imgName\"")
    writer.writeAndFlush("Content-Transfer-Encoding: base64")
    writer.writeAndFlush("Content-ID: <$imgCid>")
    writer.writeAndFlush("Content-Disposition: inline; filename=\"$imgName\"")
    writer.writeAndFlush()

    val encodedImg = imageToBase64(IMAGE_PATH)

    writer.writeAndFlush(encodedImg)
    writer.writeAndFlush()

    writer.writeAndFlush("--$boundary")
    writer.writeAndFlush(".")
    writer.writeAndFlush()
}

private fun sendMessage(message: String, reader: BufferedReader, writer: PrintWriter) {
    writer.writeAndFlush(message)
    writer.writeAndFlush(".")

    val messageResp = reader.readLine()
    println("Received: $messageResp")
    checkResponse(messageResp, 250)
}

private fun checkResponse(response: String, code: Int): Boolean {
    if (response.startsWith(code.toString())) return true
    throw IllegalStateException("Server is not capable of servicing this request/")
}

fun PrintWriter.writeAndFlush(message: String = "") {
    if (message.isNotEmpty() ) this.println(message) else this.println()
    this.flush()
}

private fun imageToBase64(imgPath: String): String {
    val image = File(imgPath)
    return Base64.getEncoder().encodeToString(image.readBytes())
}