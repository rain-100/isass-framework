// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.baomidou.mybatisplus.annotation.DbType;
import vip.isass.framework.nocode.generator.association.EntityRelationDefinition;
import vip.isass.framework.nocode.generator.model.EntityModelDefinition;

import java.util.List;
import java.util.Set;

/**
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class MybatisPlusGeneratorMeta {

    private DbType dbType;

    private String dataSourceUserName;

    private String dataSourcePassword;

    private String dataSourceUrl;

    private String schemaName;

    private String outputDir;

    /** DDD 限界上下文包名，例如 {@code attachment}。 */
    private String context;

    private String packageName;


    private String[] tablePrefix;

    private String[] includeTables;

    private String[] excludeTables;

    private String apiOutputDir;

    private String serviceOutputDir;

    /** 唯一的业务关系定义来源；不读取 DDL 关联标记。 */
    private List<EntityRelationDefinition> relations = List.of();

    /** 实体领域归属与字段语义的唯一来源。 */
    private List<EntityModelDefinition> models = List.of();

    public MybatisPlusGeneratorMeta setModels(List<EntityModelDefinition> models) {
        this.models = List.copyOf(models);
        return this;
    }

    /** 明确启用子孙级联删除的层级实体，其余层级实体默认不级联。 */
    private Set<String> treeCascadeDeleteEntities = Set.of();

    public MybatisPlusGeneratorMeta setRelations(List<EntityRelationDefinition> relations) {
        this.relations = List.copyOf(relations);
        return this;
    }

    public MybatisPlusGeneratorMeta setTreeCascadeDeleteEntities(Set<String> entities) {
        this.treeCascadeDeleteEntities = Set.copyOf(entities);
        return this;
    }

    private boolean entityFileOverride = true;

    private boolean criteriaFileOverride = true;

    private boolean mapperFileOverride = false;

    private boolean mapperXmlFileOverride = false;

    private boolean repositoryFileOverride = false;

    private boolean serviceInterfaceFileOverride = false;

    private boolean localServiceFileOverride = false;

}
