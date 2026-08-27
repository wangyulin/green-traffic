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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 以 Docker 容器运行 SUMO 的输出适配器。
 */
@Component
@ConditionalOnProperty(prefix = "green-traffic.sumo", name = "enabled", havingValue = "true")
public class DockerSimulationEngineAdapter implements SimulationEnginePort {

    private static final Logger log = LoggerFactory.getLogger(DockerSimulationEngineAdapter.class);

    private static final String SUMO_IMAGE = "ghcr.io/eclipse-sumo/sumo:latest";

    private final ConcurrentMap<String, Process> running = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong recoveredTripinfoElements = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong nullSanitizationCount = new java.util.concurrent.atomic.AtomicLong(0);

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
                execute(root, request.simulationId(), "/sumo", "netgenerate", "--grid", "--grid.number=2", "--grid.length=300",
                        "--grid.attach-length=100", "--default-junction-type=traffic_light",
                        "--output-file=config/intersection.net.xml", "--no-turnarounds=true");
            }
            writeRouteFile(network, config.resolve("flow.rou.xml"), request.durationSeconds(), request.vehiclesPerHour());
            writeConfiguration(config.resolve("simulation.sumocfg"), request.durationSeconds());
            execute(root, request.simulationId(), "/sumo/config", "sumo", "-c", "simulation.sumocfg", "--no-warnings");
            return parseTripInfo(output.resolve("tripinfo.xml"));
        } catch (Exception exception) {
            throw new IllegalStateException("Docker SUMO simulation failed", exception);
        }
    }

    private void execute(Path root, String simulationId, String workingDirectory, String... command) throws IOException, InterruptedException {
        String dockerCmd = resolveDockerCommand();
        List<String> processCommand = new ArrayList<>(List.of(
                dockerCmd, "run", "--rm", "-v", root + ":/sumo", "-w", workingDirectory, SUMO_IMAGE));
        processCommand.addAll(List.of(command));
        Process process;
        try {
            process = new ProcessBuilder(processCommand).inheritIO().start();
            // record running process so it can be stopped/queried
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
            // remove from running map after completion
            running.remove(simulationId);
        }
    }

    @Override
    public void stop(String simulationId) {
        Process p = running.remove(simulationId);
        if (p != null) {
            p.destroyForcibly();
        }
    }

    @Override
    public com.greentraffic.core.port.output.simulation.SimulationStatus status(String simulationId) {
        Process p = running.get(simulationId);
        if (p == null) return com.greentraffic.core.port.output.simulation.SimulationStatus.UNKNOWN;
        return p.isAlive() ? com.greentraffic.core.port.output.simulation.SimulationStatus.RUNNING : com.greentraffic.core.port.output.simulation.SimulationStatus.COMPLETED;
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
        StringBuilder flows = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes>\n")
                .append("  <vType id=\"passenger\" accel=\"2.6\" decel=\"4.5\" sigma=\"0.5\" length=\"5\" minGap=\"2.5\" maxSpeed=\"13.89\"/>\n");

        try {
            Document document = parse(network);
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
                // permissive regex: find <edge ... "ID" ...> and extract first quoted token as id
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
        final int maxAttempts = 5;
        final long retryDelayMs = 200L;
        String content = "";

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] raw = Files.readAllBytes(file);
            content = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
            if (content.indexOf('\0') >= 0) {
                // sanitize null characters which cause SAX parse errors
                nullSanitizationCount.incrementAndGet();
                if (attempt == 1) {
                    log.warn("[SUMO Adapter] tripinfo.xml contains invalid NULL characters; sanitizing before parsing: {} (attempt {})", file, attempt);
                } else {
                    log.debug("[SUMO Adapter] tripinfo.xml contains NULL chars; sanitizing (attempt {}) for file {}", attempt, file);
                }
            }

            // remove other C0 control chars except TAB(0x09), LF(0x0A), CR(0x0D)
            content = removeControlChars(content);

            // escape problematic characters inside attribute values: '<' and unescaped '&'
            content = escapeAttributeUnsafeChars(content);

            // trim any leading garbage before the first '<' to ensure XML declaration (<?xml ...) is at document start
            // Prefer to locate an XML declaration; if present keep from its first occurrence,
            // otherwise fall back to trimming before the first '<'. This prevents '<?xml' appearing
            // in the middle of the document which triggers SAXParseException.
            int idxXmlDecl = indexOfIgnoreCase(content, "<?xml");
            if (idxXmlDecl >= 0) {
                if (idxXmlDecl > 0) {
                    if (attempt == 1) log.warn("[SUMO Adapter] trimming leading non-XML content before first '<?xml' in file: {} (attempt {})", file, attempt);
                    else log.debug("[SUMO Adapter] trimming leading non-XML content before '<?xml' (attempt {}) for file {}", attempt, file);
                    content = content.substring(idxXmlDecl);
                }
            } else {
                int firstLt = content.indexOf('<');
                if (firstLt > 0) {
                    if (attempt == 1) log.warn("[SUMO Adapter] trimming leading non-XML content before first '<' in file: {} (attempt {})", file, attempt);
                    else log.debug("[SUMO Adapter] trimming leading non-XML content before '<' (attempt {}) for file {}", attempt, file);
                    content = content.substring(firstLt);
                }
            }

            // Replace any additional XML declarations appearing after the first with an escaped form to avoid parser errors
            content = sanitizeXmlDeclarations(content);

            if (!content.trim().isEmpty()) {
                break;
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                log.debug("[SUMO Adapter] tripinfo.xml empty; retrying read (attempt {}/{})", attempt + 1, maxAttempts);
            }
        }

        if (content.trim().isEmpty()) {
            throw new IOException("tripinfo.xml is empty or truncated after retries: " + file);
        }

        // remove other control characters that are illegal in XML (except tab/newline/carriage)
        content = removeControlChars(content);

        // sanitize attribute values: escape '<', '>', and '&' (but avoid double-escaping existing entities)
        content = sanitizeAttributeValues(content);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        // install an ErrorHandler to prevent the parser from printing fatal errors to stderr
        builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
            @Override
            public void warning(org.xml.sax.SAXParseException exception) {
                log.debug("[SUMO Adapter] XML parser warning: {}", exception.getMessage());
            }

            @Override
            public void error(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                throw exception;
            }

            @Override
            public void fatalError(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                throw exception;
            }
        });

        // Try element-level recovery preemptively if we can extract <tripinfo> elements.
        String preRecovered = recoverTripinfoElements(content);
        if (preRecovered != null) {
            try (InputStream is = new ByteArrayInputStream(preRecovered.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                java.io.PrintStream oldErr = System.err;
                try (java.io.PrintStream ps = new java.io.PrintStream(java.io.OutputStream.nullOutputStream())) {
                    System.setErr(ps);
                    return builder.parse(is);
                } finally {
                    System.setErr(oldErr);
                }
            } catch (org.xml.sax.SAXParseException preEx) {
                log.info("[SUMO Adapter] preemptive recovered-parse failed: {}. Falling back to full parse.", preEx.getMessage());
                dumpContentSample(preRecovered, file);
            }
        }

        try (InputStream is = new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            java.io.PrintStream oldErr = System.err;
            try (java.io.PrintStream ps = new java.io.PrintStream(java.io.OutputStream.nullOutputStream())) {
                System.setErr(ps);
                return builder.parse(is);
            } finally {
                System.setErr(oldErr);
            }
        } catch (org.xml.sax.SAXParseException saxEx) {
            log.info("[SUMO Adapter] parse failed after sanitization: {}. Trying element-level recovery.", saxEx.getMessage());
            dumpContentSample(content, file);
            String recovered = recoverTripinfoElements(content);
            if (recovered == null) {
                throw saxEx;
            }
            try (InputStream is2 = new ByteArrayInputStream(recovered.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                java.io.PrintStream oldErr = System.err;
                try (java.io.PrintStream ps = new java.io.PrintStream(java.io.OutputStream.nullOutputStream())) {
                    System.setErr(ps);
                    return builder.parse(is2);
                } finally {
                    System.setErr(oldErr);
                }
            }
        }
    }

    private void dumpContentSample(String content, Path sourceFile) {
        try {
            String sample = content.length() <= 2048 ? content : content.substring(0, 2048);
            StringBuilder safeSb = new StringBuilder(sample.length());
            for (int i = 0; i < sample.length(); i++) {
                char c = sample.charAt(i);
                if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) safeSb.append(c);
                else safeSb.append('?');
            }
            String safe = safeSb.toString();
            log.warn("[SUMO Adapter] tripinfo sample (first 2KB):\n{}", safe);
            Path tmp = Path.of("/tmp", "tripinfo-sample-" + System.currentTimeMillis() + ".xml");
            Files.writeString(tmp, sample);
            log.warn("[SUMO Adapter] wrote tripinfo sample to {}", tmp.toAbsolutePath());
        } catch (Exception ex) {
            log.warn("[SUMO Adapter] failed to write tripinfo sample: {}", ex.getMessage());
        }
    }

    private String recoverTripinfoElements(String content) {
        if (content == null || content.isEmpty()) return null;
        Pattern p = Pattern.compile("(?s)(<tripinfo\\b[^>]*>(?:.*?</tripinfo>)|<tripinfo\\b[^>]*/>)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(content);
        StringBuilder sb = new StringBuilder();
        int found = 0;
        while (m.find()) {
            sb.append(m.group(1));
            found++;
        }
        if (found == 0) return null;
        String wrapper = "<root>" + sb.toString() + "</root>";
        recoveredTripinfoElements.addAndGet(found);
        log.info("[SUMO Adapter] recovered {} <tripinfo> elements by wrapping into root for parsing.", found);
        return wrapper;
    }

    private String sanitizeAttributeValues(String xml) {
        Pattern attrPattern = Pattern.compile("(\\s+[^=\\s<>\"']+)=(\"(.*?)\"|'(.*?)')", Pattern.DOTALL);
        Matcher m = attrPattern.matcher(xml);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String leading = m.group(1); // includes leading whitespace + attribute name
            String full = m.group(2); // quoted value including quotes
            String inner = m.group(3) != null ? m.group(3) : m.group(4);
            if (inner == null) inner = "";

            // Escape ampersands that are not part of an entity (e.g., &amp; or &#123;)
            String escaped = inner.replaceAll("&(?!#?\\w+;)", "&amp;");
            // Escape angle brackets in attribute values
            escaped = escaped.replace("<", "&lt;").replace(">", "&gt;");

            // Reconstruct full attribute: preserve leading name and equals then the quoted escaped value
            char quoteChar = full.charAt(0);
            String replacement = leading + "=" + quoteChar + escaped + quoteChar;
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String removeControlChars(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeAttributeUnsafeChars(String input) {
        if (input == null || input.isEmpty()) return input;

        // Pattern to find double-quoted attribute values: ="..."
        Pattern dq = Pattern.compile("=\\\"([^\\\"]*)\\\"");
        // Pattern to find single-quoted attribute values: ='...'
        Pattern sq = Pattern.compile("=\\'([^\\']*)\\'");

        StringBuffer sb = new StringBuffer(input.length());
        Matcher m = dq.matcher(input);
        while (m.find()) {
            String val = m.group(1);
            String escaped = val.replaceAll("&(?!#\\d+;)(?!#x[0-9a-fA-F]+;)(?![a-zA-Z]+;)", "&amp;");
            escaped = escaped.replace("<", "&lt;");
            m.appendReplacement(sb, "=\"" + Matcher.quoteReplacement(escaped) + "\"");
        }
        m.appendTail(sb);

        String interim = sb.toString();
        sb.setLength(0);

        m = sq.matcher(interim);
        while (m.find()) {
            String val = m.group(1);
            String escaped = val.replaceAll("&(?!#\\d+;)(?!#x[0-9a-fA-F]+;)(?![a-zA-Z]+;)", "&amp;");
            escaped = escaped.replace("<", "&lt;");
            m.appendReplacement(sb, "='" + Matcher.quoteReplacement(escaped) + "'");
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private static int indexOfIgnoreCase(String src, String target) {
        if (src == null || target == null) return -1;
        String lower = src.toLowerCase();
        return lower.indexOf(target.toLowerCase());
    }

    private static String sanitizeXmlDeclarations(String content) {
        if (content == null || content.isEmpty()) return content;
        String lower = content.toLowerCase();
        String decl = "<?xml";
        int first = lower.indexOf(decl);
        if (first == -1) return content;
        // keep first occurrence; escape all subsequent occurrences
        int from = first + decl.length();
        StringBuilder sb = new StringBuilder(content.length());
        sb.append(content, 0, from);
        int idx = from;
        while (true) {
            int next = lower.indexOf(decl, idx);
            if (next == -1) {
                sb.append(content.substring(idx));
                break;
            }
            // append content between idx and next, but replace the '<?xml' at next with '&lt;?xml'
            sb.append(content, idx, next);
            sb.append("&lt;?xml");
            idx = next + decl.length();
        }
        return sb.toString();
    }

    private double doubleAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value.isBlank() ? 0 : Double.parseDouble(value);
    }
}