pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

<#if module.code.kmd?? >
	<groupId>${module.code.kmd.java.mavenGroupId}</groupId>
	<artifactId>${module.code.kmd.java.mavenArtifactId}</artifactId>
	<version>${module.code.kmd.java.mavenVersion}</version>
<#else>
	<groupId>${module.code.api.java.mavenGroupId}</groupId>
	<artifactId>${module.code.api.java.mavenArtifactId}</artifactId>
	<version>${module.code.api.java.mavenVersion}</version>
</#if>
	<packaging>jar</packaging>

	<name>${module.name}</name>
	<url>http://maven.apache.org</url>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<maven.compiler.target>1.7</maven.compiler.target>
		<maven.compiler.source>1.7</maven.compiler.source>
	</properties>

<#if module.imports[0]??>
	<dependencies>
<#list module.imports as import>
<#if module.code.kmd?? >
		<dependency>
			<groupId>${import.module.code.kmd.java.mavenGroupId}</groupId>
			<artifactId>${import.module.code.kmd.java.mavenArtifactId}</artifactId>
			<version>${import.mavenVersion}</version>
		</dependency>
<#elseif import.name != "elements" && import.name != "filters">
		<dependency>
			<groupId>${import.module.code.api.java.mavenGroupId}</groupId>
			<artifactId>${import.module.code.api.java.mavenArtifactId}</artifactId>
			<version>${import.mavenVersion}</version>
		</dependency>
</#if>
</#list>
	</dependencies>
</#if>

	<build>
		<plugins>
			<plugin>
				<groupId>org.kurento</groupId>
				<artifactId>kurento-maven-plugin</artifactId>
				<version>1.0.5</version>
				<executions>
					<execution>
						<goals>
							<goal>generate-kurento-client</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
