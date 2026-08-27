package com.greentraffic.simulator.sumo;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "green-traffic.sumo")
public class SumoSimulatorProperties {

    private boolean enabled;
    private Path workingDirectory = Path.of("sumo-work");
    private int durationSeconds = 300;
    private int vehiclesPerHour = 300;
    private boolean asyncRun = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getVehiclesPerHour() {
        return vehiclesPerHour;
    }

    public void setVehiclesPerHour(int vehiclesPerHour) {
        this.vehiclesPerHour = vehiclesPerHour;
    }

    public boolean isAsyncRun() {
        return asyncRun;
    }

    public void setAsyncRun(boolean asyncRun) {
        this.asyncRun = asyncRun;
    }
}