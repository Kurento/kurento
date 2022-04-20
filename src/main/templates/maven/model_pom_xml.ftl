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
		<!-- FIXME: Starting from Java-9, change to use the new <release> parameter. -->
		<maven.compiler.target>1.8</maven.compiler.target>
		<maven.compiler.source>1.8</maven.compiler.source>
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
<#else>
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
				<version>6.18.0-SNAPSHOT</version>
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
