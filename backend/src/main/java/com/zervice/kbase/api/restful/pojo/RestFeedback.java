package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Feedback;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

/**
 * @author chen
 * @date 2023/3/24 10:53
 */
@Setter@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RestFeedback {
    public RestFeedback(Feedback feedback, String accountName) {
        this._id = feedback.getId();
        this._accountId = feedback.getId();
        this._accountName = accountName;
        this._contact = feedback.getContact();
        this._content = feedback.getContent();
        this._time = feedback.getTime();
    }

    private Long _id;

    private Long _accountId;

    private String _accountName;

    @NotBlank
    private String _contact;

    @NotBlank
    private String _content;

    private Long _time;


}
