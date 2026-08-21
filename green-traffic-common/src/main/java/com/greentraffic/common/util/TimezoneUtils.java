package com.greentraffic.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 时区工具类，封装系统时区获取、Instant/LocalDateTime/OffsetDateTime 互转
 */
public final class TimezoneUtils {

    private TimezoneUtils() {}

    /** 返回系统默认时区 */
    public static ZoneId getSystemZone() {
        return ZoneId.systemDefault();
    }

    /**
     * 将一个 Instant 转为带本地时区偏移量的 OffsetDateTime 字符串（RFC3339），用于 Flux 查询。
     * 如果 instant 为 null，返回 null。
     */
    public static String formatForFlux(Instant instant) {
        if (instant == null) {
            return "now()";
        }
        OffsetDateTime odt = instant.atZone(getSystemZone()).toOffsetDateTime();
        return odt.toString();
    }

    /**
     * 将本地时间转换为 Instant（使用系统默认时区）。
     */
    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(getSystemZone()).toInstant();
    }

    /**
     * 规范化 Instant（当前实现为透明传递，留作未来扩展）。
     */
    public static Instant normalizeInstant(Instant instant) {
        return instant == null ? null : instant;
    }
}
