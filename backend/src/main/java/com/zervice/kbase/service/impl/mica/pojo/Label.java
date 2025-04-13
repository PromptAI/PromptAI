package com.zervice.kbase.service.impl.mica.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentUser;
import com.zervice.kbase.database.pojo.ProjectComponent;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * goto节点的 label
 *
 * @author chenchen
 * @Date 2025/1/13
 */
@Log4j2
@Builder
@Setter@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Label {

    public static final String REFERENCE_BOT = "bot";
    public static final String REFERENCE_USER = "user";
    public static final String REFERENCE_CALL = "call";

    public static Set<String> REFERENCE_TYPES = Set.of(REFERENCE_BOT, REFERENCE_USER, REFERENCE_CALL);
    /**
     * 对应的 label值
     */
    private String _value;
    /**
     * 对应的 label 引用的类型
     * - bot
     * - user?
     */
    private String _referenceType;
    /**
     * 对应的 label 引用的值
     * - bot： xxx // bot对应的 utterance
     */
    private String _referenceValue;

    public static Label factory(String value, JSONObject reference) {
        // call bot
        if (reference.containsKey(REFERENCE_BOT)) {
            return factory(value, REFERENCE_BOT, reference.getString(REFERENCE_BOT));
        }

        // call webhook or gpt
        if (reference.containsKey(REFERENCE_CALL)) {
            return factory(value, REFERENCE_CALL, reference.getString(REFERENCE_CALL));
        }

        throw new RestException(StatusCodes.BadRequest);
    }

    public static Label factory(String value, String referenceType, String referenceValue) {
        return Label.builder()
                .value(value).referenceType(referenceType).referenceValue(referenceValue)
                .build();
    }

    public ProjectComponent findLabel(List<ProjectComponent> components) {
        // call 的 gpt
        if (Label.REFERENCE_CALL.equals(this._referenceType)) {
            String gptName = this.getReferenceValue();

            return components.stream()
                    .filter(c -> RestProjectComponentGpt.TYPE_NAME.equals(c.getType()))
                    .map(RestProjectComponentGpt::new)
                    .filter(gpt -> gptName.equals(gpt.getData().getName()))
                    .map(RestProjectComponentGpt::toProjectComponent)
                    .findAny().orElse(null);
        }

        // bot 和 call 的 webhook
        if (Label.REFERENCE_BOT.equals(this.getReferenceType()) || REFERENCE_CALL.equals(this._referenceType)) {
            String botUtterance = this.getReferenceValue();

            return components.stream()
                    .filter(c -> RestProjectComponentBot.TYPE_NAME.equals(c.getType()))
                    .map(RestProjectComponentBot::new)
                    .filter(bot -> {
                        List<RestProjectComponentBot.BotResponse> responses = bot.getData().getResponses();
                        for (RestProjectComponentBot.BotResponse res : responses) {
                            // bot utterance  or webhook name
                            if (botUtterance.equals(res.getContent().getString("text"))) {
                                return true;
                            }
                        }

                        return false;
                    })
                    .map(RestProjectComponentBot::toProjectComponent)
                    .findAny().orElse(null);
        }

        if (Label.REFERENCE_USER.equals(this.getReferenceType())) {
            String userClime = this.getReferenceValue();

            return components.stream()
                    .filter(c -> RestProjectComponentUser.TYPE_NAME.equals(c.getType()))
                    .map(RestProjectComponentUser::new)
                    .filter(user -> user.matchExample(userClime))
                    .map(RestProjectComponentUser::toProjectComponent)
                    .findAny().orElse(null);
        }

        LOG.error("[unsupported reference type:{}]", _referenceType);
        throw new RestException(StatusCodes.BadRequest, "unsupported label reference type");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Objects.equals(_value, label._value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_value);
    }
}
