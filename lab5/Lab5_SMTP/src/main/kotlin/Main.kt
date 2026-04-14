package org.example

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.b
import kotlinx.html.body
import kotlinx.html.p
import net.axay.simplekotlinmail.delivery.MailerManager
import net.axay.simplekotlinmail.delivery.mailerBuilder
import net.axay.simplekotlinmail.delivery.send
import net.axay.simplekotlinmail.delivery.sendSync
import net.axay.simplekotlinmail.email.emailBuilder
import net.axay.simplekotlinmail.html.withHTML

const val SENDER_ADDRESS = "test_sender@example.com"
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        throw IllegalArgumentException("No email was specified")
    }
    val receiverAddress = args[0]
    val type = if (args.size >= 2) args[1] else "text"
    println(type)

    val mailer = mailerBuilder(host = "localhost", port = 1025)
    MailerManager.defaultMailer = mailer

    val email = emailBuilder {
        from(SENDER_ADDRESS)
        to(receiverAddress)

        withSubject("Test message")
        if (type == "text") {
            withPlainText(
                "This is a text message for lab5 - sent with a simple email client!"
            )
        } else {
            withHTML {
                body {
                    p {+"This is an "}
                    b { +"HTML message" }
                    p {+" for lab5 - sent with a simple email client!"}
                }
            }
        }
    }

    GlobalScope.launch {
        email.send()
    }
}