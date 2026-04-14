package org.example

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

fun main(args: Array<String>) {
    if (args.size < 3) {
        throw IllegalArgumentException("Malformed arguments. Try again")
    }

    val serverHost = args[0]
    val serverPort = args[1].toInt()
    val fileName = args[2]


    println("Connecting to $serverHost:$serverPort...")
    try {
        val server = Socket(serverHost, serverPort)
        val toRead = BufferedReader(InputStreamReader(server.getInputStream()))
        val toSend = PrintWriter(server.getOutputStream())

        val req = getMessage(serverHost, serverPort, fileName)
        println("Sending $req")
        toSend.print(req)
        toSend.flush()

        println("Awaiting response...")
        var line: String?
        while (toRead.readLine().also { line = it } != null) {
            println(line)
        }
        server.close()
    } catch (e: Exception) {
        println(e.message)
    }
}


fun getMessage(serverHost: String, serverPort: Int, filePath: String): String {
    return buildString {
        append("GET /$filePath HTTP/1.1\r\n")
        append("Host: $serverHost:$serverPort\r\n")
        append("Connection: close\r\n")
        append("\r\n")
    }
}