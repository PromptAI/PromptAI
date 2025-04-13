package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.zervice.common.pojo.chat.ButtonPojo;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentEntity;
import com.zervice.kbase.api.rpc.pojo.IpPojo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xh
 */
@Data
@Log4j2
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("chats")
@CompoundIndexes({@CompoundIndex(name = "chatIp", def = "{'_properties._ip' : 1}")})
public class Chat {
    /** 被模型 处理 - 默认值 */
    public static final String HANDLE_BY_MODEL = "model";
    /** 被backend 处理 */
    public static final String HANDLE_BY_BACKEND = "backend";
    private String _id;

    private Long _visitTime;

    private Prop _properties;

    public boolean addRootComponentId(String rootComponentId) {
        Set<String> _rootComponentIds = _properties.getRootComponentIds();
        if (_rootComponentIds == null) {
            _rootComponentIds = new HashSet<>(16);
            _properties.setRootComponentIds(_rootComponentIds);
        }

        return _rootComponentIds.add(rootComponentId);
    }

    public static Chat factory(String ip, String scene, String projectId,
                               String publishedProjectId, String handleBy) {
        Prop prop = Prop.builder()
                .ip(ip).scene(scene)
                .projectId(projectId).handleBy(handleBy)
                .publishedProjectId(publishedProjectId)
                .build();

        return Chat.builder()
                .visitTime(System.currentTimeMillis())
                .properties(prop)
                .build();
    }

    public boolean hasRootComponentId(String rootComponentId) {
        if (_properties == null || CollectionUtils.isEmpty(_properties.getRootComponentIds())) {
            return false;
        }
        return _properties.getRootComponentIds().contains(rootComponentId);
    }

    /**
     * 消息评价的结果
     * @param helpful {@link Evaluation#HELP}
     */
    public void appendEvaluate(Integer helpful) {
        List<Integer> evaluates = _properties._evaluates;
        if (evaluates == null) {
            evaluates = new ArrayList<>();
            _properties._evaluates = evaluates;
        }

        evaluates.add(helpful);
    }

    /**
     * 查询当前project所有的entity，返回格式为：
     * [
     *  {
     *     "id":"entityId-1",
     *     "name":"entity-name",
     *     "display":"entity-display-name-1"
     * },
     * {
     *      "id":"entityId-2",
     *      "name":"entity-name-2",
     *      "display":"entity-display-name"
     *  }
     * ]
     */
    public static List<Chat.SimpleEntity> buildEntitySnapshot( Set<ProjectComponent> projectEntities) {
        if (CollectionUtils.isEmpty(projectEntities)) {
            return Collections.emptyList();
        }
        List<SimpleEntity> snapshot = new ArrayList<>(projectEntities.size());

        for (ProjectComponent e : projectEntities) {
            RestProjectComponentEntity entity = new RestProjectComponentEntity(e);
            snapshot.add(Chat.SimpleEntity.from(entity));
        }

        return snapshot;
    }

    /**
     * 对话时实时更新被填充的slot
     *
     * @param validSlots {"entityName":"entityValue"}
     * @return if ture is returned, this chat needs to be updated to DB
     */
    public void refreshLatestFilledSlots(JSONObject validSlots) {
        List<SimpleEntity> snapshot = _properties._entitySnapshot;
        // 没有变量，无需处理
        if (snapshot == null || validSlots == null || validSlots.isEmpty()) {
            return;
        }

        Map<String, SimpleEntity> snapshotMap = snapshot.stream().collect(Collectors.toMap(SimpleEntity::getName, s -> s));

        Map<String, FilledSlot> filledSlots = Maps.newHashMapWithExpectedSize(validSlots.size());

        for (String entityName : validSlots.keySet()) {
            SimpleEntity simpleEntity = snapshotMap.get(entityName);
            if (simpleEntity == null) {
                LOG.warn("[{}][ignore store filed slots, entity not found:{}]", this._id, entityName);
                continue;
            }

            String entityValue = validSlots.getString(entityName);

            filledSlots.put(simpleEntity._id, FilledSlot.factory(simpleEntity._id, simpleEntity._name, simpleEntity._display, entityValue));
        }

        _properties.setFilledSlots(filledSlots);
    }

    /**
     * 是否从snapshot读取..
     */
    public static boolean readFromSnapshot(String scene) {
        return Prop.SCENE_PUBLISH_SNAPSHOT.equals(scene);
    }

    /**
     * 是否从snapshot读取..
     */
    public boolean readFromSnapshot() {
        return readFromSnapshot(_properties._scene);
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prop {
        public static final Integer HELPFUL_NOT_SET = -1;
        public static final Integer HELPFUL = 0;
        public static final Integer HELPFUL_LESS = 1;

        /* debug */
        public static final String SCENE_DEBUG = "debug";
        /* publish read data from db */
        public static final String SCENE_PUBLISH_DB = "publish_db";
        /* publish read data from snapshot */
        public static final String SCENE_PUBLISH_SNAPSHOT = "publish_snapshot";

        private String _ip;
        /**
         * {@link IpPojo}
         */
        private JSONObject _ipExtra;
        private String _projectId;
        private String _publishedProjectId;
        @Builder.Default
        private Boolean _hasMessage = false;
        /**
         * 标记当前会话由谁驱动，默认为 HANDLE_BY_MODEL
         */
        @Builder.Default
        private String _handleBy = HANDLE_BY_MODEL;

        /**
         * 标记会话中产生使用的到conversation/faq
         */
        private Set<String> _rootComponentIds;

        /**
         * project可以控制下级节点是否生成按钮，如果按钮过多，可以配置将部分按钮收起来。
         */
        private List<ButtonPojo> _buttons;

        /**
         * 是否选择展示节点信息（conversations）
         */
        private String _showSubNodesAsOptional;

        /**
         * 项目欢迎语
         */
        private String _welcome;

        /**
         * 默认回答
         */
        private String _fallback;

        /**
         * 记录评价的结果，便于搜索
         * 这里用数组，可以看到评价的数量，以及各种类型评价的次数
         * {@link Evaluation#HELP}
         */
        private List<Integer> _evaluates;

        /**
         * project locale
         */
        private String _locale;

        /**
         * 发布的roots
         */
        private List<String> _publishedComponents;

        private JSONObject _chatBotSettings;

        /**
         * user agent from http.request
         */
        private JSONObject _userAgent;

        /**
         * 创建chat时，将当前project使用的entity存一份快照：
         * <p>
         *  在Message中提供：slots map of  name & value,
         * 做报表时需要关注id，所以使用快照做一次转换映射，减少查询db次数
         * <p>
         * 配合{@link FilledSlot}使用，在chat中存储本次对话所提取到的变量。
         */
        private List<Chat.SimpleEntity> _entitySnapshot;

        /**
         * 对话过程中填充的变量
         */
        private Map<String /*entity id*/, FilledSlot> _filledSlots;

        /**
         * 创建对话时需要填充到上下文的slot - 从外部系统来
         */
        private Map<String, Object> _slots;

        /**
         * 需要存储的变量 - 从外部系统来
         */
        @Builder.Default
        private Map<String, Object> _variables = new HashMap<>();

        /**
         * 使用场景
         */
        private String _scene;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleEntity {
        private String _id;

        private String _name;

        private String _display;

        public static SimpleEntity from(RestProjectComponentEntity entity) {
            return SimpleEntity.builder()
                    .id(entity.getId())
                    .name(entity.getName())
                    .display(entity.getDisplay())
                    .build();
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FilledSlot {

        /**
         * entity id
         */
        private String _id;

        /**
         * entity name
         */
        private String _name;

        /**
         * entity display name
         */
        private String _display;

        /**
         * filled value...
         */
        private String _value;

        public static FilledSlot factory(String id, String name,String display, String value) {
            return FilledSlot.builder()
                    .id(id).name(name).value(value).display(display)
                    .build();
        }
    }
}
