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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 以 Docker 容器运行 SUMO 的输出适配器。
 */
@Component
@ConditionalOnProperty(prefix = "green-traffic.sumo", name = "enabled", havingValue = "true")
public class DockerSimulationEngineAdapter implements SimulationEnginePort {

    private static final String SUMO_IMAGE = "ghcr.io/eclipse-sumo/sumo:latest";

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
                execute(root, "/sumo", "netgenerate", "--grid", "--grid.number=2", "--grid.length=300",
                        "--grid.attach-length=100", "--default-junction-type=traffic_light",
                        "--output-file=config/intersection.net.xml", "--no-turnarounds=true");
            }
            writeRouteFile(network, config.resolve("flow.rou.xml"), request.durationSeconds(), request.vehiclesPerHour());
            writeConfiguration(config.resolve("simulation.sumocfg"), request.durationSeconds());
            execute(root, "/sumo/config", "sumo", "-c", "simulation.sumocfg", "--no-warnings");
            return parseTripInfo(output.resolve("tripinfo.xml"));
        } catch (Exception exception) {
            throw new IllegalStateException("Docker SUMO simulation failed", exception);
        }
    }

    private void execute(Path root, String workingDirectory, String... command) throws IOException, InterruptedException {
        String dockerCmd = resolveDockerCommand();
        List<String> processCommand = new ArrayList<>(List.of(
                dockerCmd, "run", "--rm", "-v", root + ":/sumo", "-w", workingDirectory, SUMO_IMAGE));
        processCommand.addAll(List.of(command));
        Process process;
        try {
            process = new ProcessBuilder(processCommand).inheritIO().start();
        } catch (IOException io) {
            if (io.getMessage() != null && io.getMessage().contains("error=2")) {
                throw new IOException("Docker executable not found. Ensure Docker is installed, available in PATH, or set DOCKER_CMD to its absolute path.", io);
            }
            throw io;
        }
        if (process.waitFor() != 0) {
            throw new IllegalStateException("SUMO Docker command exited with " + process.exitValue());
        }
    }

    private String resolveDockerCommand() {
        String env = System.getenv("DOCKER_CMD");
        if (env != null && !env.isBlank()) {
            return env;
        }
        // Common Docker install locations on macOS/Linux
        String[] candidates = new String[]{"/usr/local/bin/docker", "/opt/homebrew/bin/docker", "/usr/bin/docker", "/bin/docker"};
        for (String candidate : candidates) {
            try {
                if (candidate != null && Files.exists(Path.of(candidate))) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }
        return "docker";
    }

    private void writeRouteFile(Path network, Path routeFile, int durationSeconds, int vehiclesPerHour) throws Exception {
        Document document = parse(network);
        NodeList edges = document.getElementsByTagName("edge");
        StringBuilder flows = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes>\n")
                .append("  <vType id=\"passenger\" accel=\"2.6\" decel=\"4.5\" sigma=\"0.5\" length=\"5\" minGap=\"2.5\" maxSpeed=\"13.89\"/>\n");
        int sequence = 0;
        for (int index = 0; index < edges.getLength(); index++) {
            Element edge = (Element) edges.item(index);
            String id = edge.getAttribute("id");
            if (!id.isBlank() && !id.startsWith(":")) {
                flows.append("  <flow id=\"flow_").append(sequence++).append("\" type=\"passenger\" begin=\"0\" end=\"")
                        .append(durationSeconds).append("\" from=\"").append(id)
                        .append("\" departLane=\"0\" departSpeed=\"max\" vehsPerHour=\"")
                        .append(vehiclesPerHour).append("\"/>\n");
            }
        }
        flows.append("</routes>\n");
        Files.writeString(routeFile, flows.toString());
    }

    private void writeConfiguration(Path configFile, int durationSeconds) throws IOException {
        Files.writeString(configFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                  <input><net-file value="intersection.net.xml"/><route-files value="flow.rou.xml"/></input>
                  <time><begin value="0"/><end value="%d"/><step-length value="0.1"/></time>
                  <output><tripinfo-output value="../output/tripinfo.xml"/></output>
                </configuration>
                """.formatted(durationSeconds));
    }

    private List<SumoTripInfo> parseTripInfo(Path tripInfoFile) throws Exception {
        Document document = parse(tripInfoFile);
        NodeList trips = document.getElementsByTagName("tripinfo");
        List<SumoTripInfo> result = new ArrayList<>();
        for (int index = 0; index < trips.getLength(); index++) {
            Element trip = (Element) trips.item(index);
            result.add(new SumoTripInfo(
                    trip.getAttribute("id"), trip.getAttribute("vType"),
                    doubleAttribute(trip, "duration"), doubleAttribute(trip, "waitingTime"),
                    doubleAttribute(trip, "timeLoss"), doubleAttribute(trip, "routeLength")));
        }
        return result;
    }

    private Document parse(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private double doubleAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value.isBlank() ? 0 : Double.parseDouble(value);
    }
}