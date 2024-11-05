pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<!-- Maven coordinates -->
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

	<!-- Project-level information -->
	<name>${module.name}</name>
	<description></description>
	<url>https://maven.apache.org</url>

	<!-- Project configuration -->

	<properties>
		<!-- maven-resources-plugin -->
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

		<!-- maven-compiler-plugin -->
		<maven.compiler.release>11</maven.compiler.release>
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
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.11.0</version>
			</plugin>
			<plugin>
				<groupId>org.kurento</groupId>
				<artifactId>kurento-maven-plugin</artifactId>
				<version>7.1.0</version>
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

	<!--
	Kurento projects don't define a <distributionManagement> section with the
	repositories used for deployment. Instead, CI injects a `settings.xml` file
	with a "deploy" profile that configures maven-deploy-plugin through properties
	`altSnapshotDeploymentRepository` and `altReleaseDeploymentRepository`.
	Refer to Jenkins Managed File "Kurento GitHub Maven settings.xml".
	<distributionManagement></distributionManagement>
	-->

</project>
