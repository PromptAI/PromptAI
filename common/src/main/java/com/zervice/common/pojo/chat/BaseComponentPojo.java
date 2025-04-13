package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chen
 * @date 2022/8/25
 */
@Log4j2
public abstract class BaseComponentPojo {
    /**
     * 目前我们把faq的bot由broker驱动
     * 当faq的user 未启用时，则在backend的 '/rpc/project/component/children' 接口返回该值..
     * 此时bot回复 fallback
     */
    public static final String NOT_ENABLE_RES = "-1";
    public static final String DISPLAY_USER_CLICK = "user_click";
    public static final String DISPLAY_USER_INPUT = "user_input";
    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    protected List<BaseComponentPojo> children;

    public abstract String getName();

    public abstract String getType();

    public abstract String getId();

    public abstract String getParentId();

    public JSONArray toRes() {
        return new JSONArray();
    }


    @JsonIgnore
    @JSONField(serialize = false, deserialize = false)
    public List<BaseComponentPojo> getChildren() {
        return children;
    }

    public void addNext(BaseComponentPojo component) {
        if (children == null) {
            children = new ArrayList<>(16);
        }

        children.add(component);
    }


    public static BaseComponentPojo convert(JSONObject component, String chatId) {
        String type = component.getString("type");
        switch (type) {
            case ProjectComponentBotPojo.NAME:
                return component.toJavaObject(ProjectComponentBotPojo.class);
            case ProjectComponentConversationPojo.NAME:
                return component.toJavaObject(ProjectComponentConversationPojo.class);
            case ProjectComponentUserPojo.NAME:
                return component.toJavaObject(ProjectComponentUserPojo.class);
            case ProjectComponentGotoPojo.NAME:
                return component.toJavaObject(ProjectComponentGotoPojo.class);
            case ProjectComponentEntityPojo.TYPE_NAME:
                return component.toJavaObject(ProjectComponentEntityPojo.class);
            default:
                LOG.warn("[{}]un decode component type:{}", chatId, type);
                return new UnDecodeComponentPojo(component);
        }
    }

    /**
     * 构造一棵树，这里可能是会话流程的一个局部
     *
     * @param topParentId 这里是某一部分的根节点，不一定是conversation或faq
     */
    public static BaseComponentPojo buildPartTree(List<BaseComponentPojo> children, String topParentId) {
        // id -> component
        Map<String, BaseComponentPojo> componentPojoMap = children.stream()
                .collect(Collectors.toMap(BaseComponentPojo::getId, c -> c));

        // find top parent
        BaseComponentPojo parent = componentPojoMap.get(topParentId);

        // build tree
        buildComponentTree(parent, children);

        return parent;
    }

    public static boolean isFaqId(String id) {
        return id != null && id.startsWith("f_");
    }

    public static void buildComponentTree(BaseComponentPojo parent,
                                          List<BaseComponentPojo> all) {
        for (BaseComponentPojo c : all) {
            if (StringUtils.isNotBlank(c.getParentId()) && c.getParentId().equals(parent.getId())) {
                parent.addNext(c);
                buildComponentTree(c, all);
            }
        }
    }

    @JSONField(serialize = false, deserialize = false)
    @JsonIgnore
    public boolean isUserClick() {
        if (ProjectComponentUserPojo.NAME.equals(this.getType())) {
            ProjectComponentUserPojo userPojo = (ProjectComponentUserPojo) this;
            return DISPLAY_USER_CLICK.equals(userPojo.getData().getDisplay());
        }
        return false;
    }

    /**
     * 判断是否是可以由后端驱动的user点击节点：是用户点击、无重置变量、无变量提取
     * @return
     */
    public boolean isCanBackendDriveUser() {
        if (ProjectComponentUserPojo.NAME.equals(this.getType())) {
            ProjectComponentUserPojo userPojo = (ProjectComponentUserPojo) this;

            return CollectionUtils.isEmpty(userPojo.getData().getMappings())
                    && DISPLAY_USER_CLICK.equals(userPojo.getData().getDisplay())
                    && !userPojo.hasSlots();
        }
        return false;
    }

}
