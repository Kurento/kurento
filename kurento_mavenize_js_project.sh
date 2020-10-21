#!/usr/bin/env bash

# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# PROJECT_NAME string
#   Project name used in pom.xml
#
# MAVEN_SHELL_SCRIPT string
#   Script to be included in maven shell plugin
#
# ASSEMBLY_FILE path
#   Location of the assembly file to be used by maven. If not present a new
#   one will be created

# Get input parameters for backward compatibility
[[ -n "${1:-}" ]] && PROJECT_NAME="$1"
[[ -n "${2:-}" ]] && MAVEN_SHELL_SCRIPT="$2"
[[ -n "${3:-}" ]] && ASSEMBLY_FILE="$3"

# Validate parameters
[[ -z "$PROJECT_NAME" ]] && {
  log "ERROR: Undefined variable: PROJECT_NAME"
  exit 1
}

if [[ -n "$MAVEN_SHELL_SCRIPT" ]]; then
cat >maven_script.sh <<EOF
#!/usr/bin/env bash
# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset
# Trace all commands
set -o xtrace
$MAVEN_SHELL_SCRIPT
EOF
else
cat >maven_script.sh <<EOF
#!/usr/bin/env bash
# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset
# Trace all commands
set -o xtrace
echo "#### Run maven_script.sh ####"
npm install --no-color || { echo ERR7; exit 1; }
node_modules/.bin/grunt --no-color || { echo ERR8; exit 1; }
node_modules/.bin/grunt --no-color sync:bower || { echo ERR9; exit 1; }
mkdir -p src/main/resources/META-INF/resources/js/ || { echo ERR10; exit 1; }
cp dist/* src/main/resources/META-INF/resources/js/
EOF
fi

chmod +x maven_script.sh

[[ -z "$ASSEMBLY_FILE" ]] && ASSEMBLY_FILE="assembly.xml"

# Validate project structure
[[ -f package.json ]] || {
  log "ERROR: Cannot read file: package.json"
  exit 1
}

# Build maven version from package.json
VERSION="$(jshon -e version -u < package.json)" || {
  log "ERROR: Command failed: jshon -e version"
  exit 1
}

# Version must be semver compliant
echo "$VERSION" | grep -q -P "^\d+\.\d+\.\d+" || {
  log "ERROR: VERSION doesn't seem to follow semver"
  exit 1
}

RELEASE="$(echo "$VERSION" | awk -F"-" '{print $1}')"
[[ -n "$(echo "$VERSION" | awk -F"-" '{print $2}')" ]] && VERSION="${RELEASE}-SNAPSHOT"

# Exit if pom already present with correct version
if [[ -f pom.xml ]]; then
  POM_VERSION="$(kurento_get_version.sh)" || {
    log "ERROR: Command failed: kurento_get_version"
    exit 1
  }
  [[ "$VERSION" == "$POM_VERSION" ]] && {
    log "Exit: Valid pom.xml already exists"
    exit 0
  }
fi

# Add pom file
cat >pom.xml <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Maven coordinates -->
  <groupId>org.kurento</groupId>
  <artifactId>${PROJECT_NAME}</artifactId>
  <version>${VERSION}</version>
  <packaging>jar</packaging>

  <!-- Project-level information -->
  <name>${PROJECT_NAME}</name>
  <description>
    Kurento Media Server, JavaScript client code for module ${PROJECT_NAME}.
  </description>
  <url>https://www.kurento.org/docs/\${project.version}</url>
  <scm>
    <url>https://github.com/Kurento/${PROJECT_NAME}</url>
    <connection>scm:git:git://github.com/Kurento/${PROJECT_NAME}.git</connection>
    <developerConnection>scm:git:git@github.com:Kurento/${PROJECT_NAME}.git</developerConnection>
  </scm>

  <!-- Organization-level information -->
  <developers>
    <developer>
      <id>kurento.org</id>
      <name>Kurento Community</name>
      <organization>Kurento</organization>
      <organizationUrl>https://www.kurento.org</organizationUrl>
    </developer>
  </developers>
  <issueManagement>
    <system>GitHub</system>
    <url>https://github.com/Kurento/bugtracker/issues</url>
  </issueManagement>
  <licenses>
    <license>
      <name>Apache 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <mailingLists>
    <mailingList>
      <name>Kurento List</name>
      <subscribe>http://groups.google.com/group/kurento/subscribe</subscribe>
      <post>http://groups.google.com/group/kurento/post</post>
      <archive>http://groups.google.com/group/kurento/about</archive>
    </mailingList>
  </mailingLists>
  <organization>
    <name>Kurento</name>
    <url>https://www.kurento.org</url>
  </organization>

  <!-- Project configuration -->
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>1.6.0</version>
        <executions>
          <!-- Explicit CLI calls (e.g. from kurento_get_version.sh to get
          the project version) are defined separately so maven_script.sh
          doesn't get called. -->
          <execution>
            <id>default-cli</id>
            <phase/>
          </execution>
          <!-- Use the "generate-resources" phase to call maven_script.sh
          that generates all JS files and puts them into resources/ -->
          <execution>
            <id>default-package</id>
            <phase>generate-resources</phase>
            <goals>
              <goal>exec</goal>
            </goals>
            <configuration>
              <executable>maven_script.sh</executable>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <!-- Generates a ZIP file for distribution (currently unused?) -->
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-assembly-plugin</artifactId>
        <configuration>
          <descriptors>
            <descriptor>
              \${basedir}/${ASSEMBLY_FILE}
            </descriptor>
          </descriptors>
          <outputDirectory>
            \${project.build.directory}
          </outputDirectory>
          <appendAssemblyId>false</appendAssemblyId>
        </configuration>
        <executions>
          <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
    <extensions>
      <extension>
        <groupId>org.kuali.maven.wagons</groupId>
        <artifactId>maven-s3-wagon</artifactId>
        <version>1.2.1</version>
      </extension>
    </extensions>
  </build>
  <profiles>
    <!-- Kurento CI requires these profiles to exist -->
    <profile>
      <id>default</id>
    </profile>
    <profile>
      <id>deploy</id>
    </profile>
    <profile>
      <id>kurento-release</id>
    </profile>
  </profiles>
</project>
EOF

# If there's an assembly file elsewhere, stop and use the file specified. It should be placed on the root of the workspace
[[ -f "$ASSEMBLY_FILE" ]] && {
  log "Exit: Assembly file already exists: $ASSEMBLY_FILE"
  exit 0
}

# Add assembly file
cat >"$ASSEMBLY_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2 http://maven.apache.org/xsd/assembly-1.1.2.xsd">
  <id>dist</id>
  <formats>
    <format>zip</format>
  </formats>
  <includeBaseDirectory>false</includeBaseDirectory>
  <fileSets>
    <fileSet>
      <directory>\${project.basedir}</directory>
      <outputDirectory>/</outputDirectory>
      <includes>
        <include>README.*</include>
        <include>LICENSE*</include>
        <include>NOTICE*</include>
      </includes>
    </fileSet>
    <fileSet>
      <directory>\${project.basedir}/dist</directory>
      <outputDirectory>/js</outputDirectory>
      <includes>
        <include>*.js</include>
        <include>*.map</include>
      </includes>
    </fileSet>
  </fileSets>
</assembly>
EOF



log "==================== END ===================="
