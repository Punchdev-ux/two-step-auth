package com.example.two_step_auth;

// ------------------- START OF CORRECTIONS -------------------

// DELETED: import android.se.omapi.Session;
// DELETED: import java.net.Authenticator;
// DELETED: import java.net.PasswordAuthentication;

// ADDED: Correct imports for JavaMail

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

// -------------------- END OF CORRECTIONS --------------------


public class EmailSender {

    public static void sendEmail(String toEmail, String otp) {
        // IMPORTANT: For security, never hardcode credentials in a real app.
        // This is for testing purposes only.
        final String username = "youremail@gmail.com"; // Replace with your actual Gmail address
        final String password = "your_app_password";   // Replace with your 16-digit Gmail App Password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Now this will work because 'Session' is from 'javax.mail.Session'
        Session session = Session.getInstance(props,
                new Authenticator() { // This is now 'javax.mail.Authenticator'
                    protected PasswordAuthentication getPasswordAuthentication() {
                        // This is now 'javax.mail.PasswordAuthentication'
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            // This will work because 'session' is the correct type
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail)
            );
            message.setSubject("Your OTP Code");
            message.setText("Your verification OTP is: " + otp);

            Transport.send(message);

        } catch (MessagingException e) {
            // It's better to log the error for debugging
            android.util.Log.e("EmailSender", "Failed to send email", e);
            e.printStackTrace();
        }
    }
}