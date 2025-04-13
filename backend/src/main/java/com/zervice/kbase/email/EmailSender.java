package com.zervice.kbase.email;

import com.zervice.common.utils.LayeredConf;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.database.SecurityUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@Log4j2
public class EmailSender {
    private InternetAddress _fromAddress;

    private Session _session;

    public EmailSender() {
        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_EMAIL);

        // TODO: default to noreply@zervice.us
        String from = config.getString(ZBotConfig.EMAIL_SENDER_ADDRESS, ZBotConfig.EMAIL_SENDER_ADDRESS_DEFAULT);

        InternetAddress addr = null;
        try {
            String display = config.getString(ZBotConfig.EMAIL_SENDER_DISPLAY, ZBotConfig.EMAIL_SENDER_DISPLAY_DEFAULT);
            addr = new InternetAddress(from, display);
        } catch (UnsupportedEncodingException ue) {

        }

        _fromAddress = addr;
    }

    /**
     * Send a simple email
     */
    public void sendSimpleMail(String to, String subject, String content, String... cc) {
        try {
            Session session = _getJavaMailSession();

            MimeMessage message = new MimeMessage(session);
            message.setFrom(_fromAddress);
            message.addRecipients(Message.RecipientType.TO, to);
            message.setSubject(subject);
            message.setText(content);
            if (ArrayUtils.isNotEmpty(cc)) {
                message.addRecipients(Message.RecipientType.CC, Arrays.stream(cc).collect(Collectors.joining(",")));
            }

            Transport.send(message);
        } catch (MessagingException me) {
            LOG.error("[send email fail. to:{}, error:{}]", to, me.getMessage(), me);
        }
    }

    /**
     * Send HTML email, the content is in HTML
     */
    public void sendHtmlMail(String to, String subject, String content, String... cc) throws MessagingException {
        try {
            Session session = _getJavaMailSession();
            MimeMessage message = new MimeMessage(session);

            message.setFrom(_fromAddress);
            message.addRecipients(Message.RecipientType.TO, to);
            message.setSubject(subject);
            message.setText(content, null, "text/html");
            if (ArrayUtils.isNotEmpty(cc)) {
                message.addRecipients(Message.RecipientType.CC, Arrays.stream(cc).collect(Collectors.joining(",")));
            }

            Transport.send(message);
        } catch (MessagingException me) {

        }
    }

    /**
     * Send email with an attachment
     */
    public void sendAttachmentsMail(String to, String subject, String content, String filePath, String... cc) throws MessagingException {
        try {
            Session session = _getJavaMailSession();
            MimeMessage message = new MimeMessage(session);

            message.setFrom(_fromAddress);
            message.addRecipients(Message.RecipientType.TO, to);
            message.setSubject(subject);
            if (ArrayUtils.isNotEmpty(cc)) {
                message.addRecipients(Message.RecipientType.CC, Arrays.stream(cc).collect(Collectors.joining(",")));
            }

            Multipart emailContent = new MimeMultipart();

            // message.setText(content, null, "text/html");
            BodyPart body = new MimeBodyPart();
            body.setText(content);
            emailContent.addBodyPart(body);

            MimeBodyPart attach = new MimeBodyPart();
            attach.attachFile(filePath);
            emailContent.addBodyPart(attach);

            message.setContent(emailContent);

            Transport.send(message);
        } catch (IOException | MessagingException me) {

        }
    }

    /**
     * Send email with attachment that are included as static resource in the packed WAR
     */
    public void sendResourceMail(String to, String subject, String content, String rscPath, String rscId, String... cc) throws MessagingException {
        try {
            Session session = _getJavaMailSession();
            MimeMessage message = new MimeMessage(session);

            message.setFrom(_fromAddress);
            message.addRecipients(Message.RecipientType.TO, to);
            message.setSubject(subject);
            if (ArrayUtils.isNotEmpty(cc)) {
                message.addRecipients(Message.RecipientType.CC, Arrays.stream(cc).collect(Collectors.joining(",")));
            }

            Multipart emailContent = new MimeMultipart();

            // message.setText(content, null, "text/html");
            BodyPart body = new MimeBodyPart();
            body.setText(content);
            emailContent.addBodyPart(body);

            MimeBodyPart attach = new MimeBodyPart();
            attach.attachFile(rscPath);
            attach.setContentID("<" + rscId + ">");
            attach.setDisposition(MimeBodyPart.INLINE);

            emailContent.addBodyPart(attach);

            message.setContent(emailContent);

            Transport.send(message);
        } catch (IOException | MessagingException me) {

        }
    }


    /**
     * Send a simple email
     */
    public void sendSimpleMail(String to, String subject, String content, String htmlText, String cc, String bcc) throws Exception {
        Session session = _getJavaMailSession();

        MimeMessage message = new MimeMessage(session);

        message.setFrom(_fromAddress);
        message.addRecipients(Message.RecipientType.TO, to);
        message.setSubject(subject, "utf-8");

        if (StringUtils.isEmpty(htmlText)) {
            message.setText(content, "utf-8");
        } else {
            // Unformatted text version
            final MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content, "text/plain;charset=utf-8");

            // HTML version
            final MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlText, "text/html;charset=utf-8");

            // Create the Multipart.  Add BodyParts to it.
            final Multipart mp = new MimeMultipart("alternative");
            mp.addBodyPart(textPart);
            mp.addBodyPart(htmlPart);

            // Set Multipart as the message's content
            message.setContent(mp);
        }

        if (StringUtils.isNotEmpty(cc)) {
            message.addRecipients(Message.RecipientType.CC, cc);
        }

        if (StringUtils.isNotEmpty(bcc)) {
            message.addRecipients(Message.RecipientType.BCC, bcc);
        }

        Transport.send(message);
    }


    /**
     * Provider to initialize mail sender
     * https://www.baeldung.com/spring-email
     * @return
     */
    static Session _getJavaMailSession() {
        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_EMAIL);

        String type = config.getString(ZBotConfig.EMAIL_SENDER_TYPE, ZBotConfig.EMAIL_SENDER_TYPE_DEFAULT);
        if ("smtp".equalsIgnoreCase(type)) {

            /**
             * We default to Zoho for testing purpose now
             */
            Properties props = System.getProperties();
            Boolean auth = config.getBoolean(ZBotConfig.EMAIL_SMTP_AUTH_ENABLED, ZBotConfig.EMAIL_SMTP_AUTH_ENABLED_DEFAULT);
            String host = config.getString(ZBotConfig.EMAIL_SMTP_HOST, ZBotConfig.EMAIL_SMTP_HOST_DEFAULT);
            Integer port = config.getInt(ZBotConfig.EMAIL_SMTP_PORT, ZBotConfig.EMAIL_SMTP_PORT_DEFAULT);
            Boolean enableTls = config.getBoolean(ZBotConfig.EMAIL_SMTP_TLS_ENABLED, ZBotConfig.EMAIL_SMTP_TLS_ENABLED_DEFAULT);

            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", auth ? "true" : "false");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", "" + port);
            if (enableTls) {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.debug", "" + config.getBoolean(ZBotConfig.EMAIL_DEBUG_ENABLED, ZBotConfig.EMAIL_DEBUG_ENABLED_DEFAULT));

            return Session.getInstance(props, SMTPAuthenticator.getInstance());
        } else if ("ses".equalsIgnoreCase(type)) {
            // TODO
            throw new IllegalStateException("SES not supported yet");
        } else {
            throw new IllegalArgumentException("Invalid mail service type - " + type);
        }
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {
        @Getter
        private static final SMTPAuthenticator _instance = new SMTPAuthenticator();

        private SMTPAuthenticator() {

        }

        LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_EMAIL);

        public PasswordAuthentication getPasswordAuthentication() {
            final String SMTP_AUTH_USER = config.getString(ZBotConfig.EMAIL_SMTP_AUTH_USER, ZBotConfig.EMAIL_SMTP_AUTH_USER_DEFAULT);
            final String SMTP_AUTH_PWD = config.getString(ZBotConfig.EMAIL_SMTP_AUTH_PASS, ZBotConfig.EMAIL_SMTP_AUTH_PASS_DEFAULT);

            LOG.debug("Try authenticate to SMTP server (user={}, pass={}", SMTP_AUTH_USER, SecurityUtils.encodePassword(SMTP_AUTH_PWD, 2));

            return new PasswordAuthentication(SMTP_AUTH_USER, SMTP_AUTH_PWD);
        }
    }
}
