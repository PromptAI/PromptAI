package com.zervice.kbase.validator.component;

import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.validator.ComponentValidateAble;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 验证数据库节点是否正确
 *
 * @author chen
 * @date 2022/10/25
 */
public abstract class BaseComponentValidator implements ComponentValidateAble {


    public BaseComponentValidator(RestBaseProjectComponent component) {
        this._component = component;
        this._id = component.getId();
        this._parentId = component.getParentId();
        this._type = component.getType();
    }

    @Setter
    @Getter
    protected List<BaseComponentValidator> _children = new ArrayList<>();

    @Setter
    @Getter
    protected RestBaseProjectComponent _component;

    @Getter
    private String _id;

    @Getter
    private String _parentId;

    @Getter
    private String _type;

    public void addChild(BaseComponentValidator child) {
        _children.add(child);
    }


    public boolean hasChildren(String type) {
        if (CollectionUtils.isEmpty(_children)) {
            return false;
        }

        for (BaseComponentValidator child : _children) {
            if (type.equals(child.getType())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasChildren() {
        if (CollectionUtils.isEmpty(_children)) {
            return false;
        }
        return true;
    }

    public static BaseComponentValidator buildTree(String rootId, List<BaseComponentValidator> rests) {
        Optional<BaseComponentValidator> rootOp = rests.stream()
                .filter(v -> rootId.equals(v.getId()))
                .findFirst();

        if (rootOp.isPresent()) {
            BaseComponentValidator root = rootOp.get();
            _findChild(root, rests);
            return root;
        }

        return null;
    }

    private static void _findChild(BaseComponentValidator parent,
                                   List<BaseComponentValidator> rests) {
        String parentId = parent.getId();
        for (BaseComponentValidator validator : rests) {
            if (parentId.equals(validator.getParentId())) {
                parent.addChild(validator);

                _findChild(validator, rests);
            }
        }
    }
}
