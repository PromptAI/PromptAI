package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.common.utils.JSONUtils;
import com.zervice.kbase.api.restful.pojo.RestCloneProject;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentBot;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.restful.pojo.mica.Button;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Peng Chen
 * @date 2022/6/8
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Project {

    public static final String ID_PREFIX = "p_";

    /**
     * id
     */
    private String _id;

    private String _name;

    private Long _time;

    private String _ip;

    /**
     * 模板被浏览的次数，这里不能放Properties.
     *
     * 放Property 可能会导致properties更新出问题（view count++ 和Project同时更新会丢数据）
     */
    private Long _templateViewCount;

    private Prop _properties;

    public static Project clone(RestCloneProject cloneProject, Project originProject, String ip, long createBy) {
        ip = ip.split(",")[0];
        Project project = new Project();
        project.setId(cloneProject.getTemplateProjectId());
        project.setName(cloneProject.getName());
        project.setIp(ip);
        project.setTime(System.currentTimeMillis());
        project.setProperties(Prop.clone(cloneProject, originProject, createBy));
        return project;
    }

    public static String generateId() {
        return ID_PREFIX + Base36.toString(IdGenerator.generateId());
    }

    public static Project createProjectFromDao(String id, String name, String ip, Long time,
                                               Long templateViewCount, String properties) {

        return Project.builder()
                .id(id).name(name).ip(ip).time(time).templateViewCount(templateViewCount)
                .properties(JSONObject.parseObject(properties, Prop.class))
                .build();
    }


    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatBotSetting {

        /**
         * bot名称
         */
        private String _name;

        /**
         * 是否开启调查
         */
        @Builder.Default
        private Boolean _survey = false;

        /**
         * 初始化会话是填充slot
         * - 已弃用，设置转移到Entity中，使用时动态从entity读取，无需用户二次设置
         */
        @Deprecated
        private List<Slot> _slots;

        /**
         * 初始化会话保存的变量:从业务网站可选的读取
         */
        private List<Variable> _variables;

        /**
         * 是否开启附件上传
         */
        @Builder.Default
        private Boolean _upload = true;

        /**
         * 机器相关的icon
         */
        private Icon _icon;

        /**
         * 主题相关
         */
        private String _theme;

        /**
         * web部署时，加载bot图标是否最小化
         */
        @Builder.Default
        private Boolean _minimize = false;
    }

    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Icon {
        private String _user;
        private String _bot;
    }

    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Slot {

        /**
         * slotId
         */
        private String _id;

        /**
         * type: 'localStore' | 'sessionStore' | 'custom'
         */
        private String _type;

        /**
         * js代码
         */
        private String _body;

        /**
         * 将开启了默认值的 entity 转换为 slot
         * @param entity
         * @return
         */
        public static Slot factory(RestProjectComponentEntity entity) {
            return Slot.builder()
                    .id(entity.getId()).type(entity.getDefaultValueType())
                    .body(entity.getDefaultValue())
                    .build();
        }
    }

    @Builder
    @Setter@Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Variable {

        /**
         * bot名称
         */
        private String _key;

        /**
         * type: 'localStore' | 'sessionStore' | 'custom'
         */
        private String _type;

        /**
         * js代码
         */
        private String _body;
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop extends BaseProp {
        public static final String SHOW_SUB_NODES_AS_OPTIONAL_ALL = "all";
        public static final String SHOW_SUB_NODES_AS_OPTIONAL_NONE = "none";
        public static final String SHOW_SUB_NODES_AS_OPTIONAL_CUSTOM = "custom";

        public static final String LOCAL_EN = "en";
        public static final String LOCAL_ZH = "zh";
        /**
         * 文本的回复
         */
        public static final String FALLBACK_TYPE_TEXT = "text";
        /**
         * webhook的回复
         */
        public static final String FALLBACK_TYPE_WEBHOOK = "webhook";


        /**
         * llm chat 对话调度模式 - 分配器
         */
        public static final String SCHEDULE_DISPATCHER = "dispatcher";

        /**
         * llm chat 对话调度模式 - 轮询优先
         */
        public static final String SCHEDULE_PRIORITY= "priority";

        public static Set<String> SCHEDULES = new HashSet<>();
        static {
            SCHEDULES.add(SCHEDULE_PRIORITY);
            SCHEDULES.add(SCHEDULE_DISPATCHER);
        }

        private static Set<String> _fallbackTypes = new HashSet<>();

        static {
            _fallbackTypes.add(FALLBACK_TYPE_TEXT);
            _fallbackTypes.add(FALLBACK_TYPE_WEBHOOK);
        }


        private String _description;

        private String _locale;

        private String _image;

        /**
         * 欢迎语
         */
        private String _welcome;

        /**
         * 默认回答
         * 这支持一个string
         */
        private String _fallback;

        /**
         * 跟着默认回答一起出现的按钮
         */
        private List<Button> _fallbackButtons;

        @Builder.Default
        private String _fallbackType = FALLBACK_TYPE_TEXT;

        /**
         * 默认回答
         * 这支持一个webhook
         */
        private List<RestProjectComponentBot.BotResponse> _webhooks;

        /**
         * 是否选择展示节点信息（conversations） 默认展示全部
         * ALL    - 全部
         * None   - 不展示
         * custom - 自定义
         * <p>
         * 其中ALL、None时，节点上的展示不可编辑
         */
        @Builder.Default
        private String _showSubNodesAsOptional = SHOW_SUB_NODES_AS_OPTIONAL_ALL;

        @Builder.Default
        private ChatBotSetting _chatBotSettings = new ChatBotSetting();

        /**
         * a1库所用到的介绍信息 - markdown
         */
        private String _introduction;

        /**
         * 是否可以被Templates界面展示
         * - 仅a1可使用改字段
         */
        @Builder.Default
        private Boolean _viewable = true;

        /**
         * llm chat 对话调度模式
         * - priority: // default
         * - dispatcher:
         */
        @Builder.Default
        private String _schedule = SCHEDULE_PRIORITY;

        public static void checkFallbackType(String fallbackType) {
            for (String t : _fallbackTypes) {
                if (t.equals(fallbackType)) {
                    return;
                }
            }

            throw new RestException(StatusCodes.AiProjectUnsupportedFallbackType, fallbackType);
        }

        public static Prop clone(RestCloneProject cloneProject, Project originProject, long createBy) {
            Prop originProp = originProject._properties;
            // deep clone
            Prop newProp = JSONUtils.copy(originProp, Prop.class);

            newProp.setDescription(cloneProject.getDescription());
            newProp.setLocale(cloneProject.getLocale());
            newProp.setImage(cloneProject.getImage());
            newProp.setWelcome(cloneProject.getWelcome());
            newProp.setFallback(cloneProject.getFallback());
            newProp.setCreateBy(createBy);
            newProp.setCreateTime(System.currentTimeMillis());
            return newProp;
        }
    }


}
