package com.github.nagyesta.abortmission;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.PatternExclusionsFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.nagyesta.abortmission.AbortMissionFlightEvaluationReportMojo.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

class AbortMissionFlightEvaluationReportMojoTest {

    private static final String NOT_MATCHING = "NOT-MATCHING";
    private static final String JSON = "inout.json";
    private static final String HTML = "output.html";
    private static final File JSON_FILE = new File(JSON);
    private static final File HTML_FILE = new File(HTML);
    private static final File NOT_MATCHING_FILE = new File(NOT_MATCHING);
    private static final File MATCHING_FILE = new File(GROUP_ID + "_" + ARTIFACT_ID + "." + JAR);

    @Test
    void testExecuteShouldResolveAndRunTheJarWhenCalled()
            throws DependencyResolverException, IOException, MojoExecutionException, InterruptedException {
        //given
        final var jarVersion = "1.0.0";
        final var relaxed = true;
        final var artifactResults = Arrays.asList(
                artifactResult(artifact(true, true, true)),
                artifactResult(artifact(false, false, true)),
                artifactResult(artifact(false, true, false)),
                artifactResult(artifact(true, false, false)),
                artifactResult(artifact(false, false, false))
        );


        final var expectedCommandTokens = expectedCommandTokens(relaxed);
        final var projectBuildingRequest = mock(ProjectBuildingRequest.class);
        final var coordinate = expectedCoordinates(jarVersion);
        final var dependencyResolver = mockDependencyResolver(projectBuildingRequest, artifactResults);
        final var raw = rawTestInstance(dependencyResolver, jarVersion, relaxed);
        final var session = mockSession(raw, projectBuildingRequest);

        final var process = mock(Process.class);
        when(process.waitFor()).thenReturn(0);
        final var underTest = spy(raw);
        final var commandTokens = mockProcessToCaptureCommandTokens(process, underTest);

        //when
        underTest.execute();

        //then
        final var coordinatesUsed = raw.missionReportCoordinates();
        assertEquals(coordinate.getGroupId(), coordinatesUsed.getGroupId());
        assertEquals(coordinate.getArtifactId(), coordinatesUsed.getArtifactId());
        assertEquals(coordinate.getType(), coordinatesUsed.getType());
        assertEquals(coordinate.getVersion(), coordinatesUsed.getVersion());
        assertIterableEquals(expectedCommandTokens, commandTokens);

        final var inOrder = inOrder(underTest, session, dependencyResolver, process);
        inOrder.verify(underTest).execute();
        inOrder.verify(session).getProjectBuildingRequest();
        inOrder.verify(dependencyResolver)
                .resolveDependencies(
                        same(projectBuildingRequest),
                        any(DependableCoordinate.class),
                        any(PatternExclusionsFilter.class));
        inOrder.verify(underTest).createProcess(any());
        inOrder.verify(process).getInputStream();
        inOrder.verify(process).waitFor();
    }

    @Test
    void testExecuteShouldThrowExceptionWhenJarProcessExitsWithNonZeroStatus()
            throws DependencyResolverException, IOException, MojoExecutionException, InterruptedException {
        //given
        final var jarVersion = "1.0.0";
        final var relaxed = true;
        final var artifactResults = Arrays.asList(
                artifactResult(artifact(true, true, true)),
                artifactResult(artifact(false, false, true)),
                artifactResult(artifact(false, true, false)),
                artifactResult(artifact(true, false, false)),
                artifactResult(artifact(false, false, false))
        );


        final var expectedCommandTokens = expectedCommandTokens(relaxed);
        final var projectBuildingRequest = mock(ProjectBuildingRequest.class);
        final var coordinate = expectedCoordinates(jarVersion);
        final var dependencyResolver = mockDependencyResolver(projectBuildingRequest, artifactResults);
        final var raw = rawTestInstance(dependencyResolver, jarVersion, relaxed);
        final var session = mockSession(raw, projectBuildingRequest);

        final var process = mock(Process.class);
        when(process.waitFor()).thenReturn(1);
        final var underTest = spy(raw);
        final var commandTokens = mockProcessToCaptureCommandTokens(process, underTest);

        //when
        assertThrows(MojoExecutionException.class, underTest::execute);

        //then
        final var coordinatesUsed = raw.missionReportCoordinates();
        assertEquals(coordinate.getGroupId(), coordinatesUsed.getGroupId());
        assertEquals(coordinate.getArtifactId(), coordinatesUsed.getArtifactId());
        assertEquals(coordinate.getType(), coordinatesUsed.getType());
        assertEquals(coordinate.getVersion(), coordinatesUsed.getVersion());
        assertIterableEquals(expectedCommandTokens, commandTokens);

        final var inOrder = inOrder(underTest, session, dependencyResolver, process);
        inOrder.verify(underTest).execute();
        inOrder.verify(session).getProjectBuildingRequest();
        inOrder.verify(dependencyResolver)
                .resolveDependencies(same(projectBuildingRequest), any(DependableCoordinate.class), any(PatternExclusionsFilter.class));
        inOrder.verify(underTest).createProcess(any());
        inOrder.verify(process).getInputStream();
        inOrder.verify(process).waitFor();
    }

    @Test
    void testExecuteShouldThrowExceptionWhenArtifactNotResolvedProperly()
            throws DependencyResolverException, IOException, MojoExecutionException {
        //given
        final var jarVersion = "1.1.0";
        final var relaxed = false;
        final var artifactResults = Arrays.asList(
                artifactResult(artifact(true, true, true)),
                artifactResult(artifact(false, false, true)),
                artifactResult(artifact(false, true, false)),
                artifactResult(artifact(true, false, false))
        );


        final var projectBuildingRequest = mock(ProjectBuildingRequest.class);
        final var coordinate = expectedCoordinates(jarVersion);
        final var dependencyResolver = mockDependencyResolver(projectBuildingRequest, artifactResults);
        final var raw = rawTestInstance(dependencyResolver, jarVersion, relaxed);
        final var session = mockSession(raw, projectBuildingRequest);

        final var process = mock(Process.class);
        final var underTest = spy(raw);

        //when
        assertThrows(MojoExecutionException.class, underTest::execute);

        //then
        final var coordinatesUsed = raw.missionReportCoordinates();
        assertEquals(coordinate.getGroupId(), coordinatesUsed.getGroupId());
        assertEquals(coordinate.getArtifactId(), coordinatesUsed.getArtifactId());
        assertEquals(coordinate.getType(), coordinatesUsed.getType());
        assertEquals(coordinate.getVersion(), coordinatesUsed.getVersion());

        final var inOrder = inOrder(underTest, session, dependencyResolver, process);
        inOrder.verify(underTest).execute();
        inOrder.verify(session).getProjectBuildingRequest();
        inOrder.verify(dependencyResolver)
                .resolveDependencies(same(projectBuildingRequest), any(DependableCoordinate.class), any(PatternExclusionsFilter.class));
        inOrder.verify(underTest, never()).createProcess(any());
    }

    private AbortMissionFlightEvaluationReportMojo rawTestInstance(
            final DependencyResolver dependencyResolver,
            final String jarVersion,
            final boolean relaxed) {
        final var raw = new AbortMissionFlightEvaluationReportMojo(dependencyResolver);
        raw.setJarVersion(jarVersion);
        raw.setRelaxed(relaxed);
        raw.setInputFile(JSON_FILE);
        raw.setOutputFile(HTML_FILE);
        return raw;
    }

    private DefaultDependableCoordinate expectedCoordinates(final String jarVersion) {
        final var coordinate = new DefaultDependableCoordinate();
        coordinate.setArtifactId(ARTIFACT_ID);
        coordinate.setGroupId(GROUP_ID);
        coordinate.setType(JAR);
        coordinate.setVersion(jarVersion);
        return coordinate;
    }

    private List<String> expectedCommandTokens(final boolean relaxed) {
        return Arrays.asList("java", "-jar", MATCHING_FILE.getAbsolutePath(),
                "--report.input=" + JSON_FILE.getAbsolutePath(),
                "--report.output=" + HTML_FILE.getAbsolutePath(),
                "--report.relaxed=" + relaxed);
    }

    private MavenSession mockSession(
            final AbortMissionFlightEvaluationReportMojo raw,
            final ProjectBuildingRequest projectBuildingRequest) {
        final var session = mock(MavenSession.class);
        when(session.getProjectBuildingRequest()).thenReturn(projectBuildingRequest);
        raw.setSession(session);
        return session;
    }

    @SuppressWarnings("unchecked")
    private List<String> mockProcessToCaptureCommandTokens(
            final Process process,
            final AbortMissionFlightEvaluationReportMojo underTest) throws IOException {
        final InputStream stream = new ByteArrayInputStream("IGNORED".getBytes());
        when(process.getInputStream()).thenReturn(stream);

        final List<String> commandTokens = new ArrayList<>();
        doAnswer(a -> {
            commandTokens.addAll((Collection<String>) a.getArguments()[0]);
            return process;
        }).when(underTest).createProcess(any());
        return commandTokens;
    }

    private DependencyResolver mockDependencyResolver(
            final ProjectBuildingRequest projectBuildingRequest,
            final List<ArtifactResult> artifactResults)
            throws DependencyResolverException {
        final var dependencyResolver = mock(DependencyResolver.class);
        when(dependencyResolver.resolveDependencies(
                same(projectBuildingRequest), any(DependableCoordinate.class), any(PatternExclusionsFilter.class))
        ).thenReturn(artifactResults);
        return dependencyResolver;
    }

    private ArtifactResult artifactResult(final Artifact artifact) {
        final var artifactResult = mock(ArtifactResult.class);
        when(artifactResult.getArtifact()).thenReturn(artifact);
        return artifactResult;
    }

    private Artifact artifact(
            final boolean groupId,
            final boolean artifactId,
            final boolean type) {
        final var artifact = mock(Artifact.class);
        if (groupId) {
            when(artifact.getGroupId()).thenReturn(NOT_MATCHING);
        } else {
            when(artifact.getGroupId()).thenReturn(GROUP_ID);
        }
        if (artifactId) {
            when(artifact.getArtifactId()).thenReturn(NOT_MATCHING);
        } else {
            when(artifact.getArtifactId()).thenReturn(ARTIFACT_ID);
        }
        if (type) {
            when(artifact.getType()).thenReturn(NOT_MATCHING);
        } else {
            when(artifact.getType()).thenReturn(JAR);
        }
        if (groupId || artifactId || type) {
            when(artifact.getFile()).thenReturn(NOT_MATCHING_FILE);
        } else {
            when(artifact.getFile()).thenReturn(MATCHING_FILE);
        }
        return artifact;
    }
}
