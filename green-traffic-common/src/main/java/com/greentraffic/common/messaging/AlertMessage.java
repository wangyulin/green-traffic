package com.greentraffic.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警消息
 * 用于系统告警通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessage implements Serializable {

    /**
     * 告警ID
     */
    private String alertId;

    /**
     * 告警类型
     */
    private AlertType alertType;

    /**
     * 告警级别
     */
    private AlertLevel alertLevel;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String content;

    /**
     * 相关道路ID
     */
    private String roadId;

    /**
     * 相关数据
     */
    private Double co2Emission;
    private Double averageSpeed;
    private Integer trafficFlow;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 是否已处理
     */
    private boolean processed;

    /**
     * 处理时间
     */
    private LocalDateTime processedTime;

    /**
     * 处理人
     */
    private String processedBy;

    /**
     * 告警类型枚举
     */
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

    /**
     * 告警级别枚举
     */
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

    /**
     * 创建碳排放超标告警
     */
    public static AlertMessage createCo2Alert(String roadId, Double co2Emission) {
        return AlertMessage.builder()
                .alertId(java.util.UUID.randomUUID().toString())
                .alertType(AlertType.CO2_EMISSION_HIGH)
                .alertLevel(co2Emission > 100 ? AlertLevel.CRITICAL : AlertLevel.WARNING)
                .title("碳排放超标告警")
                .content(String.format("道路 %s 碳排放超标，当前值: %.2f", roadId, co2Emission))
                .roadId(roadId)
                .co2Emission(co2Emission)
                .alertTime(LocalDateTime.now())
                .processed(false)
                .build();
    }

    /**
     * 创建交通拥堵告警
     */
    public static AlertMessage createCongestionAlert(String roadId, Double averageSpeed) {
        return AlertMessage.builder()
                .alertId(java.util.UUID.randomUUID().toString())
                .alertType(AlertType.TRAFFIC_CONGESTION)
                .alertLevel(averageSpeed < 10 ? AlertLevel.CRITICAL : AlertLevel.WARNING)
                .title("交通拥堵告警")
                .content(String.format("道路 %s 交通拥堵，平均速度: %.2f km/h", roadId, averageSpeed))
                .roadId(roadId)
                .averageSpeed(averageSpeed)
                .alertTime(LocalDateTime.now())
                .processed(false)
                .build();
    }
}
