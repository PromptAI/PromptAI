package com.zervice.kbase.validator.component;

import com.google.common.collect.Maps;
import com.zervice.kbase.api.restful.pojo.RestBaseProjectComponent;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentGpt;
import com.zervice.kbase.api.restful.pojo.RestProjectComponentUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 保存校验节点的上下文
 *
 * @author chen
 * @date 2022/10/27
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentValidatorContext {

    @Getter
    private List<RestBaseProjectComponent> _currentRootComponents;

    /**
     * 节点错误信息校验的时候需要使用：
     * 项目下的所有节点，且不在回收站里面
     */

    private List<RestBaseProjectComponent> _projectComponentsNotInTrash;

    @Getter
    private String _rootComponentId;

    @Getter
    private String _dbName;

    private volatile Map<String, RestBaseProjectComponent> _componentMap;

    private volatile List<RestProjectComponentUser> _notTrashedUser;
    private volatile List<RestProjectComponentGpt> _notTrashedGpt;

    public List<RestProjectComponentGpt> getNotTrashedGPT() {
        if (_notTrashedGpt == null) {
            synchronized (this) {
                if (_notTrashedGpt == null) {
                    _notTrashedGpt = _projectComponentsNotInTrash.stream()
                            .filter(c -> c instanceof RestProjectComponentGpt)
                            .map(c -> (RestProjectComponentGpt) c)
                            .collect(Collectors.toList());
                }
            }
        }

        return _notTrashedGpt;
    }

    /**
     * 获取正常节点的user
     */
    public List<RestProjectComponentUser> getNotTrashedUser() {
        if (_notTrashedUser == null) {
            synchronized (this) {
                if (_notTrashedUser == null) {
                    _notTrashedUser = _projectComponentsNotInTrash.stream()
                            .filter(c -> c instanceof RestProjectComponentUser)
                            .map(c -> (RestProjectComponentUser) c)
                            .collect(Collectors.toList());
                }
            }
        }

        return _notTrashedUser;
    }

    /**
     * 将正常的节点建立一个 ext -> id map,
     * 如： 你好： 1，2 ()
     *
     * 以减少重复比较的次数
     */
    transient Map<String /* intent */, Set<String> /* user id */> _notTrashedExtUserIdMap;

   transient Map<String /* intent name */ , Set<String /* user id*/>> _notTrashedIntentNameUserIdMap;


    /**
     * 获取普通 user 节点的意图名称与 id
     */
    public Map<String /* intent name */ , Set<String /* user id*/>> notTrashedIntentNameUserIdMap() {
        if (_notTrashedIntentNameUserIdMap == null) {
            synchronized (this) {
                if (_notTrashedIntentNameUserIdMap == null) {
                    List<RestProjectComponentUser> notTrashedUser = getNotTrashedUser();

                    _notTrashedIntentNameUserIdMap = Maps.newHashMapWithExpectedSize(notTrashedUser.size());

                    for (RestProjectComponentUser user : notTrashedUser) {
                        String name = user.getData().getName();
                        String userId = user.getId();

                        // 有些可能没有 intent name
                        if (StringUtils.isBlank(name)) {
                            continue;
                        }

                        // 重复了，记录id
                        if (_notTrashedIntentNameUserIdMap.containsKey(name)) {
                            Set<String> userIdSet = _notTrashedIntentNameUserIdMap.get(name);
                            userIdSet.add(userId);
                            continue;
                        }

                        Set<String> userIdSet = new HashSet<>();
                        userIdSet.add(userId);
                        _notTrashedIntentNameUserIdMap.put(name, userIdSet);
                    }

                }
            }
        }

        return _notTrashedIntentNameUserIdMap;
    }

    /**
     * 这只包含普通user节点，不在模版中的
     *
     * 比较的时候，只需要通过map.get即可判断是否与数据库中的数据重复
     * 如果有重复，此时还要判断这个扩展问是否自来当前节点，即 userId.contain && userId.size==1 ?
     * 这样判断扩展问重复从 example.size * db.example.size 变成 example.size * 1 次
     */
    public Map<String /* intent */, Set<String> /* user id */> notTrashedExtUserIdMap() {
        if (_notTrashedExtUserIdMap == null) {
            synchronized (this) {
                if (_notTrashedExtUserIdMap == null) {
                    List<RestProjectComponentUser> notTrashedUser = getNotTrashedUser();

                    // 预估每个user大概10个训练语句
                    _notTrashedExtUserIdMap = Maps.newHashMapWithExpectedSize(10 * notTrashedUser.size());

                    for (RestProjectComponentUser user : notTrashedUser) {
                        List<String> examples = user.examples();
                        String userId = user.getId();

                        for (String ex : examples) {
                            // 训练语句没重复
                            if (!_notTrashedExtUserIdMap.containsKey(ex)) {
                                Set<String> userIdSet = new HashSet<>();
                                userIdSet.add(userId);
                                _notTrashedExtUserIdMap.put(ex, userIdSet);
                                continue;
                            }
                            // 重复了，记录id
                            Set<String> userIdSet = _notTrashedExtUserIdMap.get(ex);
                            userIdSet.add(userId);
                        }
                    }
                }
            }
        }

        return _notTrashedExtUserIdMap;
    }

    public  RestBaseProjectComponent getById(String id) {
        if (_componentMap == null) {
            synchronized (this) {
                if (_componentMap == null) {
                     _componentMap = _projectComponentsNotInTrash.stream()
                            .collect(Collectors.toMap(RestBaseProjectComponent::getId, r -> r));
                }
            }
        }

        return _componentMap.get(id);
    }

    /**
     * 获取指定类型的父节点
     *
     * @param parentId
     * @param type
     * @return
     */
    public RestBaseProjectComponent getParent(String parentId, String type) {
        Map<String, RestBaseProjectComponent> componentMap = _currentRootComponents.stream()
                .collect(Collectors.toMap(RestBaseProjectComponent::getId, r -> r));

        RestBaseProjectComponent parent = componentMap.get(parentId);
        if (parent == null) {
            return null;
        }

        if (type.equals(parent.getType())) {
            return parent;
        }

        return getParent(parent.getParentId(), type);
    }

    /**
     * 获取指定最近一级children
     * @param children
     * @param type
     * @return
     */
    public static List<RestBaseProjectComponent> getChildren(List<RestBaseProjectComponent> children, String type) {

        List<RestBaseProjectComponent> result = new ArrayList<>(children.size());
        _getChildren(children, result, type);

        return result;
    }

    private static void  _getChildren(List<RestBaseProjectComponent> children,
                                                               List<RestBaseProjectComponent> result,
                                                               String type) {
        if (CollectionUtils.isEmpty(children)) {
            return;
        }

        for (RestBaseProjectComponent child : children) {
            if (type.equals(child.getType())) {
                result.add(child);
                continue;
            }

            // 继续向下查找
            _getChildren(child.getChildren(), result, type);
        }
    }
}
