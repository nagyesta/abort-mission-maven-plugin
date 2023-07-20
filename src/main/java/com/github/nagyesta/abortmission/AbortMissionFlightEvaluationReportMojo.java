package com.github.nagyesta.abortmission;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.PatternExclusionsFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mojo downloading and executing the Abort-Mission Flight Evaluation Report module.
 */
@Mojo(
        name = "flight-eval-report",
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class AbortMissionFlightEvaluationReportMojo extends AbstractMojo {

    static final String GROUP_ID = "com.github.nagyesta.abort-mission.reports";
    static final String ARTIFACT_ID = "abort.flight-evaluation-report";
    static final String JAR = "jar";

    @Component
    private DependencyResolver dependencyResolver;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    @Parameter(property = "mojo.abortmission.output",
            defaultValue = "${project.build.directory}/reports/abort-mission/abort-mission-report.html")
    private File outputFile;
    @Parameter(property = "mojo.abortmission.input",
            defaultValue = "${project.build.directory}/reports/abort-mission/abort-mission-report.json")
    private File inputFile;
    @Parameter(property = "mojo.abortmission.version", defaultValue = "RELEASE")
    private String jarVersion;
    @Parameter(property = "mojo.abortmission.relaxed", defaultValue = "false")
    private boolean relaxed;

    /**
     * Entry point for the module.
     *
     * @throws MojoExecutionException When the Mojo execution fails.
     */
    @SuppressWarnings("LocalCanBeFinal")
    public void execute() throws MojoExecutionException {
        try {
            final File jarFile = resolveMissionReportArtifactJar();
            getLog().debug("Report generator resolved: " + jarFile.getAbsolutePath());
            getLog().debug("Input file: " + inputFile.getAbsolutePath());
            getLog().debug("Output file: " + outputFile.getAbsolutePath());
            final List<String> commands = prepareCommands(jarFile);
            getLog().info("Executing command: " + commands);
            final Process javaProcess = createProcess(commands);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(javaProcess.getInputStream()))) {
                final List<String> logLines = reader.lines().collect(Collectors.toList());
                final int statusCode = javaProcess.waitFor();
                if (statusCode != 0) {
                    logLines.forEach(getLog()::error);
                    getLog().error("Execution failed with status code: " + statusCode);
                    throw new IllegalStateException("Execution failed, please check logs for more details.");
                } else {
                    logLines.forEach(getLog()::info);
                    getLog().info("Execution completed.");
                }
            }
        } catch (final Exception e) {
            throw new MojoExecutionException("Error generating report: " + outputFile, e);
        }
    }

    /**
     * Creates a process from the given command tokens.
     *
     * @param commandTokens The tokens of the command.
     * @return The process we create from the command
     * @throws IOException if an I/O error occurs for the process.
     */
    protected Process createProcess(final List<String> commandTokens) throws IOException {
        return new ProcessBuilder().command(commandTokens)
                .redirectErrorStream(true)
                .start();
    }

    /**
     * Prepares the command based on the JAR file we have resolved previously.
     *
     * @param jarFile The JAR file.
     * @return The list of command tokens in the order we need to run them.
     */
    protected List<String> prepareCommands(final File jarFile) {
        return Arrays.asList("java", "-jar", jarFile.getAbsolutePath(),
                "--report.input=" + inputFile.getAbsolutePath(),
                "--report.output=" + outputFile.getAbsolutePath(),
                "--report.relaxed=" + relaxed);
    }

    /**
     * Resolves the JAR dependency of the mission report generator.
     *
     * @return The resolved JAR file.
     * @throws DependencyResolverException If dependency resolution fails.
     * @throws MojoExecutionException      If the file is not the right type or the result was empty.
     */
    protected File resolveMissionReportArtifactJar()
            throws DependencyResolverException, MojoExecutionException {
        final Set<ArtifactResult> results = resolveMatchingDependencies();
        final ArtifactResult reportArtifact = results.stream()
                .filter(dep -> JAR.equals(dep.getArtifact().getType()))
                .filter(dep -> ARTIFACT_ID.equals(dep.getArtifact().getArtifactId()))
                .filter(dep -> GROUP_ID.equals(dep.getArtifact().getGroupId()))
                .findFirst()
                .orElseThrow(() -> new MojoExecutionException("Flight Evaluation report resolution failed."));
        return reportArtifact.getArtifact().getFile();
    }

    /**
     * Returns the Maven coordinates for the mission report generator JAR.
     *
     * @return the coordinates
     */
    protected DefaultDependableCoordinate missionReportCoordinates() {
        final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
        coordinate.setArtifactId(ARTIFACT_ID);
        coordinate.setGroupId(GROUP_ID);
        coordinate.setType(JAR);
        coordinate.setVersion(jarVersion);
        return coordinate;
    }

    private Set<ArtifactResult> resolveMatchingDependencies() throws DependencyResolverException {
        final ProjectBuildingRequest buildingRequest = session.getProjectBuildingRequest();
        final Set<ArtifactResult> results = new HashSet<>();
        final Iterable<ArtifactResult> dependencies = dependencyResolver.resolveDependencies(buildingRequest, missionReportCoordinates(),
                new PatternExclusionsFilter(Collections.emptyList()));
        Optional.ofNullable(dependencies).orElse(Collections.emptyList()).forEach(results::add);
        return results;
    }

    /**
     * Getter for dependencyResolver.
     *
     * @return dependencyResolver
     */
    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Setter for dependencyResolver.
     *
     * @param dependencyResolver The new value
     */
    public void setDependencyResolver(final DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    /**
     * Getter for session.
     *
     * @return session
     */
    public MavenSession getSession() {
        return session;
    }

    /**
     * Setter for session.
     *
     * @param session The new value
     */
    public void setSession(final MavenSession session) {
        this.session = session;
    }

    /**
     * Getter for outputFile.
     *
     * @return outputFile
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Setter for outputFile.
     *
     * @param outputFile The new value
     */
    public void setOutputFile(final File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Getter for inputFile.
     *
     * @return inputFile
     */
    public File getInputFile() {
        return inputFile;
    }

    /**
     * Setter for inputFile.
     *
     * @param inputFile The new value
     */
    public void setInputFile(final File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Getter for jarVersion.
     *
     * @return jarVersion
     */
    public String getJarVersion() {
        return jarVersion;
    }

    /**
     * Setter for jarVersion.
     *
     * @param jarVersion The new value
     */
    public void setJarVersion(final String jarVersion) {
        this.jarVersion = jarVersion;
    }

    /**
     * Getter for relaxed.
     *
     * @return relaxed
     */
    public boolean isRelaxed() {
        return relaxed;
    }

    /**
     * Setter for relaxed.
     *
     * @param relaxed The new value
     */
    public void setRelaxed(final boolean relaxed) {
        this.relaxed = relaxed;
    }
}
