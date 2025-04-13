package com.zervice.kbase.database.pojo;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.JSONObject;
import com.zervice.common.i18n.MessageUtils;
import lombok.*;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.Set;

/**
 * @author chen
 * @date 2022/10/28
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Evaluation {

    /**
     * 有帮助
     */
    public static final Integer HELPFUL = 1;
    /**
     * 无帮助
     */
    public static final Integer HELPLESS = 2;


    public static final Set<Integer> HELP = new ConcurrentHashSet<>();
    static {
        HELP.add(HELPFUL);
        HELP.add(HELPLESS);
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

    private Prop _properties;

    public static Evaluation createEvaluationFromDao(Long id, String componentId,
                                                     String chatId, String messageId,
                                                     Integer helpful, String properties) {
        return Evaluation.builder()
                .id(id)
                .componentId(componentId).messageId(messageId)
                .chatId(chatId).helpful(helpful)
                .properties(JSONObject.parseObject(properties, Prop.class))
                .build();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        private Long _createTime;

    }

    /**
     * TODO 国际化
     */
    public static String convertStatus2Name(Integer helpful) {
        Locale locale = LocaleContextHolder.getLocale();
        if (helpful == null) {
            return MessageUtils.get("unknown", locale);
        }
        switch (helpful) {
            case 1:
                return MessageUtils.get("helpful", locale);
            case 2:
                return MessageUtils.get("helpless", locale);
            default:
                return MessageUtils.get("unknown", locale);
        }
    }

}
