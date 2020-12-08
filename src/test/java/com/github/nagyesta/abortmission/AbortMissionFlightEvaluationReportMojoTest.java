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
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.github.nagyesta.abortmission.AbortMissionFlightEvaluationReportMojo.ARTIFACT_ID;
import static com.github.nagyesta.abortmission.AbortMissionFlightEvaluationReportMojo.GROUP_ID;
import static com.github.nagyesta.abortmission.AbortMissionFlightEvaluationReportMojo.JAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
        final String jarVersion = "1.0.0";
        final boolean relaxed = true;
        final List<ArtifactResult> artifactResults = Arrays.asList(
                artifactResult(artifact(true, true, true)),
                artifactResult(artifact(false, false, true)),
                artifactResult(artifact(false, true, false)),
                artifactResult(artifact(true, false, false)),
                artifactResult(artifact(false, false, false))
        );

        final AbortMissionFlightEvaluationReportMojo raw = rawTestInstance(jarVersion, relaxed);

        final List<String> expectedCommandTokens = expectedCommandTokens(relaxed);
        final ProjectBuildingRequest projectBuildingRequest = mock(ProjectBuildingRequest.class);
        final MavenSession session = mockSession(raw, projectBuildingRequest);
        final DefaultDependableCoordinate coordinate = expectedCoordinates(jarVersion);
        final DependencyResolver dependencyResolver = mockDependencyResolver(raw, projectBuildingRequest, artifactResults);

        final Process process = mock(Process.class);
        final AbortMissionFlightEvaluationReportMojo underTest = spy(raw);
        final List<String> commandTokens = mockProcessToCaptureCommandTokens(process, underTest);

        //when
        underTest.execute();

        //then
        final DefaultDependableCoordinate coordinatesUsed = raw.missionReportCoordinates();
        assertEquals(coordinate.getGroupId(), coordinatesUsed.getGroupId());
        assertEquals(coordinate.getArtifactId(), coordinatesUsed.getArtifactId());
        assertEquals(coordinate.getType(), coordinatesUsed.getType());
        assertEquals(coordinate.getVersion(), coordinatesUsed.getVersion());
        assertIterableEquals(expectedCommandTokens, commandTokens);

        final InOrder inOrder = inOrder(underTest, session, dependencyResolver, process);
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
            throws DependencyResolverException, IOException, MojoExecutionException, InterruptedException {
        //given
        final String jarVersion = "1.1.0";
        final boolean relaxed = false;
        final List<ArtifactResult> artifactResults = Arrays.asList(
                artifactResult(artifact(true, true, true)),
                artifactResult(artifact(false, false, true)),
                artifactResult(artifact(false, true, false)),
                artifactResult(artifact(true, false, false))
        );

        final AbortMissionFlightEvaluationReportMojo raw = rawTestInstance(jarVersion, relaxed);

        final ProjectBuildingRequest projectBuildingRequest = mock(ProjectBuildingRequest.class);
        final MavenSession session = mockSession(raw, projectBuildingRequest);
        final DefaultDependableCoordinate coordinate = expectedCoordinates(jarVersion);
        final DependencyResolver dependencyResolver = mockDependencyResolver(raw, projectBuildingRequest, artifactResults);

        final Process process = mock(Process.class);
        final AbortMissionFlightEvaluationReportMojo underTest = spy(raw);

        //when
        assertThrows(MojoExecutionException.class, underTest::execute);

        //then
        final DefaultDependableCoordinate coordinatesUsed = raw.missionReportCoordinates();
        assertEquals(coordinate.getGroupId(), coordinatesUsed.getGroupId());
        assertEquals(coordinate.getArtifactId(), coordinatesUsed.getArtifactId());
        assertEquals(coordinate.getType(), coordinatesUsed.getType());
        assertEquals(coordinate.getVersion(), coordinatesUsed.getVersion());

        final InOrder inOrder = inOrder(underTest, session, dependencyResolver, process);
        inOrder.verify(underTest).execute();
        inOrder.verify(session).getProjectBuildingRequest();
        inOrder.verify(dependencyResolver)
                .resolveDependencies(same(projectBuildingRequest), any(DependableCoordinate.class), any(PatternExclusionsFilter.class));
        inOrder.verify(underTest, never()).createProcess(any());
    }

    private AbortMissionFlightEvaluationReportMojo rawTestInstance(final String jarVersion, final boolean relaxed) {
        final AbortMissionFlightEvaluationReportMojo raw = new AbortMissionFlightEvaluationReportMojo();
        raw.setJarVersion(jarVersion);
        raw.setRelaxed(relaxed);
        raw.setInputFile(JSON_FILE);
        raw.setOutputFile(HTML_FILE);
        return raw;
    }

    private DefaultDependableCoordinate expectedCoordinates(final String jarVersion) {
        final DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
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

    private MavenSession mockSession(final AbortMissionFlightEvaluationReportMojo raw,
                                     final ProjectBuildingRequest projectBuildingRequest) {
        final MavenSession session = mock(MavenSession.class);
        when(session.getProjectBuildingRequest()).thenReturn(projectBuildingRequest);
        raw.setSession(session);
        return session;
    }

    @SuppressWarnings("unchecked")
    private List<String> mockProcessToCaptureCommandTokens(final Process process,
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

    private DependencyResolver mockDependencyResolver(final AbortMissionFlightEvaluationReportMojo raw,
                                                      final ProjectBuildingRequest projectBuildingRequest,
                                                      final List<ArtifactResult> artifactResults)
            throws DependencyResolverException {
        final DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        when(dependencyResolver.resolveDependencies(
                same(projectBuildingRequest), any(DependableCoordinate.class), any(PatternExclusionsFilter.class))
        ).thenReturn(artifactResults);
        raw.setDependencyResolver(dependencyResolver);
        return dependencyResolver;
    }

    private ArtifactResult artifactResult(final Artifact artifact) {
        final ArtifactResult artifactResult = mock(ArtifactResult.class);
        when(artifactResult.getArtifact()).thenReturn(artifact);
        return artifactResult;
    }

    private Artifact artifact(final boolean groupId, final boolean artifactId, final boolean type) {
        final Artifact artifact = mock(Artifact.class);
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
