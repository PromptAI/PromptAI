package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Message;
import lombok.*;

/**
 * @author chen
 * @date 2022/10/28
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestMessage {

    public RestMessage(Message message, RestEvaluation evaluation) {
        this._id = message.getId();
        this._chatId = message.getChatId();
        this._dialog = message.getDialog();
        this._time = message.getTime();
        this._properties = message.getProperties();
        this._evaluation = evaluation;
    }

    private String _id;
    private String _chatId;
    private Message.Dialog _dialog;
    private Long _time;
    private Message.Prop _properties;

    private RestEvaluation _evaluation;

}
