package com.zervice.common.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.utils.Constants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xh
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageModel {
    private static final String CLICK_PREFIX = Constants.BUTTON_PREFIX;
    @NotBlank(message = "chatId required")
    private String _chatId;
    /**
     * send to mica
     */
    @NotBlank(message = "message required")
    private String _message;
    /**
     * presented to user
     */
    @NotBlank(message = "content required")
    private String _content;

    @NotNull
    private String _scene;

    /**
     * 是否试用流模式
     */
    @Builder.Default
    private Boolean _stream = false;

    /**
     * 运行模式
     */
    private String _runModel;

    /**
     * 内部使用
     */
    private String _ip;
    private String _projectId;
    private String _publishedProjectId;

    /**
     * 如果是点击消息
     * message ： /componentId
     */
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public boolean isClick() {
        return StringUtils.isNotBlank(_message) && _message.startsWith(CLICK_PREFIX);
    }

    /**
     * 如果是点击消息
     * message ： /componentId or /f_xxxx{cp_xxx} or /intent_name  or /f_xxx{intent_name}
     *
     * @return componentId or intent_name
     */
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public String parseComponentIdOrName() {
        if (isClick()) {
            // 这里有两种情况：
            // 1：/cp_xxx
            // 2：/f_xxx{cp_xxx}
            String removePrefix = _message.replace(CLICK_PREFIX, "");

            // 尝试情况2,如果未返回null，则表示处理成功
            String result = _parseClickFromFaq(removePrefix);
            if (result != null) {
                return result;
            }

            // 那么采用情况1，移除前缀后就可工作了
            return removePrefix;
        }
        return null;
    }

    /**
     * f_xxx{cp_xxx}
     *
     * @return cp_xxx
     */
    private String _parseClickFromFaq(String message) {
        if (StringUtils.isBlank(message) && !message.contains("{")) {
            return null;
        }

        // 定义正则表达式
        String regex = "\\{(.*?)\\}";
        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);
        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(message);

        // 查找匹配的内容
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
