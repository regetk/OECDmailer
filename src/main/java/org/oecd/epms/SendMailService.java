package org.oecd.epms;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import org.oecd.messagebeans.StatusMessageJSON;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 *
 *
 * @author Reget Kalamees
 */
public class SendMailService extends Verticle {

    private Session session;
    private Transport transport;

    private boolean ssl;
    private boolean tls;
    private String host;
    private int port;
    private boolean auth;
    private String username;
    private String password;
    private String contentType;
    private String from;

    private Logger logger;

    private Pattern pattern;
    private Matcher matcher;
    private static final String EMAIL_PATTERN
            = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    @Override
    public void start() {
        super.start();
        logger = container.logger();
        readConfigParameters();

        final EventBus eb = vertx.eventBus();
        Handler<Message<JsonObject>> myHandler = new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject messageBody = message.body();
                logger.info(messageBody);
                StatusMessageJSON answer = new StatusMessageJSON();
                String uuid = messageBody.getString("uuid");
                if (uuid == null) {
                    uuid = "";
                }
                try {
                    if (!inputValid(messageBody)) {
                        answer.setError();
                        answer.setMessage("the validation errors on input");
                    } else {
                        sendEmail(messageBody);
                        answer.setSuccess();
                        answer.setMessage("You email was sent successfully!");
                    }
                } catch (Exception e) {
                    answer.setError();
                    answer.setMessage("Your email could not be sent, please try again later");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    answer.setStacktrace(sw.toString());
                } finally {
                    logger.info("epms.email.out." + uuid);
                    eb.send("epms.email.out." + uuid, answer.getJsonObject());
                    //for integration test only
                    message.reply(answer.getJsonObject());
                }
            }
        };
        eb.registerHandler("epms.email.in", myHandler);

    }

    private void readConfigParameters() {
        JsonObject mailerConfig = container.config();
        ssl = mailerConfig.getBoolean("ssl", false);
        tls = mailerConfig.getBoolean("tls", false);
        host = mailerConfig.getString("host", "localhost");
        port = mailerConfig.getInteger("port", 25);
        auth = mailerConfig.getBoolean("auth", false);
        username = mailerConfig.getString("username", null);
        password = mailerConfig.getString("password", null);
        contentType = mailerConfig.getString("content_type", "text/plain");
        from = mailerConfig.getString("from", "localhost");
        pattern = Pattern.compile(EMAIL_PATTERN);

    }

    private void sendEmail(JsonObject emailMessage) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", Boolean.toString(auth));
        if (ssl) {
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        }
        if (tls) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", Integer.toString(port));

        session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            transport = session.getTransport("smtp");
            transport.connect();

        } catch (MessagingException e) {
            String message = "Failed to setup mail transport";
            logger.error("Failed to setup mail transport", e);
            throw new RuntimeException(message);
        }

        InternetAddress fromAddress;
        try {
            fromAddress = new InternetAddress(from, true);
        } catch (AddressException e) {
            String message = "Invalid from address: " + from;
            logger.error(message, e);
            throw new RuntimeException(message);
        }

        String to = emailMessage.getString("to");
        InternetAddress toAddress;
        try {
            toAddress = new InternetAddress(to, true);
        } catch (AddressException e) {
            String message = "Invalid to address: " + to;
            logger.error(message, e);
            throw new RuntimeException(message);
        }
        Address[] recipients = new Address[1];
        recipients[0] = toAddress;

        String subject = emailMessage.getString("subject");

        String body = emailMessage.getString("body");

        javax.mail.Message msg = new MimeMessage(session);

        try {
            msg.setFrom(fromAddress);
            msg.setRecipients(javax.mail.Message.RecipientType.TO, recipients);
            msg.setSubject(subject);
            msg.setContent(body, contentType);
            msg.setSentDate(new Date());
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();

        } catch (MessagingException e) {
            String message = "Failed to send message";
            logger.error(message, e);
            throw new RuntimeException(message);
        }

    }

    @Override
    public void stop() {
        try {
            if (transport != null) {
                transport.close();
            }
            logger.info("Mailer vert closing");
        } catch (MessagingException e) {
            logger.error("Failed to stop mail transport", e);
        }
    }

    private boolean inputValid(JsonObject mailMessage) {
        String mailTo = mailMessage.getString("to");
        if (mailTo == null || mailTo.length() < 3) {
            return false;
        }
        matcher = pattern.matcher(mailTo);
        if (!matcher.matches()) {
            return false;
        }

        String subject = mailMessage.getString("subject");
        if (subject == null || subject.length() == 0 || subject.length() > 128) {
            return false;
        }

        String body = mailMessage.getString("body");
        if (body == null || body.length() == 0) {
            return false;
        }

        String uuid = mailMessage.getString("uuid");
        if (uuid == null || uuid.length() == 0) {
            return false;
        }

        return true;
    }

}
