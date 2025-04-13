package com.zervice.kbase.database.pojo;

import lombok.*;

@Setter@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Feedback {
    private Long _id;

    private Long _accountId;

    private String _contact;

    private String _content;

    private Long _time;

    public static Feedback createFeedbackFromDao(Long id, long accountId,
                                              String contact, String content,
                                              Long time) {
        return Feedback.builder()
                .id(id).accountId(accountId)
                .contact(contact).content(content)
                .time(time)
                .build();
    }
}
