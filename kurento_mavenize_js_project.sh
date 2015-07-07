#!/bin/bash -x

echo "##################### EXECUTE: mavenice-js-module #####################"

# Verify parameters
if [ -n "$1" ]; then
	PROJECT_NAME=$1
else
	echo "Usage: $0 <project_name>"
	exit 1
fi

[ -n "$2" ] && MAVEN_SHELL_SCRIPT=$2 || MAVEN_SHELL_SCRIPT="cd \${basedir} ; npm -d install || exit 1 ; node_modules/.bin/grunt || exit 1 ; node_modules/.bin/grunt sync:bower || exit 1 ; mkdir -p src/main/resources/META-INF/resources/js/ || exit 1 ; cp dist/* src/main/resources/META-INF/resources/js/"

[ -n "$3" ] && ASSEMBLY_FILE=$3 || ASSEMBLY_FILE="assembly.xml"

[ -f package.json ] || exit 1

# Build maven version from package.json
VERSION=$(jshon -e version -u < package.json)

# Version must be semver compliant
echo $VERSION | grep -q -P "^\d+\.\d+\.\d+" || exit 1
RELEASE=$(echo $VERSION | awk -F"-" '{print $1}')
[ -n "$(echo $VERSION | awk -F"-" '{print $2}')" ] && VERSION=$RELEASE-SNAPSHOT

# Exit silently if pom already present with correct version
[ -f pom.xml ] && [ $VERSION == `mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"` ] && exit 0

# Add pom file
cat > pom.xml <<-EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>org.kurento</groupId>
	<artifactId>${PROJECT_NAME}</artifactId>
	<version>${VERSION}</version>
	<packaging>jar</packaging>

	<name>${PROJECT_NAME}</name>
	<description>
		Kurento Media Server module ${PROJECT_NAME}
		Javascript control client code
	</description>
	<url>http://www.kurento.org</url>

	<licenses>
		<license>
			<name>GNU Lesser General Public License</name>
			<url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
			<distribution>repo</distribution>
		</license>
	</licenses>

	<organization>
		<name>Kurento</name>
		<url>http://www.kurento.org</url>
	</organization>

	<scm>
	   <url>https://github.com/Kurento/${PROJECT_NAME}</url>
   <connection>
      scm:git:https://github.com/Kurento/${PROJECT_NAME}.git
   </connection>
   <developerConnection>
      scm:git:ssh://git@github.com:Kurento/${PROJECT_NAME}.git
   </developerConnection>
	</scm>
	<developers>
		<developer>
			<id>kurento.org</id>
			<name>-kurento.org Community</name>
			<organization>Kurento.org</organization>
			<organizationUrl>http://www.kurento.org</organizationUrl>
		</developer>
	</developers>
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
	</build>
</project>
EOF

# If there's an assembly file elsewhere, stop and use the file specified. It should be placed on the root of the workspace
[ -n $ASSEMBLY_FILE ] && exit 0

# Add assembly file
cat > assembly.xml <<-EOF
<?xml version="1.0" encoding="UTF-8"?>
<assembly
	xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.2
      http://maven.apache.org/xsd/assembly-1.1.2.xsd">
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
