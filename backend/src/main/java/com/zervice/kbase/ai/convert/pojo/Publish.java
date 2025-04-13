package com.zervice.kbase.ai.convert.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.zervice.kbase.ai.convert.PublishContext;
import lombok.*;

/**
 * @author chenchen
 * @Date 2024/8/19
 */
@Builder
@Setter @Getter
@AllArgsConstructor
@NoArgsConstructor
public class Publish {

    private String _id;
    private String _botName;
    private LlmConfig _llmConfig;
    private Data _data;

    public Publish(PublishContext context) {
        this._id = context.getPublishedProject().getId();
        this._botName = context.botName();
        this._llmConfig = LlmConfig.factory(context);
        this._data = Data.factory(context);
    }

    public static Publish factory(PublishContext context) {
        return new Publish(context);
    }

    @Override
    public String toString() {
        JSONObject result = new JSONObject(true);
        result.put("id", _id);
        result.put("bot_name", _botName);
        result.put("llm_config", _llmConfig);
        result.put("data", _data.toPublishData());

        return result.toString(SerializerFeature.PrettyFormat);
    }
}
