package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Evaluation;
import lombok.Getter;
import lombok.Setter;

/**
 * @author chen
 * @date 2022/10/28
 */
@Setter@Getter
public class RestEvaluation {

    public RestEvaluation(Evaluation evaluation) {
        this._id = evaluation.getId();
        this._componentId = evaluation.getComponentId();
        this._chatId = evaluation.getChatId();
        this._messageId = evaluation.getMessageId();
        this._helpful = evaluation.getHelpful();
        this._properties = evaluation.getProperties();
    }

    private Long _id;

    /**
     * 根节点id
     */
    private String _componentId;

    private String _chatId;

    private String _messageId;

    /**
     *  1:有用，2:答非所问 3:内容看不懂 4:答案不详细 5:操作后解决  6 :其他
     */
    private Integer _helpful;

    private Evaluation.Prop _properties;
}
