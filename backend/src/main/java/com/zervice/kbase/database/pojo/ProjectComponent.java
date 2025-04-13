package com.zervice.kbase.database.pojo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Base36;
import com.zervice.common.utils.IdGenerator;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.utils.StrUtil;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/22
 */
@Log4j2
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProjectComponent {

    public static final String ID_PREFIX_FLOW = "c_";
    public static final String ID_PREFIX_FAQ = "f_";
    public static final String ID_PREFIX_OTHERS = "cp_";


    /**
     * kb content训练状态：待解析文本
     */
    public static final String KB_TRAIN_STATUS_WAIT_PARSE = "wait_parse";

    /**
     * kb content训练状态：解析中
     */
    public static final String KB_TRAIN_STATUS_PARSING = "parsing";

    /**
     * kb content训练状态：解析失败
     */
    public static final String KB_TRAIN_STATUS_PARSE_FAIL = "parse_fail";

    /**
     * kb content训练状态：待训练
     */
    public static final String KB_TRAIN_STATUS_WAIT_TRAIN = "wait_train";
    /**
     * kb content训练状态：训练中
     */
    public static final String KB_TRAIN_STATUS_TRAINING = "training";
    /**
     * kb content训练状态：训练完成
     */
    public static final String KB_TRAIN_STATUS_FINISH_TRAIN = "finish_train";


    public static final String TYPE_BOT = "bot";
    public static final String TYPE_GPT = "gpt";
    public static final String TYPE_USER = "user";
    public static final String TYPE_GOTO = "goto";
    public static final String TYPE_ENTITY = "any";
    public static final String TYPE_WEBHOOK = "webhook";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_CONVERSATION = "conversation";

    /**
     * 可以加入到回收站的类型
     **/
    public static final Set<String> TYPE_CAN_TRASH = Set.of(
            TYPE_USER, TYPE_BOT, TYPE_GOTO, TYPE_GPT);
    /**
     * globals
     **/
    public static final List<String> TYPE_ROOTS = Arrays.asList(TYPE_CONVERSATION);
    private String _id;

    private String _projectId;

    /**
     * 根节点id
     */
    private String _rootComponentId;

    private String _type;

    private Prop _properties;

    public Integer getNo() {
        return _properties._no;
    }

    public Long createTime() {
        return _properties == null ? 0 : _properties.getCreateTime();
    }

    /**
     * 这儿可能会返回空列表
     */
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public List<String> getRelations() {
        JSONArray relations = this._properties.getData().getJSONArray("relations");
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }


        return relations.toJavaList(String.class);
    }

    public ProjectComponent clone(String newProjectId, long createBy) {
        return ProjectComponent.builder()
                .id(generateId(_type))
                .projectId(newProjectId)
                .rootComponentId(_rootComponentId)
                .type(_type)
                .properties(_properties.clone(createBy))
                .build();
    }

    public void move2Trash() {
        try {
            _properties.getData().put("parentId", null);
        } catch (Exception ex) {
            throw new RestException(StatusCodes.AiProjectComponentTrashFailed);
        }
    }

    public void trashPutback(String parentId) {
        _properties.getData().put("parentId", parentId);
    }

    /**
     * 统一前缀
     * flow：c_
     * Faq：f_
     * 其他类型的Component: cp_
     */
    public static String generateId(String type) {
        //1.是否是root节点
        String prefix;
        switch (type) {
            case TYPE_CONVERSATION:
                prefix = ID_PREFIX_FLOW;
                break;
            default:
                prefix = ID_PREFIX_OTHERS;
        }

        return prefix + Base36.toString(IdGenerator.generateId());
    }

    public boolean isRoot() {
        return TYPE_CONVERSATION.equals(_type);
    }

    public static ProjectComponent createProjectComponentFromDao(String id, String projectId,
                                                                 String rootComponentId, String type,
                                                                 String properties) {
        return ProjectComponent.builder()
                .id(id).projectId(projectId).rootComponentId(rootComponentId)
                .type(type).properties(JSONObject.parseObject(properties, Prop.class))
                .build();
    }

    /**
     * update relations when clone from template project:
     * 1.projectId
     * 2.relations
     * 3.parentId
     */
    public void updateRelations(Map<String, String> map) {
        String data = _properties.getData().toJSONString();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            data = data.replaceAll(entry.getKey(), entry.getValue());
        }
        _properties.setData(JSONObject.parseObject(data));

        // update root componentId
        if (this._rootComponentId != null) {
            this._rootComponentId = map.get(this._rootComponentId);
        }
    }

    public String parseParentId() {
        try {
            return _properties.getData().getString("parentId");
        } catch (Exception ex) {
            LOG.warn("can't find this component parentId. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }


    /**
     * 这有两个目的
     *  - 如果有错误，更新到节点
     *  - 如果现在没错了，之前有错误，不更新到db
     * @return removed Validator error
     */
    public boolean appendValidatorError(ValidatorError validatorError) {
        JSONObject data = this._properties.getData().getJSONObject("data");
        // 设置 错误码
        if (validatorError != null) {
            data.put(Prop.VALIDATOR_ERROR_KEY, validatorError);
            this._properties.getData().put("data", data);
            return true;
        }

        // 清理错误码
        if (data != null && data.get(Prop.VALIDATOR_ERROR_KEY) != null) {
            data.remove(Prop.VALIDATOR_ERROR_KEY);
            this._properties.getData().put("data", data);
            return true;
        }

        return false;
    }

    /**
     * 保存节点是否可训练
     *
     * @param ready true 可训练，false，不可训练
     */
    public void appendReady(boolean ready) {
        this._properties.getData().put(Prop.READY_KEY, ready);
    }

    public String parseSlotId() {
        try {
            return _properties.getData().getJSONObject("data").getString("slotId");
        } catch (Exception ex) {
            LOG.warn("can't find this component slotId. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseFormId() {
        try {
            return _properties.getData().getJSONObject("data").getString(Prop.FORM_ID_KEY);
        } catch (Exception ex) {
            LOG.warn("can't find this component formId. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public void updateFormId(String formId) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put(Prop.FORM_ID_KEY, formId);
        _properties.getData().put("data", data);
    }

    public void updateContent(String content) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put(Prop.KB_CONTENT_KEY, content);
        _properties.getData().put("data", data);
    }

    public void updateTrainStatus(String trainStatus) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put("trainStatus", trainStatus);
        _properties.getData().put("data", data);
    }

    public void updateSetSlots(List<SlotPojo> setSlots) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put("setSlots", setSlots);
        _properties.getData().put("data", data);
    }

    public void updateTotalChunks(int totalChunks) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put("totalChunks", totalChunks);
        _properties.getData().put("data", data);
    }

    public void updateLinkId(String linkId) {
        JSONObject data = _properties.getData().getJSONObject("data");
        data.put("linkId", linkId);
        _properties.getData().put("data", data);
    }

    public String parseConditionId() {
        try {
            return _properties.getData().getJSONObject("data").getString("conditionId");
        } catch (Exception ex) {
            LOG.warn("can't find this component conditionId. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseBreakId() {
        try {
            return _properties.getData().getJSONObject("data").getString("breakId");
        } catch (Exception ex) {
            LOG.warn("can't find this component breakId. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseSlotName() {
        try {
            return _properties.getData().getJSONObject("data").getString("slotName");
        } catch (Exception ex) {
            LOG.warn("can't find this component slotName. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseStatus() {
        try {
            return _properties.getData().getJSONObject("data").getString("status");
        } catch (Exception ex) {
            LOG.warn("can't find this kb-url component status. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseName() {
        try {
            String name = _properties.getData().getJSONObject("data").getString("name");
            return name == null ? "" : name;
        } catch (Exception ex) {
            LOG.warn("can't find this component root name. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public String parseContent() {
        try {
            return _properties.getData().getJSONObject("data").getString("content");
        } catch (Exception ex) {
            LOG.warn("can't find this component content. projectId:{},id:{}", _projectId, _id);
            return null;
        }
    }

    public String parseKbTrainStatus() {
        try {
            String trainStatus = _properties.getData().getJSONObject("data").getString("trainStatus");
            return trainStatus == null ? "" : trainStatus;
        } catch (Exception ex) {
            LOG.warn("[can't find this component trainStatus. projectId:{}, id:{}]", _projectId, _id);
        }
        return "";
    }

    public String parseKbContent() {
        return parseContent();
    }

    public String parseFileId() {
        try {
            String fileId = _properties.getData().getJSONObject("data").getString("fileId");
            return fileId == null ? "" : fileId;
        } catch (Exception ex) {
            LOG.warn("[can't read file from:{} in projectId:{}]", _id, _projectId);
        }
        return "";
    }

    public String parseUrl() {
        try {
            String fileId = _properties.getData().getJSONObject("data").getString("url");
            return fileId == null ? "" : fileId;
        } catch (Exception ex) {
            LOG.warn("[can't read url from:{} in projectId:{}]", _id, _projectId);
        }
        return "";
    }

    public String parseDisplay() {
        try {
            String display = _properties.getData().getJSONObject("data").getString("display");
            return display == null ? "" : display;
        } catch (Exception ex) {
            LOG.warn("can't find this component display. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public boolean hasError() {
        return !parseIsReady() || StringUtils.isNotBlank(parseError());
    }

    public Boolean parseIsReady() {
        try {
            Boolean isReady = _properties.getData().getBoolean(Prop.READY_KEY);
            return isReady == null || isReady;
        } catch (Exception ex) {
            LOG.warn("can't find this component isReady. projectId:{},id:{}", _projectId, _id);
        }
        return true;
    }

    public void setParentId(String parentId) {
        if (_properties.getData() != null) {
            _properties.getData().put("parentId", parentId);
        }
    }

    /**
     * 查找组件id列表中的所有子节点
     * <p>
     * TODO fix me:这实现效率太低了
     *
     * @param ids 组件id列表
     * @param all project下的所有组件
     * @return
     */
    @Deprecated
    public static List<ProjectComponent> queryChildren(List<String> ids, List<ProjectComponent> all) {
        //根据父节点id查询所有父节id与之相等的节点
        List<ProjectComponent> components = all.stream()
                .filter(item -> ids.contains(item.parseParentId()))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(components)) {
            //拿到查询到的节点id
            List<String> parentIds = components.stream()
                    .map(ProjectComponent::getId)
                    .collect(Collectors.toList());

            //将拿到的节点id作为父节点id继续查询
            components.addAll(queryChildren(parentIds, all));
            return components;
        } else {
            //如果没有那么我们就返回空集合，结束递归。
            return new ArrayList();
        }
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComponentRelation {

        public static ComponentRelation empty() {
            return ComponentRelation.builder()
                    .ownedByComponentRoot(ComponentRelationInfo.empty())
                    .usedByComponentRoots(new ArrayList<>())
                    .build();
        }

        /**
         * 拥有者component root id，可创建编辑对应的intent
         */
        private ComponentRelationInfo _ownedByComponentRoot;
        /**
         * 使用者component root id，目前仅可以引用
         */
        private List<ComponentRelationInfo> _usedByComponentRoots;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComponentRelationInfo {
        public static ComponentRelationInfo empty() {
            return ComponentRelationInfo.builder()
                    .build();
        }

        public static ComponentRelationInfo factory(ProjectComponent rootComponent, String id, String componentName) {
            return ComponentRelationInfo.builder()
                    .rootComponentId(rootComponent.getId())
                    .rootComponentName(rootComponent.parseName())
                    .rootComponentType(rootComponent.getType())
                    .componentName(componentName).componentId(id)
                    .build();
        }

        /**
         * 被引用的根节点id
         */
        private String _rootComponentId;
        private String _rootComponentName;
        private String _rootComponentType;
        private String _componentId;
        private String _componentName;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ComponentRelationInfo that = (ComponentRelationInfo) o;
            return _componentId.equals(that._componentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_componentId);
        }
    }

    /**
     * 如果有新的child节点来了，那么使用这个方法分配一个
     * 注意：调用该方法后，需要保存当前节点的properties
     */
    public Integer generateNewChildNo() {
        return _properties._nextChildNo++;
    }

    @Getter
    @Setter
    @SuperBuilder
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Prop extends BaseProp {
        /**
         * 保存节点完整性的key
         */
        public static final String VALIDATOR_ERROR_KEY = "validatorError";

        /**
         * 当前flow是否完整，一般root节点才有这个key
         */
        public static final String READY_KEY = "isReady";

        public static final String FORM_ID_KEY = "formId";
        public static final String KB_CONTENT_KEY = "content";

        private JSONObject _data;

        /**
         * 孩子节点的编号,这里需要从父节点进行分配
         */
        @Builder.Default
        private Integer _no = 0;

        /**
         * 维护孩子节点的编号
         */
        @Builder.Default
        private Integer _nextChildNo = 0;


        public static Prop empty() {
            return Prop.builder()
                    .createTime(System.currentTimeMillis())
                    .build();
        }

        public static Prop empty(long userId) {
            return Prop.builder()
                    .createTime(System.currentTimeMillis())
                    .createBy(userId)
                    .build();
        }

        public Prop clone(long createBy) {
            return Prop.builder()
                    .createBy(createBy)
                    .createTime(System.currentTimeMillis())
                    .data(JSONObject.parseObject(this.getData().toJSONString()))
                    .build();
        }
    }

    public String parseError() {
        JSONObject data = this._properties.getData().getJSONObject("data");
        JSONObject error = data.getJSONObject(Prop.VALIDATOR_ERROR_KEY);
        return error == null ? null : error.toJavaObject(ValidatorError.class).buildError();
    }


    public Set<ProjectComponent.Label> parseLabels() {
        try {
            if (_properties.getData().getJSONObject("data").containsKey("labels")) {
                List<Label> labels = _properties.getData()
                        .getJSONObject("data").getJSONArray("labels").toJavaList(Label.class);
                return new HashSet<>(labels);
            }
        } catch (Exception ex) {
            LOG.warn("can't find this component labels. projectId:{},id:{}", _projectId, _id);
        }
        return new HashSet<>();
    }

    public String parseOriginal() {
        try {
            String original = _properties.getData().getJSONObject("data").getString("original");
            return original == null ? "" : original;
        } catch (Exception ex) {
            LOG.warn("can't find this component root original. projectId:{},id:{}", _projectId, _id);
        }
        return "";
    }

    public Boolean parseEnable() {
        try {
            Boolean enable = _properties.getData().getJSONObject("data").getBoolean("enable");
            return enable == null || enable;
        } catch (Exception ex) {
            LOG.warn("can't find this component root original. projectId:{},id:{}", _projectId, _id);
        }
        return true;
    }

    public boolean matchLabel(String criteriaLabel) {
        Set<ProjectComponent.Label> labels = parseLabels();
        ProjectComponent.Label existLabel = labels.stream()
                .filter(label -> StrUtil.containsAnyIgnoreCase(label.getText(), criteriaLabel))
                .findAny().orElse(null);
        return existLabel != null;
    }

    public boolean marchOriginal(String criteriaOriginal) {
        String original = parseOriginal();
        return StrUtil.containsAnyIgnoreCase(original, criteriaOriginal);
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Label {
        private String _text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProjectComponent component = (ProjectComponent) o;
        return _id.equals(component._id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id);
    }
}
