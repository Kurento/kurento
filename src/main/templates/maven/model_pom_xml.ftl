pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<!-- Maven coordinates -->
	<parent>
		<groupId>org.kurento</groupId>
		<artifactId>kurento-parent-pom</artifactId>
		<version>7.0.0-SNAPSHOT</version>
	</parent>
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
				<version>7.0.0-SNAPSHOT</version>
				<executions>
					<execution>
						<goals>
							<goal>generate-kurento-client</goal>
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
		<profile>
			<id>deploy</id>
			<build>
				<plugins>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-surefire-plugin</artifactId>
						<configuration>
							<skipTests>true</skipTests>
						</configuration>
					</plugin>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-failsafe-plugin</artifactId>
						<configuration>
							<skipTests>true</skipTests>
						</configuration>
					</plugin>
				</plugins>
			</build>
		</profile>
	</profiles>

</project>
