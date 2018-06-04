#!/bin/bash -x

echo "##################### EXECUTE: kurento_mavenice_js_project #####################"

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
[ -n "$1" ] && PROJECT_NAME=$1
[ -n "$2" ] && MAVEN_SHELL_SCRIPT=$2
[ -n "$3" ] && ASSEMBLY_FILE=$3

# Validate parameters
[ -z "$PROJECT_NAME" ] && {
  echo "[kurento_mavenize_js_project] ERROR: Undefined variable: PROJECT_NAME"
  exit 1
}
[ -z "$MAVEN_SHELL_SCRIPT" ] && MAVEN_SHELL_SCRIPT="\
  npm install npm -g || exit 1; \
  cd \${basedir}; \
  npm -d install || exit 1; \
  node_modules/.bin/grunt || exit 1; \
  node_modules/.bin/grunt sync:bower || exit 1; \
  mkdir -p src/main/resources/META-INF/resources/js/ || exit 1; \
  cp dist/* src/main/resources/META-INF/resources/js/"
[ -z "$ASSEMBLY_FILE" ] && ASSEMBLY_FILE="assembly.xml"

# Validate project structure
[ -f package.json ] || {
  echo "[kurento_mavenize_js_project] ERROR: Cannot read file: package.json"
  exit 1
}

# Build maven version from package.json
VERSION="$(jshon -e version -u < package.json)" || {
  echo "[kurento_mavenize_js_project] ERROR: Command failed: jshon -e version"
  exit 1
}

# Version must be semver compliant
echo "$VERSION" | grep -q -P "^\d+\.\d+\.\d+" || {
  echo "[kurento_mavenize_js_project] ERROR: VERSION doesn't seem to follow semver"
  exit 1
}

RELEASE="$(echo $VERSION | awk -F"-" '{print $1}')"
[ -n "$(echo $VERSION | awk -F"-" '{print $2}')" ] && VERSION="${RELEASE}-SNAPSHOT"

# Exit if pom already present with correct version
if [ -f pom.xml ]; then
  POM_VERSION="$(mvn --batch-mode --non-recursive help:evaluate -Dexpression=project.version 2>/dev/null | grep -v '^\[.*\]')"
  [ "$VERSION" == "$POM_VERSION" ] && {
    echo "[kurento_mavenize_js_project] Exit: pom.xml already exists"
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
        <artifactId>shell-maven-plugin</artifactId>
        <version>1.0-beta-1</version>
        <executions>
          <execution>
            <id>stage-sources</id>
            <phase>process-sources</phase>
            <goals>
              <goal>shell</goal>
            </goals>
            <configuration>
              <workDir>\${workDir}</workDir>
              <chmod>true</chmod>
              <keepScriptFile>true</keepScriptFile>
              <script>
                ${MAVEN_SHELL_SCRIPT}
              </script>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
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
    <!-- Kurento CI requires this profiles to exist -->
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
[ -n $ASSEMBLY_FILE ] && {
  echo "[kurento_mavenize_js_project] Exit: Assembly file already exists: $ASSEMBLY_FILE"
  exit 0
}

# Add assembly file
cat >$ASSEMBLY_FILE <<-EOF
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
			<includes>
				<include>README.md</include>
				<include>LICENSE</include>
			</includes>
		</fileSet>
		<fileSet>
			<directory>dist</directory>
			<outputDirectory>/js</outputDirectory>
			<includes>
				<include>*.js</include>
				<include>*.map</include>
			</includes>
		</fileSet>
	</fileSets>
</assembly>
EOF
