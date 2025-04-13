package com.zervice.broker.service;

import com.zervice.common.agent.pojo.SimplePublishedProject;
import com.zervice.common.model.MessageRes;
import com.zervice.common.model.SendMessageModel;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 对话
 *
 * @author chen
 * @date 2023/3/9
 */
public interface MessageService extends BaseMessageService{

    default MessageService setLocal(Locale locale) {
        LocaleContextHolder.setLocale(locale);
        return this;
    }

    /**
     * 对话
     */
    MessageRes message(SendMessageModel message, SimplePublishedProject project, String account);
}
