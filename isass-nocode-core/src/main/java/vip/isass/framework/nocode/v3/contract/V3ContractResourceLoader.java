package vip.isass.framework.nocode.v3.contract;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;

public class V3ContractResourceLoader {

    public static final String RESOURCE = "META-INF/isass/v3-contract.json";

    private final ObjectMapper objectMapper;
    private final ClassLoader classLoader;

    public V3ContractResourceLoader(ObjectMapper objectMapper, ClassLoader classLoader) {
        this.objectMapper = objectMapper;
        this.classLoader = classLoader;
    }

    public List<V3ContractDocument> load() {
        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE);
            List<V3ContractDocument> documents = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (var input = resource.openStream()) {
                    V3ContractDocument document =
                            objectMapper.readValue(input, V3ContractDocument.class);
                    validateHash(document, resource);
                    documents.add(document);
                }
            }
            return List.copyOf(documents);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load V3 contracts", exception);
        }
    }

    private void validateHash(V3ContractDocument document, URL resource) {
        if (document.contentHash().isBlank()) {
            return;
        }
        try {
            V3ContractDocument unhashed = new V3ContractDocument(
                    document.version(), "", document.services(), document.types());
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(unhashed)));
            if (!actual.equalsIgnoreCase(document.contentHash())) {
                throw new IllegalStateException("V3 contract hash mismatch: " + resource);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot validate V3 contract: " + resource, exception);
        }
    }
}
