package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.*;
import lombok.extern.log4j.Log4j2;

/**
 *
 */
@EqualsAndHashCode
@ToString
@Builder
@Log4j2
public class NotifyEmail {
    public static final String STATUS_CREATED = "created";
    public static final String STATUS_SENT = "sent";

    public static String CONTACT_COMMENT_SUBJECT = "there are new comment leave!";


    public enum CATEGORY {
        INVITATION, DELETION, SUSPEND, TRIAL_APPLY,
        CONTACT_COMMENT, RELEASE_PUBLISHED_PROJECT, FINISHED_RELEASE_PUBLISHED_PROJECT
    }

    @Getter @Setter @Builder @AllArgsConstructor
    public static class EmailProp {
        int _ver;
        String _apiKey;
        String _accountExternalId;
        String _firstName;
        String _lastName;
        String _description;

        public EmailProp() {}

        public static EmailProp parse(String properties) {
            return JSON.parseObject(properties, EmailProp.class);
        }
    }

    @Getter @Setter
    long _id;

    @Getter @Setter
    String _recipient;

    @Getter @Setter
    String _subject;

    @Getter @Setter
    String _body;

    @Getter @Setter
    String _category;

    @Getter @Setter
    String _status;

    @Getter @Setter
    long _createdEpochMs;

    @Getter @Setter
    long _sentEpochMs;

    @Getter @Setter
    String _username;

    @Getter @Setter
    EmailProp _properties;

    @Getter @Setter
    String _htmlBody;

    public void setPropertiesAsString(String properties) {
        if (Strings.isNullOrEmpty(properties)) {
            _properties = new EmailProp();
        } else {
            _properties = JSONObject.parseObject(properties).toJavaObject(EmailProp.class);
        }
    }


    public static NotifyEmail factory(String recipient, String subject, String body, String category, EmailProp props) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(recipient));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(subject));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(body));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(category));

        NotifyEmail email = NotifyEmail.builder().recipient(recipient).subject(subject).body(body).category(category)
                .createdEpochMs(System.currentTimeMillis()).status(STATUS_CREATED).build();

        email.setProperties(props);
        return email;
    }

    public static NotifyEmail factory(String recipient, String subject, String body, String htmlBody, String category) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(recipient));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(subject));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(body));

        NotifyEmail email = NotifyEmail.builder().recipient(recipient).subject(subject).body(body).htmlBody(htmlBody).category(Strings.isNullOrEmpty(category) ? "" : category)
                .createdEpochMs(System.currentTimeMillis()).status(STATUS_CREATED).build();
        return email;
    }


}
