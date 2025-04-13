package com.zervice.common.pojo.chat;

import com.alibaba.fastjson.JSONObject;

/**
 * 包装一些不解析的节点
 *
 * @author chen
 * @date 2022/10/14
 */
public class UnDecodeComponentPojo extends BaseComponentPojo {

    public UnDecodeComponentPojo(JSONObject component) {
        this._component = component;
    }

    private JSONObject _component;

    @Override
    public String getName() {
        return getType();
    }

    @Override
    public String getType() {
        return _component.getString("type");
    }

    @Override
    public String getId() {
        return _component.getString("id");
    }


    @Override
    public String getParentId() {
        return _component.getString("parentId");
    }

    public JSONObject getComponent() {
        return _component;
    }
}
