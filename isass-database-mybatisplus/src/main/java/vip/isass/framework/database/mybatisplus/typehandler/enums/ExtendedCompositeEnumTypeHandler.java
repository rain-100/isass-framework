// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler.enums;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;

import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩展mybatis-plus的枚举映射处理器，新增枚举字段为code的映射
 * 如果枚举类含有code字段，则使用 CodeFieldMybatisEnumTypeHandler 否则使用 EnumTypeHandler
 * <p>
 * <p>
 * <a href="https://baomidou.com/reference/#defaultenumtypehandler">defaultenumtypehandler官网描述</a>
 * <p>
 * MyBatis-Plus 支持多种枚举处理方式，包括存储枚举名称、索引或自定义处理。从 3.5.2 开始，默认枚举处理器为 CompositeEnumTypeHandler，它会根据枚举是否为通用枚举来选择合适的处理方式。
 * <p>
 * org.apache.ibatis.type.EnumTypeHandler : 存储枚举的名称
 * org.apache.ibatis.type.EnumOrdinalTypeHandler : 存储枚举的索引
 * com.baomidou.mybatisplus.extension.handlers.MybatisEnumTypeHandler : 枚举类需要实现 IEnum 接口或字段标记@EnumValue 注解.(3.1.2 以下版本为 EnumTypeHandler)
 * com.baomidou.mybatisplus.extension.handlers.EnumAnnotationTypeHandler: 枚举类字段需要标记@EnumValue 注解
 * 从 3.5.2 开始，默认枚举处理器为 CompositeEnumTypeHandler，会对定义为 MyBatis-Plus 通用枚举的枚举(实现IEnum了或加了EnumValue注解) 在内部使用MybatisEnumTypeHandler处理枚举。
 * <p>
 * 其他的枚举使用内部属性 defaultEnumTypeHandler(默认为org.apache.ibatis.type.EnumTypeHandler)进行处理。
 * <p>
 * 此配置仅改变 CompositeEnumTypeHandler#defaultEnumTypeHandler的值
 */
@Slf4j
public class ExtendedCompositeEnumTypeHandler<E extends Enum<E>> implements TypeHandler<E> {

    private static final Map<Class<?>, Boolean> MP_ENUM_CACHE = new ConcurrentHashMap<>();
    private static Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;
    private final TypeHandler<E> delegate;

    public ExtendedCompositeEnumTypeHandler(Class<E> enumClassType) {
        if (enumClassType == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        if (CollectionUtils.computeIfAbsent(MP_ENUM_CACHE, enumClassType, CodeFieldMybatisEnumTypeHandler::isCodeEnums)) {
            delegate = new CodeFieldMybatisEnumTypeHandler<>(enumClassType);
        } else {
            delegate = getInstance(enumClassType, defaultEnumTypeHandler);
        }
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        delegate.setParameter(ps, i, parameter, jdbcType);
    }

    @Override
    public E getResult(ResultSet rs, String columnName) throws SQLException {
        return delegate.getResult(rs, columnName);
    }

    @Override
    public E getResult(ResultSet rs, int columnIndex) throws SQLException {
        return delegate.getResult(rs, columnIndex);
    }

    @Override
    public E getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return delegate.getResult(cs, columnIndex);
    }

    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
        if (javaTypeClass != null) {
            try {
                Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
                return (TypeHandler<T>) c.newInstance(javaTypeClass);
            } catch (NoSuchMethodException ignored) {
                // ignored
            } catch (Exception e) {
                throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
            }
        }
        try {
            Constructor<?> c = typeHandlerClass.getConstructor();
            return (TypeHandler<T>) c.newInstance();
        } catch (Exception e) {
            throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
        }
    }
}
