package com.greentraffic.infrastructure.mysql;

import com.greentraffic.common.messaging.TrafficDataMessage;
import com.greentraffic.common.repository.TrafficDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MySQL 交通数据存储实现（备选方案）
 * 用于演示如何替换存储实现
 */
@Slf4j
@Repository
@Profile("mysql")
@RequiredArgsConstructor
public class MySQLTrafficDataRepository implements TrafficDataRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean save(TrafficDataMessage data) {
        String sql = "INSERT INTO traffic_data (road_id, vehicle_type, traffic_flow, " +
                "average_speed, co2_emission, location, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    data.getRoadId(),
                    data.getVehicleType(),
                    data.getTrafficFlow(),
                    data.getAverageSpeed(),
                    data.getCo2Emission(),
                    data.getLocation(),
                    data.getTimestamp()
            );
            return true;
        } catch (Exception e) {
            log.error("MySQL 保存失败", e);
            return false;
        }
    }

    @Override
    public boolean saveBatch(List<TrafficDataMessage> dataList) {
        // 批量插入实现
        return false;
    }

    @Override
    public List<TrafficDataMessage> findByRoadId(String roadId,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime) {
        String sql = "SELECT * FROM traffic_data WHERE road_id = ? " +
                "AND timestamp BETWEEN ? AND ?";

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> TrafficDataMessage.builder()
                        .roadId(rs.getString("road_id"))
                        .vehicleType(rs.getString("vehicle_type"))
                        .trafficFlow(rs.getInt("traffic_flow"))
                        .averageSpeed(rs.getDouble("average_speed"))
                        .co2Emission(rs.getDouble("co2_emission"))
                        .location(rs.getString("location"))
                        .timestamp(rs.getObject("timestamp", LocalDateTime.class))
                        .build(),
                roadId, startTime, endTime
        );
    }

    @Override
    public Double findAverageCo2Emission(String roadId,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime) {
        String sql = "SELECT AVG(co2_emission) FROM traffic_data " +
                "WHERE road_id = ? AND timestamp BETWEEN ? AND ?";

        return jdbcTemplate.queryForObject(sql, Double.class,
                roadId, startTime, endTime);
    }

    @Override
    public boolean isAvailable() {
        try {
            jdbcTemplate.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void cleanOldData(LocalDateTime beforeTime) {
        String sql = "DELETE FROM traffic_data WHERE timestamp < ?";
        jdbcTemplate.update(sql, beforeTime);
    }
}