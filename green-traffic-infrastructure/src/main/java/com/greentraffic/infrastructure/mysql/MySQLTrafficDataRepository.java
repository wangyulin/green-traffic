package com.greentraffic.infrastructure.mysql;

import com.greentraffic.core.port.outbound.TrafficDataRepository;
import com.greentraffic.model.entity.traffic.TrafficMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
    public boolean save(TrafficMetric data) {
        String sql = "INSERT INTO traffic_data (road_id, vehicle_type, traffic_flow, " +
                "average_speed, co2_emission, location, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    data.roadId(),
                    data.vehicleType(),
                    data.trafficFlow(),
                    data.averageSpeed(),
                    data.co2Emission(),
                    data.location(),
                    data.timestamp()
            );
            return true;
        } catch (Exception e) {
            log.error("MySQL 保存失败", e);
            return false;
        }
    }

    @Override
    public boolean saveBatch(List<TrafficMetric> dataList) {
        // 批量插入实现
        return false;
    }

    @Override
        public List<TrafficMetric> findByRoadId(String roadId,
                             Instant startTime,
                             Instant endTime) {
        String sql = "SELECT * FROM traffic_data WHERE road_id = ? " +
                "AND timestamp BETWEEN ? AND ?";
        return jdbcTemplate.query(sql,
            (rs, rowNum) -> new TrafficMetric(
                rs.getString("road_id"),
                null,
                rs.getString("vehicle_type"),
                rs.getInt("traffic_flow"),
                rs.getDouble("average_speed"),
                rs.getDouble("co2_emission"),
                rs.getString("location"),
                rs.getObject("timestamp", Instant.class)
            ),
            roadId, startTime, endTime
        );
    }

    @Override
        public Double findAverageCo2Emission(String roadId,
                         Instant startTime,
                         Instant endTime) {
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
    public void cleanOldData(Instant beforeTime) {
        String sql = "DELETE FROM traffic_data WHERE timestamp < ?";
        jdbcTemplate.update(sql, beforeTime);
    }
}