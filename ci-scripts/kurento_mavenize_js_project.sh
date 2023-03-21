#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Convert a JavaScript package into a Java deployable artifact.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Verify project
# ==============

[[ -f package.json ]] || {
    log "ERROR: File not found: package.json"
    exit 1
}



# Prepare script called from pom.xml
tee maven_script.sh >/dev/null <<EOF
#!/usr/bin/env bash
# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset
# Trace all commands
set -o xtrace
echo "==== Run maven_script.sh ===="
npm install --no-color --loglevel=info --audit=false --fund=false || { echo ERR1; exit 1; }
node_modules/.bin/grunt --no-color || { echo ERR2; exit 2; }
node_modules/.bin/grunt --no-color sync:bower || { echo ERR3; exit 3; }
mkdir -p src/main/resources/META-INF/resources/js/ || { echo ERR4; exit 4; }
cp dist/* src/main/resources/META-INF/resources/js/
EOF
chmod +x maven_script.sh



# Build Maven version string
# ==========================

PROJECT_VERSION="$(jq --raw-output '.version' package.json)"
PROJECT_RELEASE="$(echo "$PROJECT_VERSION" | awk -F '-' '{print $1}')"
MAVEN_VERSION="$PROJECT_RELEASE"

if [[ "$PROJECT_VERSION" != "$PROJECT_RELEASE" ]]; then
    MAVEN_VERSION+="-SNAPSHOT"
fi



# Verify versions
# ===============

# Exit if pom already present with correct version.
if [[ -f pom.xml ]]; then
    POM_VERSION="$(kurento_get_version.sh)"
    if [[ "$POM_VERSION" == "$MAVEN_VERSION" ]]; then
        log "Exit: Valid pom.xml already exists"
        exit 0
    fi
fi



# Mavenize
# ========

PROJECT_NAME="$(kurento_get_name.sh)-js" || {
    echo "ERROR: Command failed: kurento_get_name"
    exit 1
}

ASSEMBLY_FILE="assembly.xml"

tee pom.xml >/dev/null <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Maven coordinates -->
  <groupId>org.kurento</groupId>
  <artifactId>${PROJECT_NAME}</artifactId>
  <version>${MAVEN_VERSION}</version>
  <packaging>jar</packaging>

  <!-- Project-level information -->
  <name>${PROJECT_NAME}</name>
  <description>
    Kurento Media Server JavaScript client code for ${PROJECT_NAME}.
  </description>
  <url>https://kurento.openvidu.io/docs/\${project.version}</url>
  <scm>
    <url>https://github.com/Kurento/${PROJECT_NAME}</url>
    <connection>scm:git:https://github.com/Kurento/${PROJECT_NAME}.git</connection>
    <developerConnection>scm:git:ssh://github.com/Kurento/${PROJECT_NAME}.git</developerConnection>
  </scm>

  <!-- Organization-level information -->
  <developers>
    <developer>
      <id>kurento.org</id>
      <name>Kurento Community</name>
      <organization>Kurento</organization>
      <organizationUrl>https://kurento.openvidu.io/</organizationUrl>
    </developer>
  </developers>
  <issueManagement>
    <system>GitHub</system>
    <url>https://github.com/Kurento/kurento/issues</url>
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
    <url>https://kurento.openvidu.io/</url>
  </organization>

  <!-- Project configuration -->
  <properties>
    <!-- maven-resources-plugin -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- maven-compiler-plugin -->
    <maven.compiler.release>11</maven.compiler.release>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.0.0</version>
        <executions>
          <!-- Explicit CLI calls to the exec:exec goal are defined separately,
          so maven_script.sh doesn't get called. -->
          <execution>
            <id>default-cli</id>
            <phase/>
          </execution>
          <!-- Use the "generate-resources" phase to call maven_script.sh
          that generates all JS files and puts them into resources/ -->
          <execution>
            <id>maven-script</id>
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

# If there's an assembly file, stop and use the file specified.
if [[ -f "$ASSEMBLY_FILE" ]]; then
    log "Exit: Assembly file already exists: $ASSEMBLY_FILE"
    exit 0
fi

# Add assembly file.
tee "$ASSEMBLY_FILE" >/dev/null <<EOF
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
