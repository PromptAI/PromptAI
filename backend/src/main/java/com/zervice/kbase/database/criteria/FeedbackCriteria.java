package com.zervice.kbase.database.criteria;

import lombok.*;

/**
 * @author chen
 * @date 2022/9/20
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackCriteria {
    private String _contact;
    private String _content;
    private String _accountId;
}
