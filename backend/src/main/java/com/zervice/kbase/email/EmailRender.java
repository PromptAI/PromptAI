package com.zervice.kbase.email;

import com.zervice.kbase.ServerInfo;
import com.zervice.kbase.email.templates.EmailTemplateBuilder;
import com.zervice.kbase.email.templates.config.TbConfiguration;
import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.experimental.UtilityClass;

import java.text.NumberFormat;

@UtilityClass
public class EmailRender {
    private static final TbConfiguration _CONFIG = TbConfiguration.newInstance();

    private static final String _EMAIL_REGISTER_CODE = "${name} is in ${severity} state for ${service}";

    public Email renderTrialExpiringEmail(String emailAddr, String date) {
        String display = ServerInfo.getName();

        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        builder.text("Dear Valued User,").and()
                .text("You receive this email because you have a trial account registered with this email address (" + emailAddr + "). If you didn't create the account, you can safely ignore this email.").and()
                .text("").and()
                .text("We are sending this email to remind you that your trial has expired at " + date + " and your account has been downgrade to Basic Package. If you want to continue using our full service, please reach out to our service team, otherwise please login and get your personal data be backed up properly before it expires!").and()
                .text("").and()
                .text("Thank you for trying our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_REGISTER_CODE)
                .subject("[" + display + "]: Trial Account Expiring Notification")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderTrialExpiredEmail(String emailAddr, String date) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("You receive this email because you have a trial account registered with this email address (" + emailAddr + "). If you didn't create the account, you can safely ignore this email.").and()
                .text("").and()
                .text("We are sending this email to let you know that your trial has expired at " + date + ". Your account has been terminated and you will not be able to login to the system anymore.").and()
                .text("").and()
                .text("Thank you for trying our service and we would look forward to serving you again in the near future!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_REGISTER_CODE)
                .subject("[" + display + "]: Trial Account Terminated Notification")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderRegisterLinkEmail(String emailAddr, String activeLink, String code, String order) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Welcome to " + display + ". " + " We received a request to create a " + display + " account using this email address  (" + emailAddr + "). If it is not requested by you, you can safely ignore this email. Otherwise, please click the following link to activate the account: ").and()
                .text("").and()
                .text(activeLink).bold().linkUrl(activeLink).color("red").and()
                .text("").and()
                .text("Your login account is: " + emailAddr).and()
                .text("Your login password is: " + code).and()
                .text("").and()
                .text("Please change the password after the account activation.").and()
                .text("").and()
                .text("Thanks for your interest.").and()
                .text("").and()
                .text(display + " Team").and()
                .hr().margin("20px 0").and();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_REGISTER_CODE)
                .subject("[" + display + "]: Activate your account")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    // TODO: internalization ...
    public Email renderRegisterEmail(String emailAddr, String code, String order) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Somebody is requesting to create a " + display + " Service Account using this email address (" + emailAddr + "). If it is not requested by you, you can safely ignore this email.").and()
                .text("").and()
                .text("If you are requesting to create the " + display + " Service Account, please find your verification code and corresponding serial # below:").and()
                .text("Verification Code (Please input this code according to the instructions): ").and().text(code).bold().color("red").and()
                .text("Serial #: ").and().text(order).bold().and()
                .text("").and()
                .text("The verification will also be your initial password! Please change your password after your have signed into your account!").and()
                .text("Enjoy our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_REGISTER_CODE)
                .subject("[" + display + "]: Account Registration Verification Code")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderLoginEmail(String emailAddr, String code, String order) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Somebody is requesting to login a " + display + " Service Account using this email address (" + emailAddr + "). If it is not requested by you, you can safely ignore this email.").and()
                .text("").and()
                .text("If you are requesting to login to the " + display + " Service Account, please find your verification code and corresponding serial # below:").and()
                .text("Verification Code (Please input this code according to the instructions): ").and().text(code).bold().color("red").and()
                .text("Serial #: ").and().text(order).bold().and()
                .text("").and()
                .text("Enjoy our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_LOGIN_CODE)
                .subject("[" + display + "]: Account Login Verification Code")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    /**
     * Subject:[Talk2Bits]: Insufficient token balance
     * Content:
     * Dear Valued User (better to use the first name),
     *
     * You have 9,467 (please use , every three digits) remaining tokens. It might be the time to recharge your account.  Recharge link (click, the link, pop up the recharge popup window).
     *
     * Thanks for using our service.
     *
     * Talk2Bits Team
     *
     * Dear Valued User (better to use the first name),
     *
     * Thanks for using our service.  It seems you have used up your tokens.  The current balance is -297.  Please recharge your account as soon as possible.  Recharge link (click, the link, pop up the recharge popup window).
     *
     * Talk2Bits Team
     * mailto:info@talk2bits.com
     */
    public Email renderRestTokenNotify(Long restToken) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();
        builder.text("Dear Valued User,").and();

        // TODO format
        String restTokenStr = NumberFormat.getInstance().format(restToken);
        if (restToken > 0) {
            builder.text("You have " + restTokenStr + " remaining tokens. It might be the time to recharge your account.").and()
                    .text("").and()
                    .text("Thanks for using our service.").and()
                    .text(display + " Team").and();
        } else {
            builder.text("Thanks for using our service. It seems you have used up your tokens. The current balance is " + restTokenStr + ". ").and()
                    .text("").and()
                    .text("Please recharge your account as soon as possible.").and()
                    .text("").and()
                    .text("Thanks for using our service.").and()
                    .text(display + " Team").and();
        }

        builder.hr().margin("20px 0").and();

        HtmlTextEmail email = builder.build();

        String subject = restToken > 0 ? "[" + display + "]: Insufficient token balance" : "[" + display + "]: Token has been used up";
        return Email.builder()
                .type(Email.TYPE_ACCOUNT_REST_TOKEN_NOT_ENOUGH)
                .subject(subject)
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderAccountCreatedEmail(String accountLoginName, String passcode) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Thanks for your apply for " + display + " Account! ").and()
                .text("Your account:" + accountLoginName).and()
                .text("Your passcode:" + passcode).and()
                .text("Click to login: " + ServerInfo.getLoginAddr()).and()
                .text("If it is not requested by you, you can safely ignore this email.").and()
                .text("").and()
                .text("").and()
                .text("Enjoy our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_ACCOUNT_CREATED)
                .subject("[" + display + "]: Your Account Created")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderModifyEmail(String emailAddr, String code, String order) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Somebody is requesting to use this email address (" + emailAddr + ") as " + display + " Service Account email. If it is not requested by you, you can safely ignore this email.").and()
                .text("").and()
                .text("If you are requesting to use this email address as the " + display + " Service Account email, please find your verification code and corresponding serial # below:").and()
                .text("Verification Code (Please input this code according to the instructions): ").and().text(code).bold().color("red").and()
                .text("Serial #: ").and().text(order).bold().and()
                .text("").and()
                .text("Enjoy our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_MODIFY_CODE)
                .subject("[" + display + "]: Modify Account Email Verification Code")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public Email renderRestPwdEmail(String emailAddr, String code, String order) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);

        String display = ServerInfo.getName();

        /**
         * Subject: Modify Account Email Verification Code
         * Content:
         * Dear Valued User (better to use the first name),
         *
         * We received a request to reset your password in talk2bits.com. If it is not requested by you, you can safely ignore this email. If you are requesting to reset password, please find your verification code
         *
         * Verification Code
         *
         * Please input this code according to the link (click the link, will popup the password reset window).
         *
         * Serial #: ( 这个 Serial 如果能省掉就省掉， 如果只是Bookkeeing的话， 放到 信的最后)
         *
         * Thanks for using our service.
         *
         * Talk2Bits Team
         */
        builder.text("Dear Valued User,").and()
                .text("We received a request to reset your password in " + display + ". If it is not requested by you, you can safely ignore this email. If you are requesting to reset password, please find your verification code").and()
                .text("").and()
                .text(code).bold().color("red").and()
                .text("").and()
                .text("Thanks for using our service. ").and()
                .text("").and()
                .text(display + " Team").and()
                .hr().margin("20px 0").and();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_MODIFY_CODE)
                .subject("[" + display + "]: Modify Account Email Verification Code")
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }


    public Email renderAnnounceEmail(String emailAddr, String title, String body) {
        EmailTemplateBuilder.EmailTemplateConfigBuilder builder = EmailTemplateBuilder.builder()
                .configuration(_CONFIG);
        String display = ServerInfo.getName();

        builder.text("Dear Valued User,").and()
                .text("Your " + display + " account using this email address (" + emailAddr + ") has a new announcement for \"" + title + "\". If it is not your account, you can safely ignore this email.").and()
                .text("").and()
                .text("The announcement details is as follows:").and()
                .text(body).and()
                .text("").and()
                .text("Enjoy our service!").and()
                .text("").and()
                .text(display + " Team").linkUrl("mailto:" + ServerInfo.getMailTo()).and()
                .hr().margin("20px 0").and()
                .button("Visit Our Site", ServerInfo.getWebsite()).blue().left();

        HtmlTextEmail email = builder.build();

        return Email.builder()
                .type(Email.TYPE_FUNCTION_ANNOUNCE)
                .subject("[" + display + "]: Account Announcement: " + title)
                .body(email.getText())
                .htmlBody(email.getHtml())
                .build();
    }

    public static void main(String args[]) throws Exception {
        Email e = renderRestTokenNotify(112121L);
        System.out.println(e.getHtmlBody());
    }
}
