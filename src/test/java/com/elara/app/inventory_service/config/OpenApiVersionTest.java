package com.elara.app.inventory_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig version")
class OpenApiVersionTest {

    @Test
    @DisplayName("OpenAPI info.version matches the project version in pom.xml")
    void openApiVersion_matchesPomProjectVersion() throws Exception {
        String projectVersion = readProjectVersion(locatePom());
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo()).as("OpenAPI info block").isNotNull();
        assertThat(openAPI.getInfo().getVersion())
            .as("OpenAPI info.version must match the project version in pom.xml")
            .isEqualTo(projectVersion);
    }

    private static Path locatePom() {
        Path pomPath = Path.of("pom.xml");
        if (Files.isRegularFile(pomPath)) {
            return pomPath;
        }

        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("pom.xml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }

        throw new AssertionError("Could not locate pom.xml starting from " + Path.of("").toAbsolutePath());
    }

    private static String readProjectVersion(Path pomPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        Document document = factory.newDocumentBuilder().parse(pomPath.toFile());
        Element project = document.getDocumentElement();
        NodeList children = project.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "version".equals(child.getNodeName())) {
                String version = child.getTextContent().trim();
                if (version.isEmpty()) {
                    throw new AssertionError("pom.xml declares an empty project version: " + pomPath);
                }
                return version;
            }
        }

        throw new AssertionError("pom.xml has no direct <version> element under <project>: " + pomPath);
    }
}
