// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import cn.hutool.core.util.RandomUtil;
import vip.isass.framework.common.support.LocalDateTimeUtil;
import vip.isass.framework.common.support.SystemClock;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

/**
 * @author Rain
 */
public interface IEntity<E extends IEntity<E>> extends Serializable, IAnyJsonEntity {

    long serialVersionUID = 1L;

    /**
     * 数据库表名。非空时直接使用，为空时由框架按规则拼接。
     * 例如不规则命名：{@code override tableName() { return "t_app_icon"; }}
     */
    default String tableName() {
        return "";
    }

    /**
     * Generated non-persistent associations. Empty by default so ordinary entities
     * do not pay any relationship-loading cost.
     */
    default List<EntityAssociation> associations() {
        return List.of();
    }

    default String randomString() {
        return RandomUtil.randomString(6);
    }

    default Byte randomByte() {
        return (byte) RandomUtil.randomInt(Byte.MAX_VALUE);
    }

    default Boolean randomBoolean() {
        return RandomUtil.randomBoolean();
    }

    default Integer randomInteger() {
        return RandomUtil.randomInt();
    }

    default Long randomLong() {
        return RandomUtil.randomLong();
    }

    default Float randomFloat() {
        return ThreadLocalRandom.current().nextFloat();
    }

    default Double randomDouble() {
        return RandomUtil.randomDouble();
    }

    default BigDecimal randomBigDecimal() {
        return RandomUtil.randomBigDecimal(BigDecimal.TEN);
    }

    default LocalDateTime randomLocalDateTime() {
        return LocalDateTimeUtil.now();
    }

    default LocalDate randomLocalDate() {
        return LocalDateTimeUtil.nowLocalDate();
    }

    default LocalTime randomLocalTime() {
        return LocalDateTimeUtil.nowLocalTime();
    }

    default Long randomLongTimestamp() {
        return SystemClock.now() - randomInteger();
    }

    /**
     * 生成随机的entity
     * 所有字段都随机赋值
     */
    E randomEntity();

}
