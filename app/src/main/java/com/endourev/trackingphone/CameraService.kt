package com.endourev.trackingphone

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Properties
import javax.mail.*
import javax.mail.internet.*
class CameraService : Service() {

    private var lastLat = "unknown"
    private var lastLon = "unknown"
    private lateinit var fusedClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                lastLat = "%.4f".format(loc.latitude)
                lastLon = "%.4f".format(loc.longitude)
            }
            takePhoto()
        }.addOnFailureListener {
            takePhoto()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "intruder_cam"
        val channel = NotificationChannel(
            channelId, "IntruderCam",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Keamanan aktif")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        startForeground(1, notif)
    }

    private fun takePhoto() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    FakeLifecycleOwner(), cameraSelector, imageCapture
                )
                val photoFile = File(
                    getExternalFilesDir(null),
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(Date()) + "_lat${lastLat}_lon${lastLon}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            showIntruderNotification()
                            sendEmail(photoFile)
                            stopSelf()
                        }
                        override fun onError(exc: ImageCaptureException) {
                            stopSelf()
                        }
                    }
                )
            } catch (e: Exception) {
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showIntruderNotification() {
        val channelId = "intruder_alert"
        val channel = NotificationChannel(
            channelId, "Intruder Alert",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ SOMEONE TRYNNA OPEN UR FUVKCING PHONE!!")
            .setContentText("Lokasi: $lastLat, $lastLon")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(2, notif)
    }

    private fun sendEmail(photoFile: File) {
        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val senderEmail = BuildConfig.EMAIL_SENDER
                val senderPass = BuildConfig.EMAIL_PASSWORD
                val receiverEmail = BuildConfig.EMAIL_RECEIVER

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(senderEmail, senderPass)
                })

                val msg = MimeMessage(session).apply {
                    setFrom(InternetAddress(senderEmail))
                    setRecipient(Message.RecipientType.TO, InternetAddress(receiverEmail))
                    subject = "⚠️ Intruder Detected!"
                    val multipart = MimeMultipart()
                    val textPart = MimeBodyPart().apply {
                        setText("Ada yang coba buka HP!\nLokasi: $lastLat, $lastLon")
                    }
                    val photoPart = MimeBodyPart().apply {
                        attachFile(photoFile)
                        fileName = photoFile.name

                    }
                    multipart.addBodyPart(textPart)
                    multipart.addBodyPart(photoPart)
                    setContent(multipart)
                }

                Transport.send(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}