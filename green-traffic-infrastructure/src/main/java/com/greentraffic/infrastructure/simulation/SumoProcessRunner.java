package com.greentraffic.infrastructure.simulation;

import com.greentraffic.core.port.output.simulation.SimulationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jakarta.annotation.PreDestroy;

@Component
public class SumoProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(SumoProcessRunner.class);

    private final String sumoImage;

    private final ConcurrentMap<String, Process> running = new ConcurrentHashMap<>();

    public SumoProcessRunner(@org.springframework.beans.factory.annotation.Value("${green-traffic.sumo.image:ghcr.io/eclipse-sumo/sumo:v1_27_1}") String sumoImage) {
        this.sumoImage = Objects.requireNonNull(sumoImage, "sumoImage must not be null");
    }

    @PreDestroy
    public void shutdown() {
        for (String id : new ArrayList<>(running.keySet())) {
            try {
                stop(id);
            } catch (Exception e) {
                log.warn("Failed to stop SUMO container for {} during shutdown: {}", id, e.getMessage());
            }
        }
    }

    public void execute(Path root, String simulationId, String workingDirectory, String... command) throws IOException, InterruptedException {
        String dockerCmd = resolveDockerCommand();
        String containerName = "greentraffic-sumo-" + simulationId;
        List<String> processCommand = new ArrayList<>(List.of(
            dockerCmd, "run", "--rm", "--name", containerName, "-v", root + ":/sumo", "-w", workingDirectory, sumoImage));
        processCommand.addAll(List.of(command));
        Process process;
        try {
            process = new ProcessBuilder(processCommand).inheritIO().start();
            running.put(simulationId, process);
        } catch (IOException io) {
            if (io.getMessage() != null && io.getMessage().contains("error=2")) {
                throw new IOException("Docker executable not found. Ensure Docker is installed, available in PATH, or set DOCKER_CMD to its absolute path.", io);
            }
            throw io;
        }
        try {
            if (process.waitFor() != 0) {
                throw new IllegalStateException("SUMO Docker command exited with " + process.exitValue());
            }
        } finally {
            running.remove(simulationId);
        }
    }

    public void stop(String simulationId) {
        Process p = running.remove(simulationId);
        if (p != null) {
            try {
                if (p.isAlive()) {
                    p.destroyForcibly();
                    try {
                        p.waitFor();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Ensure docker container is removed. Use the same container name convention as in execute().
        String containerName = "greentraffic-sumo-" + simulationId;
        String dockerCmd = resolveDockerCommand();
        List<String> rmCmd = List.of(dockerCmd, "rm", "-f", containerName);
        try {
            Process rm = new ProcessBuilder(rmCmd).inheritIO().start();
            try {
                rm.waitFor();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException ioe) {
            log.warn("Failed to run docker rm for container {}: {}", containerName, ioe.getMessage());
        }
    }

    public SimulationStatus status(String simulationId) {
        Process p = running.get(simulationId);
        if (p == null) return SimulationStatus.UNKNOWN;
        return p.isAlive() ? SimulationStatus.RUNNING : SimulationStatus.COMPLETED;
    }

    private String resolveDockerCommand() {
        String env = System.getenv("DOCKER_CMD");
        if (env != null && !env.isBlank()) {
            return env;
        }
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
}
