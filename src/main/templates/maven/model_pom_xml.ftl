pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

<#if model.code.kmd?? >
	<groupId>${model.code.kmd.java["maven.groupId"]}</groupId>
	<artifactId>${model.code.kmd.java["maven.artifactId"]}</artifactId>
	<version>${model.code.kmd.java["maven.version"]}</version>
<#else>
	<groupId>${model.code.api.java["maven.groupId"]}</groupId>
	<artifactId>${model.code.api.java["maven.artifactId"]}</artifactId>
	<version>${model.code.api.java["maven.version"]}</version>
</#if>
	<packaging>jar</packaging>

	<name>${model.name}</name>
	<url>http://maven.apache.org</url>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<maven.compiler.target>1.7</maven.compiler.target>
		<maven.compiler.source>1.7</maven.compiler.source>
	</properties>

	<dependencies>
<#list model.imports as import>
		<dependency>
<#if model.code.kmd?? >
			<groupId>${import.model.code.kmd.java["maven.groupId"]}</groupId>
			<artifactId>${import.model.code.kmd.java["maven.artifactId"]}</artifactId>
			<version>${import.model.code.kmd.java["maven.version"]}</version>
<#else>
			<groupId>${import.model.code.api.java["maven.groupId"]}</groupId>
			<artifactId>${import.model.code.api.java["maven.artifactId"]}</artifactId>
			<version>${import.model.code.api.java["maven.version"]}</version>
</#if>
		</dependency>
</#list>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>com.kurento</groupId>
				<artifactId>kurento-maven-plugin</artifactId>
				<version>0.0.19-SNAPSHOT</version>
				<executions>
					<execution>
						<goals>
							<goal>generate-java-media-api</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
