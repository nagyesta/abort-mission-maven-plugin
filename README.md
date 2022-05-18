![Abort-Mission](.github/assets/Abort-Mission-logo_export_transparent_640.png)

[![GitHub license](https://img.shields.io/github/license/nagyesta/abort-mission-maven-plugin?color=informational)](https://raw.githubusercontent.com/nagyesta/abort-mission-maven-plugin/main/LICENSE)
[![Java version](https://img.shields.io/badge/Java%20version-11-yellow?logo=java)](https://img.shields.io/badge/Java%20version-11-yellow?logo=java)
[![latest-release](https://img.shields.io/github/v/tag/nagyesta/abort-mission-maven-plugin?color=blue&logo=git&label=releases&sort=semver)](https://github.com/nagyesta/abort-mission-maven-plugin/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.nagyesta.abort-mission/abort-mission-maven-plugin?logo=apache-maven)](https://search.maven.org/artifact/com.github.nagyesta.abort-mission/abort-mission-maven-plugin)
[![JavaCI](https://img.shields.io/github/workflow/status/nagyesta/abort-mission-maven-plugin/JavaCI?logo=github)](https://img.shields.io/github/workflow/status/nagyesta/abort-mission-maven-plugin/JavaCI?logo=github)

[![codecov](https://img.shields.io/codecov/c/github/nagyesta/abort-mission-maven-plugin?label=Coverage&token=9L48Q6M9HN)](https://img.shields.io/codecov/c/github/nagyesta/abort-mission-maven-plugin?label=Coverage&token=9L48Q6M9HN)
[![code-climate-maintainability](https://img.shields.io/codeclimate/maintainability/nagyesta/abort-mission-maven-plugin?logo=code%20climate)](https://img.shields.io/codeclimate/maintainability/nagyesta/abort-mission-maven-plugin?logo=code%20climate)
[![code-climate-tech-debt](https://img.shields.io/codeclimate/tech-debt/nagyesta/abort-mission-maven-plugin?logo=code%20climate)](https://img.shields.io/codeclimate/tech-debt/nagyesta/abort-mission-maven-plugin?logo=code%20climate)
[![last_commit](https://img.shields.io/github/last-commit/nagyesta/abort-mission-maven-plugin?logo=git)](https://img.shields.io/github/last-commit/nagyesta/abort-mission-maven-plugin?logo=git)
[![wiki](https://img.shields.io/badge/See-Wiki-informational)](https://github.com/nagyesta/abort-mission/wiki)

Abort-Mission is a lightweight Java library providing flexible test abortion support for test groups to allow fast
failures.

This project provides Maven integration for Abort-Mission report generation.

## Installation

Abort-Mission can be downloaded from a few Maven repositories. Please head to
[this page](https://github.com/nagyesta/abort-mission/wiki/Configuring-our-repository-for-your-build-system)
to find out more.

**NOTE:** Please don't forget, that this is a plugin, you will need to add the repository as a plugin repository.

### Minimal configuration

```xml
<plugin>
    <groupId>com.github.nagyesta.abort-mission</groupId>
    <artifactId>abort-mission-maven-plugin</artifactId>
    <version>RELEASE</version>
    <executions>
        <execution>
            <id>generate-report</id>
            <goals>
                <goal>flight-eval-report</goal>
            </goals>
            <phase>prepare-package</phase>
        </execution>
    </executions>
</plugin>
```

### Configuration properties

The following optional properties can be provided to the plugin in the `<configuration>` tag if you wish to
override the default behavior

| Parameter    | Property                       | Description                                              | Default value                                                                |
| ------------ | ------------------------------ | -------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `inputFile`  | `${mojo.abortmission.input}`   | The location of the JSON input                           | `${project.build.directory}/reports/abort-mission/abort-mission-report.json` |
| `outputFile` | `${mojo.abortmission.output}`  | The location of the HTML output                          | `${project.build.directory}/reports/abort-mission/abort-mission-report.html` |
| `jarVersion` | `${mojo.abortmission.version}` | The version of the report generator                      | `RELEASE`                                                                    |
| `relaxed`    | `${mojo.abortmission.relaxed}` | Whether we can use relaxed JSON schema validation or not | `false`                                                                      |

## About the reports

[Flight Evaluation Report explained](https://github.com/nagyesta/abort-mission/wiki/Flight-Evaluation-Report-explained)
