package com.example.ebook

import android.os.AsyncTask
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailSender(
    private val username: String = "amitprasad1326@gmail.com",
    private val password: String = "arei kbwt pmxt rgoh"
) {

    private val properties: Properties
        get() {
            val props = Properties()
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = "smtp.gmail.com"
            props["mail.smtp.port"] = "587"
            return props
        }

    private val session: Session
        get() {
            return Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })
        }

    fun sendEmail(recipient: String, subject: String, body: String) {
        SendEmailTask(session, username, recipient, subject, body).execute()
    }

    private class SendEmailTask(
        private val session: Session,
        private val fromEmail: String,
        private val toEmail: String,
        private val subject: String,
        private val body: String
    ) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void?): Void? {
            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(fromEmail))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                message.subject = subject
                message.setText(body)
                Transport.send(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}


