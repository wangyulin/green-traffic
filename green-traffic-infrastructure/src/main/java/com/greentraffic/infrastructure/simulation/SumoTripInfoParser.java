package com.greentraffic.infrastructure.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.greentraffic.core.port.output.simulation.SumoTripInfo;

@Component
public class SumoTripInfoParser {

    private static final Logger log = LoggerFactory.getLogger(SumoTripInfoParser.class);

    private final AtomicLong recoveredTripinfoElements = new AtomicLong(0);
    private final AtomicLong nullSanitizationCount = new AtomicLong(0);

    public List<SumoTripInfo> parseTripInfo(Path tripInfoFile) throws Exception {
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

    public Document parseDocument(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
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
        try (InputStream is = Files.newInputStream(file)) {
            return builder.parse(is);
        }
    }

    public Document parse(Path file) throws Exception {
        final int maxAttempts = 5;
        final long retryDelayMs = 200L;
        String content = "";

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            byte[] raw = Files.readAllBytes(file);
            content = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
            if (content.indexOf('\0') >= 0) {
                nullSanitizationCount.incrementAndGet();
                if (attempt == 1) {
                    log.warn("[SUMO Adapter] tripinfo.xml contains invalid NULL characters; sanitizing before parsing: {} (attempt {})", file, attempt);
                } else {
                    log.debug("[SUMO Adapter] tripinfo.xml contains NULL chars; sanitizing (attempt {}) for file {}", attempt, file);
                }
            }

            content = removeControlChars(content);
            content = escapeAttributeUnsafeChars(content);

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

            content = sanitizeXmlDeclarations(content);

            if (!content.trim().isEmpty()) {
                break;
            }

            if (attempt < maxAttempts) {
                try { Thread.sleep(retryDelayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
                log.debug("[SUMO Adapter] tripinfo.xml empty; retrying read (attempt {}/{})", attempt + 1, maxAttempts);
            }
        }

        if (content.trim().isEmpty()) {
            throw new java.io.IOException("tripinfo.xml is empty or truncated after retries: " + file);
        }

        content = removeControlChars(content);
        content = sanitizeAttributeValues(content);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
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

        String preRecovered = recoverTripinfoElements(content);
        if (preRecovered != null) {
            try (InputStream is = new ByteArrayInputStream(preRecovered.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return builder.parse(is);
            } catch (org.xml.sax.SAXParseException preEx) {
                log.info("[SUMO Adapter] preemptive recovered-parse failed: {}. Falling back to full parse.", preEx.getMessage());
                dumpContentSample(preRecovered, file);
            }
        }

        try (InputStream is = new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return builder.parse(is);
        } catch (org.xml.sax.SAXParseException saxEx) {
            log.info("[SUMO Adapter] parse failed after sanitization: {}. Trying element-level recovery.", saxEx.getMessage());
            dumpContentSample(content, file);
            String recovered = recoverTripinfoElements(content);
            if (recovered == null) throw saxEx;
            try (InputStream is2 = new ByteArrayInputStream(recovered.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return builder.parse(is2);
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

        int found = 0;
        StringBuilder sb = new StringBuilder();

        while (true) {
            int start = indexOfIgnoreCase(content, "<tripinfo");
            if (start == -1) break;
            int pos = start;
            boolean inQuote = false;
            char quoteChar = 0;
            int endOfStartTag = -1;
            for (pos = start; pos < content.length(); pos++) {
                char c = content.charAt(pos);
                if (!inQuote && (c == '"' || c == '\'')) {
                    inQuote = true; quoteChar = c;
                } else if (inQuote && c == quoteChar) {
                    inQuote = false;
                } else if (!inQuote && c == '>') {
                    endOfStartTag = pos; break;
                }
            }
            if (endOfStartTag == -1) break;
            boolean selfClosing = content.charAt(endOfStartTag - 1) == '/';
            int endOfElement = endOfStartTag;
            if (!selfClosing) {
                int closeTag = content.toLowerCase().indexOf("</tripinfo>", endOfStartTag + 1);
                if (closeTag != -1) endOfElement = closeTag + "</tripinfo>".length() - 1;
                else endOfElement = endOfStartTag;
            }

            String startTag = content.substring(start, endOfStartTag + 1);
            java.util.Map<String, String> attrs = new java.util.LinkedHashMap<>();
            int p = 8;
            while (p < startTag.length() - 1) {
                while (p < startTag.length() && Character.isWhitespace(startTag.charAt(p))) p++;
                if (p >= startTag.length() - 1) break;
                int nameStart = p;
                while (p < startTag.length()) {
                    char ch = startTag.charAt(p);
                    if (ch == '=' || Character.isWhitespace(ch) || ch == '>' || ch == '/') break;
                    p++;
                }
                if (p >= startTag.length()) break;
                String name = startTag.substring(nameStart, p).trim();
                if (name.isEmpty()) break;
                while (p < startTag.length() && Character.isWhitespace(startTag.charAt(p))) p++;
                if (p >= startTag.length() || startTag.charAt(p) != '=') continue;
                p++;
                while (p < startTag.length() && Character.isWhitespace(startTag.charAt(p))) p++;
                if (p >= startTag.length()) break;
                char qc = startTag.charAt(p);
                String value = "";
                if (qc == '"' || qc == '\'') {
                    p++; int valStart = p; StringBuilder valSb = new StringBuilder(); boolean closed = false;
                    for (; p < startTag.length(); p++) { char cc = startTag.charAt(p); if (cc == qc) { closed = true; p++; break; } valSb.append(cc); }
                    value = valSb.toString(); if (!closed) value = value.trim();
                } else {
                    int valStart = p; while (p < startTag.length()) { char cc = startTag.charAt(p); if (Character.isWhitespace(cc) || cc == '>') break; p++; }
                    value = startTag.substring(valStart, p);
                }
                String safe = value.replaceAll("&(?!#?\\w+;)", "&amp;");
                safe = safe.replace("<", "&lt;").replace(">", "&gt;");
                attrs.put(name, safe);
            }

            StringBuilder elem = new StringBuilder();
            elem.append("<tripinfo");
            for (java.util.Map.Entry<String, String> e : attrs.entrySet()) {
                elem.append(' ').append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
            }
            elem.append("/>");
            sb.append(elem.toString()).append('\n');
            found++;

            int after = Math.min(endOfElement + 1, content.length());
            content = content.substring(after);
        }

        if (found == 0) return null;
        recoveredTripinfoElements.addAndGet(found);
        String wrapper = "<root>\n" + sb.toString() + "</root>\n";
        log.info("[SUMO Adapter] safely recovered {} <tripinfo> elements into wrapper for parsing.", found);
        return wrapper;
    }

    private String sanitizeAttributeValues(String xml) {
        Pattern attrPattern = Pattern.compile("(\\s+[^=\\s<>\\\"']+)=(\"(.*?)\"|'(.*?)')", Pattern.DOTALL);
        Matcher m = attrPattern.matcher(xml);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String leading = m.group(1);
            String full = m.group(2);
            String inner = m.group(3) != null ? m.group(3) : m.group(4);
            if (inner == null) inner = "";
            String escaped = inner.replaceAll("&(?!#?\\w+;)", "&amp;");
            escaped = escaped.replace("<", "&lt;").replace(">", "&gt;");
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
            if (c == '\t' || c == '\n' || c == '\r' || c >= 0x20) sb.append(c);
        }
        return sb.toString();
    }

    private static String escapeAttributeUnsafeChars(String input) {
        if (input == null || input.isEmpty()) return input;
        Pattern dq = Pattern.compile("=\\\"([^\\\"]*)\\\"");
        Pattern sq = Pattern.compile("=\\\'([^\\\']*)\\\'");
        StringBuffer sb = new StringBuffer(input.length());
        Matcher m = dq.matcher(input);
        while (m.find()) {
            String val = m.group(1);
            String escaped = val.replaceAll("&(?!#\\d+;)(?!#x[0-9a-fA-F]+;)(?![a-zA-Z]+;)", "&amp;");
            escaped = escaped.replace("<", "&lt;");
            m.appendReplacement(sb, "=\"" + Matcher.quoteReplacement(escaped) + "\"");
        }
        m.appendTail(sb);
        String interim = sb.toString(); sb.setLength(0);
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
        int from = first + decl.length();
        StringBuilder sb = new StringBuilder(content.length());
        sb.append(content, 0, from);
        int idx = from;
        while (true) {
            int next = lower.indexOf(decl, idx);
            if (next == -1) { sb.append(content.substring(idx)); break; }
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
