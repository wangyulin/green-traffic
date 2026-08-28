package com.greentraffic.infrastructure.simulation;

import com.greentraffic.core.port.output.simulation.SimulationEnginePort;
import com.greentraffic.core.port.output.simulation.SumoSimulationRequest;
import com.greentraffic.core.port.output.simulation.SumoTripInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 以 Docker 容器运行 SUMO 的输出适配器。
 */
@Component
@ConditionalOnProperty(prefix = "green-traffic.sumo", name = "enabled", havingValue = "true")
public class DockerSimulationEngineAdapter implements SimulationEnginePort {

    private static final Logger log = LoggerFactory.getLogger(DockerSimulationEngineAdapter.class);

    // SUMO 镜像版本由 SumoProcessRunner 注入的配置项提供，避免在多个位置硬编码

    private final SumoProcessRunner processRunner;
    private final SumoFileManager fileManager;
    private final SumoTripInfoParser tripInfoParser;

    public DockerSimulationEngineAdapter(SumoProcessRunner processRunner, SumoFileManager fileManager, SumoTripInfoParser tripInfoParser) {
        this.processRunner = processRunner;
        this.fileManager = fileManager;
        this.tripInfoParser = tripInfoParser;
    }

    @Override
    public List<SumoTripInfo> run(SumoSimulationRequest request) {
        try {
            Path root = request.workingDirectory().toAbsolutePath();
            Path config = root.resolve("config");
            Path output = root.resolve("output");
            Files.createDirectories(config);
            Files.createDirectories(output);
            Path network = config.resolve("intersection.net.xml");
            if (Files.notExists(network)) {
                processRunner.execute(root, request.simulationId(), "/sumo", "netgenerate", "--grid", "--grid.number=2", "--grid.length=300",
                        "--grid.attach-length=100", "--default-junction-type=traffic_light",
                        "--output-file=config/intersection.net.xml", "--no-turnarounds=true");
            }
            fileManager.writeRouteFile(network, config.resolve("flow.rou.xml"), request.durationSeconds(), request.vehiclesPerHour());
            fileManager.writeConfiguration(config.resolve("simulation.sumocfg"), request.durationSeconds());
            processRunner.execute(root, request.simulationId(), "/sumo/config", "sumo", "-c", "simulation.sumocfg", "--no-warnings");
            return tripInfoParser.parseTripInfo(output.resolve("tripinfo.xml"));
        } catch (Exception exception) {
            throw new IllegalStateException("Docker SUMO simulation failed", exception);
        }
    }
    @Override
    public void stop(String simulationId) {
        processRunner.stop(simulationId);
    }

    @Override
    public com.greentraffic.core.port.output.simulation.SimulationStatus status(String simulationId) {
        return processRunner.status(simulationId);
    }
}