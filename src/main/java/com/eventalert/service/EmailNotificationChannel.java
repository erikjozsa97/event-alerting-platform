package com.eventalert.service;

import com.eventalert.exception.NotificationDeliveryException;
import com.eventalert.model.Channel;
import com.eventalert.model.ChannelType;
import com.eventalert.model.NotificationMessage;
import com.eventalert.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                     @Value("${app.notifications.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public ChannelType getType() {
        return ChannelType.EMAIL;
    }

    @Override
    public void send(Channel channel, User recipient, NotificationMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(recipient.getEmail());
        mail.setSubject(message.title());
        mail.setText(message.body());

        try {
            mailSender.send(mail);
        } catch (Exception e) {
            throw new NotificationDeliveryException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
