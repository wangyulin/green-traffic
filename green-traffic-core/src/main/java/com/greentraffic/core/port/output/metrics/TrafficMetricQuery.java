package com.greentraffic.core.port.output.metrics;

import java.time.Instant;
import java.util.Objects;

/**
 * 交通指标查询条件。
 *
 * <p>这是 Core 与指标存储 Adapter 之间的业务查询契约，
 * 用于替代 Map<String, String> 形式的泛化查询参数。</p>
 */
public record TrafficMetricQuery(
        Instant from,
        Instant to
) {

    public TrafficMetricQuery {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}