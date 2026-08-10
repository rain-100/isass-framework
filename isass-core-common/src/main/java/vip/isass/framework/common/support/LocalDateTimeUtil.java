// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author Rain
 */
public class LocalDateTimeUtil {

    /**
     * 默认使用系统当前时区
     */
    public static final ZoneId ZONE = ZoneId.systemDefault();

    /**
     * 1970-01-01
     */
    public static LocalDate EPOCH_LOCAL_DATE = LocalDate.ofEpochDay(0);

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /*
     以下转换方法，按照以下时间的表示方式进行排序：
     string
     millisecond
     data
     localDate
     localTime
     localDateTime
     */

    // region 转成 milliseconds

    public static long epochMilliToDateEpochMilli(long epochMilli) {
        return localDateToEpochMilli(epochMilliToLocalDate(epochMilli));
    }

    public static long toMilliseconds(LocalDate localDate) {
        return toMilliseconds(localDate.atStartOfDay());
    }

    public static long localDateToEpochMilli(LocalDate localDate) {
        return localDateTimeToEpochMilli(localDate.atStartOfDay());
    }

    public static long toMilliseconds(LocalTime localTime) {
        return localDateTimeToEpochMilli(localTime.atDate(EPOCH_LOCAL_DATE));
    }

    public static Long localTimeToEpochMilli(LocalTime localTime) {
        return localDateTimeToEpochMilli(localTime.atDate(EPOCH_LOCAL_DATE));
    }

    public static long toMilliseconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    public static long localDateTimeToEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    // endregion

    // region 转成 Date

    public static Date localDateToDate(@NonNull LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZONE).toInstant());
    }

    public static Date nowDate() {
        return new Date(SystemClock.now());
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZONE).toInstant());
    }

    // endregion

    // region 转成 LocalDate

    public static LocalDate dateToLocalDate(@NonNull Date date) {
        return dateToLocalDateTime(date).toLocalDate();
    }

    public static LocalDate epochMilliToLocalDate(long epochMilli) {
        return epochMilliToLocalDateTime(epochMilli).toLocalDate();
    }

    public static LocalDate nowLocalDate() {
        return epochMilliToLocalDate(SystemClock.now());
    }


    // endregion

    // region 转成 LocalTime

    public static LocalTime nowLocalTime() {
        return epochMilliToLocalTime(SystemClock.now());
    }


    public static LocalTime dateToLocalTime(@NonNull Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZONE).toLocalTime();
    }

    public static LocalDateTime toLocalDateTime(@NonNull LocalDate localDate) {
        return localDate.atStartOfDay();
    }

    public static LocalTime epochMilliToLocalTime(long epochMilli) {
        return epochMilliToLocalDateTime(epochMilli).toLocalTime();
    }

    // endregion

    // region 转成 LocalDataTime

    public static LocalDateTime localDateToLocalDateTime(@NonNull LocalDate localDate) {
        return localDate.atStartOfDay();
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZONE);
    }

    public static LocalDateTime toLocalDateTime(LocalTime localTime) {
        return epochMilliToLocalDateTime(toMilliseconds(localTime));
    }

    public static LocalDateTime toLocalDateTime(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZONE);
    }

    public static LocalDateTime epochMilliToLocalDateTime(long milliseconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZONE);
    }

    public static LocalDateTime now() {
        return epochMilliToLocalDateTime(SystemClock.now());
    }

    public static LocalDateTime stringToLocalDateTime(String date) {
        if (StrUtil.isBlank(date)) {
            return null;
        }
        return dateToLocalDateTime(DateUtil.parse(date));
    }

    public static LocalDateTime localTimeToLocalDateTime(LocalTime localTime) {
        return epochMilliToLocalDateTime(localTimeToEpochMilli(localTime));
    }

    // endregion

}
