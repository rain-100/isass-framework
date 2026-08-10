// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.contract;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;

public class ContractResourceLoader {

    public static final String RESOURCE = "META-INF/isass/nocode-contract.json";

    private final ObjectMapper objectMapper;
    private final ClassLoader classLoader;

    public ContractResourceLoader(ObjectMapper objectMapper, ClassLoader classLoader) {
        this.objectMapper = objectMapper;
        this.classLoader = classLoader;
    }

    public List<ContractDocument> load() {
        try {
            Enumeration<URL> resources = classLoader.getResources(RESOURCE);
            List<ContractDocument> documents = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (var input = resource.openStream()) {
                    ContractDocument document =
                            objectMapper.readValue(input, ContractDocument.class);
                    validateHash(document, resource);
                    documents.add(document);
                }
            }
            return List.copyOf(documents);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load  contracts", exception);
        }
    }

    private void validateHash(ContractDocument document, URL resource) {
        if (document.contentHash().isBlank()) {
            return;
        }
        try {
            ContractDocument unhashed = new ContractDocument(
                    document.version(), "", document.services(), document.types());
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(unhashed)));
            if (!actual.equalsIgnoreCase(document.contentHash())) {
                throw new IllegalStateException(" contract hash mismatch: " + resource);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot validate  contract: " + resource, exception);
        }
    }
}
