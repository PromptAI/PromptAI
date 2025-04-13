package com.zervice.kbase.api.restful.pojo;

import com.zervice.kbase.database.pojo.Chat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestChat {

    public RestChat(Chat chat) {
        this._id = chat.getId();
        this._visitTime = chat.getVisitTime();
        this._properties = chat.getProperties();
    }

    public RestChat(Chat chat, List<RootComponent> rootComponents) {
        this(chat);
        this._rootComponents = rootComponents;
    }

    public RestChat(Chat chat, List<RootComponent> rootComponents, Long evaluationCount) {
        this(chat, rootComponents);
        this._evaluationCount = evaluationCount;
    }

    private String _id;

    private Long _visitTime;

    private Chat.Prop _properties;

    private List<RootComponent> _rootComponents;

    private Long _evaluationCount;

    /**
     * 这里标记chat使用到的根节点信息
     * 这里只需要rootId & rootName
     */
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RootComponent {
        private String _id;
        private String _name;
        private String _type;
    }
}
