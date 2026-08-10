// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.generator.SmartDocConfigGenerator;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/** Generates the effective smart-doc configuration for the current Maven project only. */
@Mojo(name = "generate-smart-doc-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateSmartDocConfigMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.compileSourceRoots}", required = true, readonly = true)
    private java.util.List<String> sourceRoots;

    @Parameter(defaultValue = "${project.artifactId}", required = true, readonly = true)
    private String projectName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Set<java.nio.file.Path> roots = sourceRoots.stream().map(java.nio.file.Path::of).collect(Collectors.toSet());
            var config = new SmartDocConfigGenerator(new ObjectMapper())
                    .generate(projectName, roots, outputDirectory.toPath());
            getLog().info("Generated smart-doc configuration: " + config);
        } catch (Exception exception) {
            throw new MojoExecutionException("Cannot generate smart-doc configuration", exception);
        }
    }
}
