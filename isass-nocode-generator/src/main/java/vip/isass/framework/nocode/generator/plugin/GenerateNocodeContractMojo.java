// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.plugin;

import vip.isass.framework.nocode.generator.ContractGenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateNocodeContractMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/java", required = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip || !sourceDirectory.isDirectory()) {
            return;
        }
        try {
            var document = new ContractGenerator(new ObjectMapper())
                    .generate(sourceDirectory.toPath(), outputDirectory.toPath());
            getLog().info("Generated nocode contract for " + document.services().size() + " services");
        } catch (Exception exception) {
            throw new MojoExecutionException("Cannot generate nocode transport contract", exception);
        }
    }
}
