package com.greentraffic.core.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 告警消息（Core 版）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage implements Serializable {

    private String alertId;
    private AlertType alertType;
    private AlertLevel alertLevel;
    private String title;
    private String content;
    private String roadId;
    private Double co2Emission;
    private Double averageSpeed;
    private Integer trafficFlow;
    private Instant alertTime;
    private boolean processed;
    private Instant processedTime;
    private String processedBy;

    public enum AlertType {
        CO2_EMISSION_HIGH("碳排放超标"),
        TRAFFIC_CONGESTION("交通拥堵"),
        SPEED_ABNORMAL("速度异常"),
        SYSTEM_ERROR("系统错误"),
        DATA_ABNORMAL("数据异常");

        private final String description;

        AlertType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AlertLevel {
        INFO("信息"),
        WARNING("警告"),
        CRITICAL("严重"),
        EMERGENCY("紧急");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Factories that accept id/time from composition root (preferred for testability)
    public static AlertMessage createCo2Alert(String alertId, String roadId, Double co2Emission, Instant alertTime) {
        return AlertMessage.builder()
                .alertId(alertId)
                .alertType(AlertType.CO2_EMISSION_HIGH)
                .alertLevel(co2Emission != null && co2Emission > 100 ? AlertLevel.CRITICAL : AlertLevel.WARNING)
                .title("碳排放超标告警")
                .content(String.format("道路 %s 碳排放超标，当前值: %.2f", roadId, co2Emission))
                .roadId(roadId)
                .co2Emission(co2Emission)
                .alertTime(alertTime)
                .processed(false)
                .build();
    }

    public static AlertMessage createCongestionAlert(String alertId, String roadId, Double averageSpeed, Instant alertTime) {
        return AlertMessage.builder()
                .alertId(alertId)
                .alertType(AlertType.TRAFFIC_CONGESTION)
                .alertLevel(averageSpeed != null && averageSpeed < 10 ? AlertLevel.CRITICAL : AlertLevel.WARNING)
                .title("交通拥堵告警")
                .content(String.format("道路 %s 交通拥堵，平均速度: %.2f km/h", roadId, averageSpeed))
                .roadId(roadId)
                .averageSpeed(averageSpeed)
                .alertTime(alertTime)
                .processed(false)
                .build();
    }
}
