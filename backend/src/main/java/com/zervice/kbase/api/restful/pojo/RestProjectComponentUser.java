package com.zervice.kbase.api.restful.pojo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.kbase.api.restful.pojo.mica.SlotPojo;
import com.zervice.kbase.database.pojo.ProjectComponent;
import com.zervice.kbase.utils.StrUtil;
import com.zervice.kbase.validator.error.ValidatorError;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Peng Chen
 * @date 2022/6/23
 */
@Log4j2
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"examples"})
public class RestProjectComponentUser extends RestBaseProjectComponent {
    public static final String TYPE_NAME = ProjectComponent.TYPE_USER;

    public static RestProjectComponentUser newInstance(String projectId, String parentId, String rootComponentId, List<String> exampleSet) {
        List<Example> examples = exampleSet.stream()
                .map(Example::factory)
                .collect(Collectors.toList());

        Data data = new Data();
        data.setExamples(examples);
        data.setName(exampleSet.get(0));

        return newInstance(projectId, parentId, rootComponentId, data);
    }
    public static RestProjectComponentUser newEmptyInstance(String projectId, String parentId, String rootComponentId) {
        return newInstance(projectId, parentId, rootComponentId, new Data());
    }

    public static RestProjectComponentUser newInstance(String projectId, String parentId, String rootComponentId, Data data) {
        RestProjectComponentUser user = new RestProjectComponentUser();
        user.setId(ProjectComponent.generateId(TYPE_NAME));
        user.setRootComponentId(rootComponentId);
        user.setParentId(parentId);
        user.setProjectId(projectId);
        user.setType(TYPE_NAME);

        user.setData(data);
        return user;
    }



    public RestProjectComponentUser(ProjectComponent component) {
        super(component);

        this._id = component.getId();
        this._type = component.getType();
        this._projectId = component.getProjectId();
        this._rootComponentId = component.getRootComponentId();
        this._properties = component.getProperties();
        this._no = component.getProperties().getNo();

        JSONObject data = component.getProperties().getData();

        this._parentId = data.getString("parentId");

        if (data.containsKey("data") && data.getJSONObject("data") != null) {
            this._data = data.getJSONObject("data").toJavaObject(Data.class);

            this._data.setValidatorError(_validatorError);
        }

        if (data.containsKey("relations") && data.getJSONArray("relations") != null) {
            this._relations = data.getJSONArray("relations").toJavaList(String.class);
        }
    }

    /**
     * 是否使用了 Entity
     *
     * @param entityId entityID
     * @return
     */
    public boolean entityInUse(String entityId) {
        if (entityId == null) {
            return false;
        }

        List<Mapping> mappings = _data.getMappings();
        List<SlotPojo> setSlots = _data.getSetSlots();

        if (CollectionUtils.isNotEmpty(mappings)) {
            for (Mapping m : mappings) {
                if (entityId.equals(m.getSlotId())) {
                    return true;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(setSlots)) {
            for (SlotPojo s : setSlots) {
                if (entityId.equals(s.getSlotId())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String _id;
    private String _type;
    private String _projectId;
    private String _rootComponentId;

    private String _parentId;

    private List<String> _relations;

    private Data _data;

    public static RestProjectComponentUser factory(String id, String parentId, String projectId,
                                                   String display, Boolean mappingEnable,
                                                   List<Example> examples, List<Mapping> mappings,
                                                   Boolean virtual) {
        boolean supportDisplay = Data.supportDisplay(display);
        if (!supportDisplay) {
            throw new RestException(StatusCodes.UnsupportedUserDisplay);
        }

        Data data = Data.builder()
                .mappings(mappings)
                .mappingsEnable(mappingEnable)
                .examples(examples)
                .display(display)
                .virtual(virtual)
                .build();

        return RestProjectComponentUser.builder()
                .id(id)
                .data(data).type(TYPE_NAME)
                .parentId(parentId).projectId(projectId)
                .build();
    }

    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private ProjectComponent.Prop _properties;

    /**
     * 查找意图中试用的变量di
     */
    public List<String> findUsedEntityIds() {
        // 没开启转换
        if (!Boolean.TRUE.equals(_data._mappingsEnable)) {
            return null;
        }

        if (CollectionUtils.isEmpty(_data._mappings)) {
            return null;
        }

        return _data._mappings.stream()
                .map(Mapping::getSlotId)
                .collect(Collectors.toList());
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        /** 用户点击*/
        public static final String DISPLAY_CLICK = "user_click";
        /** 用户输入*/
        public static final String DISPLAY_INPUT = "user_inout";
        private static final Set<String> _SUPPORT_DISPLAY = new HashSet<>();
        static {
            _SUPPORT_DISPLAY.add(DISPLAY_CLICK);
            _SUPPORT_DISPLAY.add(DISPLAY_INPUT);
        }

        public static boolean supportDisplay(String display) {
            return _SUPPORT_DISPLAY.contains(display);
        }

        private String _name;

        private String _description;
        private Integer _errorCode;

        private List<Example> _examples;

        private Boolean _mappingsEnable;

        private List<Mapping> _mappings;

        private Boolean _rhetoricalEnable;

        private String _rhetorical;

        @Builder.Default
        private Boolean _enable = true;

        private List<SlotPojo> _setSlots;
        @Builder.Default
        private List<ProjectComponent.Label> _labels = new ArrayList<>();
        /**
         * 前端按照display展示为user_click/user_input
         */
        private String _display;

        /**
         * 是否虚拟的user节点，即后端转换文件时动态生成的
         */
        private Boolean _virtual;

        /**
         * 训练状态
         */
        private String _trainStatus;

        /**
         * 训练完成chunks
         */
        private Integer _finishChunks;
        /**
         * 总chunk
         */
        private Integer _totalChunks;

        /**
         * 记录text chunk 的 md5, 便于判断chunk是否需要更新
         */
        private String _chunkMd5;

        private ValidatorError _validatorError;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mapping {

        public static final String TYPE_FROM_ENTITY = "from_entity";
        public static final String TYPE_FROM_TEXT = "from_text";
        public static final String TYPE_FROM_INTENT = "from_intent";
        private static final Set<String> TYPES = new HashSet<>();
        static {
            TYPES.add(TYPE_FROM_ENTITY);
            TYPES.add(TYPE_FROM_TEXT);
            TYPES.add(TYPE_FROM_INTENT);
        }

        private String _id;
        private String _slotId;
        private String _slotName;
        private String _slotDisplay;
        private String _type;
        @Builder.Default
        private Boolean _multiInput = false;
        private String _value;
        private Boolean _enable;

        /**
         * 多变量标注，虚拟节点绑定的 ComponentFiledId
         */
        private String _targetId;

        /**
         *
         * 在多变量标注时，比如：从成都到北京
         * 这里包含"出发地点"、"到达地点"，但是变量都是地点，这里需要区分变量应该怎么填。
         *
         * 这里出现的值，就是"出发地点"、"到达地点"
         */
        private String _role;

        public static Mapping factory(String slotId,String type,
                                      String targetId, boolean enable,
                                      String role) {
            checkType(type);

            return Mapping.builder()
                    .slotId(slotId).type(type)
                    .enable(enable).targetId(targetId)
                    .role(role)
                    .build();
        }

        private static void checkType(String type) {
            if (TYPES.contains(type)) {
                return;
            }

            throw new IllegalArgumentException("invalid user.Mapping type:" + type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Mapping mapping = (Mapping) o;
            return Objects.equals(_slotId, mapping._slotId) && Objects.equals(_targetId, mapping._targetId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(_slotId, _targetId);
        }

    }


    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String _text;
        private List<Mark> _marks;

        public static Example factory(String _text) {
            return new Example(_text, null);
        }

        public boolean empty() {
            return StringUtils.isBlank(_text);
        }

    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mark {
        private Integer _start;
        private Integer _end;
        private String _name;

        /**
         * 多变量标注，虚拟节点绑定的 ComponentFiledId
         */
        private String _targetId;

        private String _entityId;
        private Boolean _markRole;

        /**
         * 将标注的字符取出来
         *
         * @param sentence
         * @return
         */
        public String take(String sentence) {
            return sentence.substring(_start, _end);
        }

        /**
         * 我想明天从[西安]{\"entity\": \"站点\", \"role\": \"出发站点\"}
         */
        public String mark(String take, String name, String role) {
            JSONObject mark = new JSONObject();
            mark.put("entity", name);
            if (StringUtils.isNotBlank(role) && Boolean.TRUE.equals(_markRole)) {
                mark.put("role", role);
            }
            return "[" + take + "]" + mark.toJSONString();
        }

        /**
         * 筛选用一句标注中出现多次entityId
         * 即：需要role的情况，比如 出发站点、到达站点都是 站点这个entity
         */
        public static Set<String> filterEntityMoreThanOnce(List<Mark> marks) {
            Map<String, List<String>> entityMap = marks.stream()
                    .map(Mark::getEntityId)
                    .collect(Collectors.groupingBy(Object::toString));

            Set<String> repeatEntities = Sets.newHashSetWithExpectedSize(entityMap.size());
            for (Map.Entry<String, List<String>> entry : entityMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    repeatEntities.add(entry.getKey());
                }
            }

            return repeatEntities;
        }
    }

    @Override
    public ProjectComponent toProjectComponent() {
        _data.setValidatorError(_validatorError);
        JSONObject data = new JSONObject();
        data.put("parentId", _parentId);
        data.put("data", _data);
        data.put("relations", _relations);
        if (_properties == null) {
            _properties = ProjectComponent.Prop.empty();
        }
        _properties.setData(data);

        return ProjectComponent.builder()
                .id(_id).type(_type)
                .projectId(_projectId)
                .rootComponentId(_rootComponentId)
                .properties(_properties)
                .build();
    }

    /**
     * 这个方法可能重复了
     * 这会重复调用，所以把他缓存起来
     *
     * @return 如果是空list，不能进行编辑
     */
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    private List<String> _examples;
    public List<String> examples() {
        if (_examples == null) {
            if (CollectionUtils.isEmpty(_data._examples)) {
                _examples = Collections.emptyList();
                return _examples;
            }

            _examples = new ArrayList<>();
            for (Example example : _data._examples) {
                _examples.add(example.getText());
            }
        }

        return _examples;
    }

    /**
     * mark 检查，start end不能越界
     *
     * @return
     */
    public static boolean validateMark(List<Example> examples) {
        AtomicReference<Boolean> result = new AtomicReference<>(true);
        if (CollectionUtils.isNotEmpty(examples)) {
            examples.forEach(item -> {
                if (CollectionUtils.isNotEmpty(item.getMarks())) {
                    item.getMarks().forEach(mark -> {
                        long textLen = item.getText().length();
                        long start = mark.getStart();
                        long end = mark.getEnd();
                        if (start > textLen || end > textLen) {
                            result.set(false);
                        }
                    });
                }
            });
        }
        return result.get();
    }


    public boolean matchExample(String criteriaExample) {
        if (_data == null || CollectionUtils.isEmpty(_data.getExamples())) {
            return false;
        }

        RestProjectComponentUser.Example existExample = _data.getExamples().stream()
                .filter(example -> StrUtil.containsAnyIgnoreCase(example.getText(), criteriaExample))
                .findAny().orElse(null);
        return existExample != null;
    }
}
