package vip.isass.framework.nocode.generator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates smart-doc configuration from REST controllers owned by the current Maven project. */
public final class SmartDocConfigGenerator {

    private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");

    private final ObjectMapper objectMapper;

    public SmartDocConfigGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path generate(String projectName, Set<Path> sourceRoots, Path outputDirectory) throws IOException {
        Set<String> controllerPackages = new TreeSet<>();
        for (Path sourceRoot : sourceRoots) {
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (var files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(path -> collectControllerPackage(path, controllerPackages));
            }
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("projectName", projectName);
        root.put("outPath", outputDirectory.resolve("openapi3").toString());
        root.put("openUrl", false);
        root.put("allInOne", true);
        root.put("coverOld", true);
        root.put("packageFilters", controllerPackages.isEmpty()
                ? "isass.generated.no.controller"
                : String.join(",", controllerPackages));
        root.put("requestFieldToUnderline", false);
        root.put("responseFieldToUnderline", false);
        root.put("inlineEnum", true);
        root.put("componentType", "NORMAL");
        root.put("displayActualType", true);
        root.put("isStrict", false);
        Path config = outputDirectory.resolve("isass/smart-doc.json");
        Files.createDirectories(config.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(config.toFile(), root);
        return config;
    }

    private static void collectControllerPackage(Path source, Set<String> packages) {
        try {
            String content = Files.readString(source);
            if (!content.contains("@RestController") && !content.contains("@Controller")) {
                return;
            }
            Matcher matcher = PACKAGE.matcher(content);
            if (matcher.find()) {
                packages.add(matcher.group(1) + ".*");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read source file: " + source, exception);
        }
    }
}
