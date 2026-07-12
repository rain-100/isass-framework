package vip.isass.framework.nocode.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.lang.tree.parser.NodeParser;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 列表转树结构工具（nocode）。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 方式一：函数式（推荐）
 * List<Menu> tree = TreeEntityUtil.toTree(menus,
 *         Menu::getId, Menu::getParentId, Menu::getChildren, Menu::setChildren);
 *
 * // 方式二：反射
 * List<Menu> tree = TreeEntityUtil.toTree(menus, "id", "parentId", "children");
 *
 * // 方式三：hutool Tree builder
 * List<Menu> tree = TreeEntityUtil.toTree(menus, null,
 *         (menu, node) -> node.setId(menu.getId()).setParentId(menu.getParentId()),
 *         Menu::from,
 *         Menu::getChildren, Menu::setChildren);
 * }</pre>
 */
public final class TreeEntityUtil {

    private TreeEntityUtil() {
    }

    // ==================== toTree: 函数式 ====================

    /**
     * 将实体列表转为树形结构（函数式，推荐）。
     *
     * @param entities         实体列表
     * @param idGetter         主键获取器
     * @param parentIdGetter   父 ID 获取器
     * @param childrenGetter   children getter
     * @param childrenSetter   children setter
     * @param <PK>             主键类型
     * @param <E>              实体类型
     * @return 树形结构（顶层节点列表）
     */
    public static <PK extends Serializable, E> List<E> toTree(
            List<E> entities,
            Function<E, PK> idGetter,
            Function<E, PK> parentIdGetter,
            Function<E, List<E>> childrenGetter,
            BiConsumer<E, List<E>> childrenSetter) {
        if (CollUtil.isEmpty(entities)) {
            return Collections.emptyList();
        }

        Map<PK, E> entityMap = MapUtil.newHashMap(entities.size());
        List<E> roots = new ArrayList<>();
        List<E> orphans = new ArrayList<>();

        for (E entity : entities) {
            PK id = idGetter.apply(entity);
            Assert.notNull(id, "存在主键为空的实体");
            entityMap.put(id, entity);

            PK parentId = parentIdGetter.apply(entity);
            if (parentId == null || parentId.equals(id)) {
                roots.add(entity);
            } else {
                E parent = entityMap.get(parentId);
                if (parent == null) {
                    orphans.add(entity);
                } else {
                    mergeChild(parent, childrenGetter, childrenSetter, entity);
                }
            }
        }

        for (E entity : orphans) {
            PK parentId = parentIdGetter.apply(entity);
            E parent = entityMap.get(parentId);
            if (parent == null) {
                roots.add(entity);
            } else {
                mergeChild(parent, childrenGetter, childrenSetter, entity);
            }
        }

        return roots;
    }

    // ==================== toTree: 反射 ====================

    /**
     * 将实体列表转为树形结构（反射，按字段名）。
     *
     * @param entityList            实体列表
     * @param idFieldName           id 字段名
     * @param parentIdFieldName     parentId 字段名
     * @param childrenFieldName     children 字段名
     * @param topLevelIdValueArr    顶层实体 id（parentId 为空但 id 在此数组中也视为顶层）
     * @param <T>                   实体类型
     * @return 树形结构（顶层节点列表）
     */
    @SuppressWarnings("uncheckeded")
    public static <T> List<T> toTree(List<T> entityList,
                                      String idFieldName,
                                      String parentIdFieldName,
                                      String childrenFieldName,
                                      String... topLevelIdValueArr) {
        if (CollUtil.isEmpty(entityList)) {
            return Collections.emptyList();
        }

        Assert.notBlank(idFieldName, "idFieldName 必填");
        Assert.notBlank(parentIdFieldName, "parentIdFieldName 必填");
        Assert.notBlank(childrenFieldName, "childrenFieldName 必填");
        List<String> topLevelIdValues = ArrayUtil.isEmpty(topLevelIdValueArr)
                ? Collections.emptyList()
                : Arrays.asList(topLevelIdValueArr);

        Map<String, T> entityMap = MapUtil.newHashMap(entityList.size());
        List<T> tempList = new ArrayList<>();
        List<T> result = new ArrayList<>();

        for (T entity : entityList) {
            String id = Optional.ofNullable(ReflectUtil.getFieldValue(entity, idFieldName))
                    .map(Object::toString).orElse("");
            Assert.notBlank(id, "存在主键为空的实体");
            entityMap.put(id, entity);

            String parentId = Optional.ofNullable(ReflectUtil.getFieldValue(entity, parentIdFieldName))
                    .map(Object::toString).orElse("");
            if (StrUtil.isBlank(parentId) || topLevelIdValues.contains(id)) {
                result.add(entity);
            } else {
                T parentEntity = entityMap.get(parentId);
                if (parentEntity == null) {
                    tempList.add(entity);
                } else {
                    mergeChild(parentEntity, childrenFieldName, entity);
                }
            }
        }

        for (T entity : tempList) {
            String parentId = Optional.ofNullable(ReflectUtil.getFieldValue(entity, parentIdFieldName))
                    .map(Object::toString).orElse("");
            T parentEntity = entityMap.get(parentId);
            if (parentEntity == null) {
                result.add(entity);
            } else {
                mergeChild(parentEntity, childrenFieldName, entity);
            }
        }
        return result;
    }

    // ==================== toTree: hutool Tree builder ====================

    /**
     * 基于 hutool {@link TreeUtil} 构建并转换实体树。
     *
     * @param entities              实体列表
     * @param parentId              根节点 parentId（通常为 null）
     * @param entityToTreeNode      实体 → hutool TreeNode 转换器
     * @param treeNodeToEntity      hutool Tree → 目标实体 转换器
     * @param childrenGetter        children getter
     * @param childrenSetter        children setter
     * @param <TE>                  TreeNode id 类型
     * @param <E>                   源实体类型
     * @param <R>                   目标实体类型
     * @return 树形结构
     */
    public static <TE, E, R> List<R> toTree(List<E> entities,
                                             TE parentId,
                                             NodeParser<E, TE> entityToTreeNode,
                                             Function<Tree<TE>, R> treeNodeToEntity,
                                             Function<R, List<R>> childrenGetter,
                                             BiConsumer<R, List<R>> childrenSetter) {
        return toTree(entities, parentId, TreeNodeConfig.DEFAULT_CONFIG,
                entityToTreeNode, treeNodeToEntity, childrenGetter, childrenSetter);
    }

    /**
     * 基于 hutool {@link TreeUtil} 构建并转换实体树（自定义 {@link TreeNodeConfig}）。
     */
    public static <TE, E, R> List<R> toTree(List<E> entities,
                                             TE parentId,
                                             TreeNodeConfig treeNodeConfig,
                                             NodeParser<E, TE> entityToTreeNode,
                                             Function<Tree<TE>, R> treeNodeToEntity,
                                             Function<R, List<R>> childrenGetter,
                                             BiConsumer<R, List<R>> childrenSetter) {
        List<Tree<TE>> trees = TreeUtil.build(entities, parentId, treeNodeConfig, entityToTreeNode);
        return toTree(trees, treeNodeToEntity, childrenGetter, childrenSetter);
    }

    private static <TE, R> List<R> toTree(List<Tree<TE>> treeNodes,
                                           Function<Tree<TE>, R> converter,
                                           Function<R, List<R>> childrenGetter,
                                           BiConsumer<R, List<R>> childrenSetter) {
        if (CollUtil.isEmpty(treeNodes)) {
            return Collections.emptyList();
        }
        List<R> list = new ArrayList<>(treeNodes.size());
        for (Tree<TE> treeNode : treeNodes) {
            R r = converter.apply(treeNode);
            childrenSetter.accept(r, toTree(treeNode.getChildren(), converter, childrenGetter, childrenSetter));
            list.add(r);
        }
        return list;
    }

    // ==================== 内部工具 ====================

    private static <E> void mergeChild(E parent,
                                        Function<E, List<E>> childrenGetter,
                                        BiConsumer<E, List<E>> childrenSetter,
                                        E child) {
        List<E> children = childrenGetter.apply(parent);
        if (children == null) {
            children = new ArrayList<>();
            childrenSetter.accept(parent, children);
        }
        children.add(child);
    }

    @SuppressWarnings("unchecked")
    private static <T> void mergeChild(T parentEntity, String childrenFieldName, T entity) {
        Object children = ReflectUtil.getFieldValue(parentEntity, childrenFieldName);
        List<T> childrenList;
        if (children == null) {
            childrenList = new ArrayList<>();
            ReflectUtil.setFieldValue(parentEntity, childrenFieldName, childrenList);
        } else {
            childrenList = (List<T>) children;
        }
        childrenList.add(entity);
    }
}
