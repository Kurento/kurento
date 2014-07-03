package com.kurento.ktool.rom.processor.codegen;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.jar.JarFile;

public abstract class KmsSource {

	public static class File extends KmsSource {

		private java.io.File file;

		public File(java.io.File file) {
			this.file = file;
		}

		@Override
		public Reader openReader() throws IOException {
			return new BufferedReader(new InputStreamReader(
					new FileInputStream(file)));
		}
	}

	public static class JarEntry extends KmsSource {

		private java.util.jar.JarEntry entry;
		private JarFile jarFile;

		public JarEntry(JarFile jarFile, java.util.jar.JarEntry entry) {
			this.entry = entry;
			this.jarFile = jarFile;
		}

		@Override
		public Reader openReader() throws IOException {
			return new BufferedReader(new InputStreamReader(
					jarFile.getInputStream(entry)));
		}
	}

	public static KmsSource fromFile(java.io.File file) {
		return new KmsSource.File(file);
	}

	public static KmsSource fromJarEntry(JarFile jarFile,
			java.util.jar.JarEntry entry) {
		return new KmsSource.JarEntry(jarFile, entry);
	}

	public abstract Reader openReader() throws IOException;

}
