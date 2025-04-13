package com.zervice.kbase.email;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zervice.common.utils.LayeredConf;
import com.zervice.common.utils.TimeUtils;
import com.zervice.kbase.ZBotConfig;
import com.zervice.kbase.database.dao.NotifyEmailDao;
import com.zervice.kbase.database.pojo.NotifyEmail;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.common.ding.DingTalkSender;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.jdbc.pool.PoolExhaustedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.*;

@Log4j2
public class EmailService {
    @Getter
    private static final EmailService _instance = new EmailService();

    private EmailService() {

    }

    private final EmailSender _sender = new EmailSender();

    /**
     * A single worker thread from this executor is used to execute the EmailCronTask
     */
    private final ExecutorService _executor =
            Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("email-sender").build());

    EmailCronTask _emailCronTask = new EmailCronTask();

    public void start() {
        _executor.submit(_emailCronTask);
    }

    /**
     * The email is an infinite loop. It peeks the head unsent email from db,
     * send the email and update email status
     */
    static class EmailCronTask implements Callable<Void> {
        // peeking from db, or blocking
        NotifyEmail peek(long timeoutMs) throws TimeoutException, InterruptedException {
            Preconditions.checkArgument(timeoutMs > 0);

            boolean toSleep = true;
            while (true) {
                Connection conn = null;
                try {
                    conn = DaoUtils.getConnection(true);
                    NotifyEmail email = NotifyEmailDao.getUnsentHead(conn);
                    if (email != null) {
                        return email;
                    }

                    // no tasks
                    if (toSleep) {
                        synchronized (_instance) {
                            // do not hold db connection while sleeping
                            DaoUtils.closeQuietly(conn);
                            _instance.wait(timeoutMs);
                            toSleep = false;
                        }
                    } else {
                        throw new TimeoutException();
                    }
                } catch (SQLException e) {
                    if (e instanceof PoolExhaustedException) {
                        LOG.error("Connection pool exhausted, try again later");
                        Thread.sleep(Math.min(timeoutMs, 500));
                    }
                } finally {
                    DaoUtils.closeQuietly(conn);
                }
            }
        }

        @Override
        public Void call() throws Exception {
            LOG.info("Starting Email Task, Looping on Email Table...");

            while (true) {
                try {
                    LayeredConf.Config config = LayeredConf.getConfig(ZBotConfig.CONFIG_OBJECT_EMAIL);
                    long iVal = TimeUtils.toMillis(config.getString(ZBotConfig.EMAIL_CHECK_IDLE_INTERVAL, ZBotConfig.EMAIL_CHECK_IDLE_INTERVAL_DEFAULT));
                    NotifyEmail email = peek(iVal);

                    // when comming here, email must be valid
                    try {
                        EmailService.getInstance().sendEmail(email);
                    } catch (Throwable e) {
                        LOG.error("Unable to send one notify email - " + email.getSubject(), e);
                    }
                } catch (TimeoutException e) {
                    // no new tasks, try again
                } catch (Exception e) {
                    // triggered by no-recoverable issues. Nothing we can do besides
                    // logging and quit
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    LOG.fatal("The EmailCronTask has to quit because of a non-recoverable error", e);
                    break;
                }
            }

            LOG.info("Email service Task Done");
            return null;
        }
    }

    public void queueEmail(Email email) throws Exception {
        NotifyEmail ne = NotifyEmail.factory(email.getRecipients(), email.getSubject(), email.getBody(), email.getHtmlBody(), email.getType());
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        NotifyEmailDao.addReturnId(conn, ne);
    }

    public void sendEmail(Email email) {
        NotifyEmail ne = NotifyEmail.factory(email.getRecipients(), email.getSubject(), email.getBody(), email.getHtmlBody(), email.getType());
        sendEmail(ne);
    }

    /**
     * send email using ses
     */
    public void sendEmail(NotifyEmail email) {
        if (email == null) {
            return;
        }

        Stopwatch stopWatch = Stopwatch.createStarted();
        // when we here, email shall never be null.
        LOG.info("Sending one notification email - " + email.getSubject());
        try {
            _sender.sendSimpleMail(email.getRecipient(), email.getSubject(), email.getBody(), email.getHtmlBody(), "", "");
            LOG.info("success send email to:{}, spend:{}", email.getRecipient(), stopWatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            // send mail exception
            LOG.error("Failed:{} to send one notify email to:{}, spend:{} ",
                    email.getSubject(), stopWatch.elapsed(TimeUnit.MILLISECONDS), e.getMessage(), e);
            DingTalkSender.sendQuietly("send email:" + email + " with error:" + e.getMessage());
        } finally {
            try (Connection conn = DaoUtils.getConnection(true)) {
                NotifyEmailDao.updateSent(conn, email.getId(), NotifyEmail.STATUS_SENT, System.currentTimeMillis());
            } catch (SQLException sqle) {
                // sql exception
            }
        }
    }
}
