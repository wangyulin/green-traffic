package com.greentraffic.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class NoDirectSystemCallsTest {

    @Test
    void coreShouldNotUseDirectUuidOrInstantOrThreadLocalRandom() throws IOException {
        Path src = Paths.get("src/main/java/com/greentraffic/core");
        if (!Files.exists(src)) return; // nothing to check

        List<Path> files = Files.walk(src)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        StringBuilder violations = new StringBuilder();
        for (Path f : files) {
            List<String> lines = Files.readAllLines(f);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                // detect explicit UUID.randomUUID(...) usage
                if (line.contains("UUID.randomUUID(")) {
                    violations.append(f.toString()).append(":").append(i + 1).append(" -> ").append(line.trim()).append("\n");
                    continue;
                }
                // detect Instant.now() without clock argument
                if (line.contains("Instant.now()") || line.contains("Instant.now (")) {
                    violations.append(f.toString()).append(":").append(i + 1).append(" -> ").append(line.trim()).append("\n");
                    continue;
                }
                // detect ThreadLocalRandom.current() usage
                if (line.contains("ThreadLocalRandom.current(")) {
                    violations.append(f.toString()).append(":").append(i + 1).append(" -> ").append(line.trim()).append("\n");
                }
            }
        }

        if (violations.length() > 0) {
            fail("Found direct system calls in core sources:\n" + violations.toString());
        }
    }
}
