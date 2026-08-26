package com.greentraffic.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 时区工具类，封装系统时区获取、Instant/LocalDateTime/OffsetDateTime 互转
 */
public final class TimezoneUtils {

    private TimezoneUtils() {}

    /** 返回系统默认时区 */
    public static ZoneId getSystemZone() {
        // 优先使用环境变量 GREEN_TRAFFIC_TIMEZONE，其次使用 JVM 属性 user.timezone，最后使用系统默认时区
        String env = System.getenv("GREEN_TRAFFIC_TIMEZONE");
        if (env != null && !env.isBlank()) {
            try {
                return ZoneId.of(env.trim());
            } catch (Exception ignored) {
            }
        }

        String prop = System.getProperty("user.timezone");
        if (prop != null && !prop.isBlank()) {
            try {
                return ZoneId.of(prop.trim());
            } catch (Exception ignored) {
            }
        }

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
     * 规范化 Instant。Instant 表示绝对时间点，不应施加本地时区偏移。
     */
    public static Instant normalizeInstant(Instant instant) {
        return instant;
    }
}
