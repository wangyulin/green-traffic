package com.greentraffic.infrastructure.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SumoFileManager {

    private static final Logger log = LoggerFactory.getLogger(SumoFileManager.class);

    private final SumoTripInfoParser parser;

    public SumoFileManager(SumoTripInfoParser parser) {
        this.parser = parser;
    }

    public void writeRouteFile(Path network, Path routeFile, int durationSeconds, int vehiclesPerHour) throws Exception {
        StringBuilder flows = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes>\n")
                .append("  <vType id=\"passenger\" accel=\"2.6\" decel=\"4.5\" sigma=\"0.5\" length=\"5\" minGap=\"2.5\" maxSpeed=\"13.89\"/>\n");

        try {
            Document document = parser.parseDocument(network);
            NodeList edges = document.getElementsByTagName("edge");
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
        } catch (Exception ex) {
            log.warn("[SUMO Adapter] failed to parse network file {}; attempting permissive edge extraction. Error: {}", network, ex.getMessage());
            try {
                String raw = Files.readString(network);
                Pattern p = Pattern.compile("<edge[^>]*\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE);
                Matcher mm = p.matcher(raw);
                int seq = 0;
                while (mm.find()) {
                    String id = mm.group(1);
                    if (id == null || id.isBlank() || id.startsWith(":")) continue;
                        flows.append("  <flow id=\"flow_").append(seq++).append("\" type=\"passenger\" begin=\"0\" end=\"")
                            .append(durationSeconds).append("\" from=\"").append(id)
                            .append("\" departLane=\"0\" departSpeed=\"max\" vehsPerHour=\"")
                            .append(vehiclesPerHour).append("\"/>\n");
                }
                if (seq == 0) {
                    log.warn("[SUMO Adapter] permissive extraction found no edges; writing minimal routes file.");
                }
            } catch (Exception ex2) {
                log.warn("[SUMO Adapter] permissive edge extraction failed: {}", ex2.getMessage());
            }
        }

        flows.append("</routes>\n");
        Files.writeString(routeFile, flows.toString());
    }

    public void writeConfiguration(Path configFile, int durationSeconds) throws Exception {
        Files.writeString(configFile, """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                  <input><net-file value="intersection.net.xml"/><route-files value="flow.rou.xml"/></input>
                  <time><begin value="0"/><end value="%d"/><step-length value="0.1"/></time>
                  <output><tripinfo-output value="../output/tripinfo.xml"/></output>
                </configuration>
                """.formatted(durationSeconds));
    }
}
