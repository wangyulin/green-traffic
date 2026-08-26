package com.greentraffic.simulator.sumo;

import com.greentraffic.core.port.output.simulation.SumoTripInfo;
import com.greentraffic.core.domain.traffic.SimulationTrafficMetric;

import java.time.Instant;
import java.util.List;

final class SumoTrafficMetricMapper {

    private static final double CO2_KG_PER_KM = 0.192;

    private SumoTrafficMetricMapper() {
    }

    static SimulationTrafficMetric map(String simulationId, List<SumoTripInfo> trips, Instant timestamp) {
        if (trips.isEmpty()) {
            return null;
        }
        double totalRouteLength = trips.stream().mapToDouble(SumoTripInfo::routeLengthMeters).sum();
        double averageDuration = trips.stream().mapToDouble(SumoTripInfo::durationSeconds).average().orElse(0);
        double averageWaiting = trips.stream().mapToDouble(SumoTripInfo::waitingTimeSeconds).average().orElse(0);
        double averageTimeLoss = trips.stream().mapToDouble(SumoTripInfo::timeLossSeconds).average().orElse(0);
        double averageSpeed = trips.stream()
                .filter(trip -> trip.durationSeconds() > 0)
                .mapToDouble(trip -> trip.routeLengthMeters() / trip.durationSeconds() * 3.6)
                .average()
                .orElse(0);
        String vehicleType = trips.stream().map(SumoTripInfo::vehicleType).distinct().count() == 1
            ? trips.get(0).vehicleType() : "mixed";

        return new SimulationTrafficMetric(
                simulationId, "SUMO-GRID", "UNKNOWN", vehicleType, trips.size(), averageSpeed,
                totalRouteLength / 1000 * CO2_KG_PER_KM, averageDuration, averageWaiting,
                averageTimeLoss, totalRouteLength, timestamp);
    }
}