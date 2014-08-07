pom.xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

<#if module.code.kmd?? >
	<groupId>${module.code.kmd.java["maven.groupId"]}</groupId>
	<artifactId>${module.code.kmd.java["maven.artifactId"]}</artifactId>
	<version>${module.code.kmd.java["maven.version"]}</version>
<#else>
	<groupId>${module.code.api.java["maven.groupId"]}</groupId>
	<artifactId>${module.code.api.java["maven.artifactId"]}</artifactId>
	<version>${module.code.api.java["maven.version"]}</version>
</#if>
	<packaging>jar</packaging>

	<name>${module.name}</name>
	<url>http://maven.apache.org</url>

	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<maven.compiler.target>1.7</maven.compiler.target>
		<maven.compiler.source>1.7</maven.compiler.source>
	</properties>

	<dependencies>
<#list module.imports as import>
		<dependency>
<#if module.code.kmd?? >
			<groupId>${import.module.code.kmd.java["maven.groupId"]}</groupId>
			<artifactId>${import.module.code.kmd.java["maven.artifactId"]}</artifactId>
			<version>${import.module.code.kmd.java["maven.version"]}</version>
<#else>
			<groupId>${import.module.code.api.java["maven.groupId"]}</groupId>
			<artifactId>${import.module.code.api.java["maven.artifactId"]}</artifactId>
			<version>${import.module.code.api.java["maven.version"]}</version>
</#if>
		</dependency>
</#list>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.kurento</groupId>
				<artifactId>kurento-maven-plugin</artifactId>
				<version>0.0.21-SNAPSHOT</version>
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
