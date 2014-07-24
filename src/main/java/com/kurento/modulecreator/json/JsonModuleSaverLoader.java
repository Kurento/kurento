package com.kurento.modulecreator.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kurento.modulecreator.descriptor.ModuleDescriptor;
import com.kurento.modulecreator.descriptor.Param;
import com.kurento.modulecreator.descriptor.Property;
import com.kurento.modulecreator.descriptor.RemoteClass;
import com.kurento.modulecreator.descriptor.TypeRef;

public class JsonModuleSaverLoader {

	private static JsonModuleSaverLoader INSTANCE = new JsonModuleSaverLoader();

	public static JsonModuleSaverLoader getInstance() {
		return INSTANCE;
	}

	private Gson gson;

	private JsonModuleSaverLoader() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
		gsonBuilder.registerTypeAdapter(TypeRef.class, new TypeRefAdapter());
		gsonBuilder.registerTypeAdapter(Param.class, new DataItemAdapter());
		gsonBuilder.registerTypeAdapter(Property.class, new DataItemAdapter());
		gsonBuilder.registerTypeAdapter(RemoteClass.class,
				new RemoteClassAdapter());
		gsonBuilder.registerTypeAdapter(Method.class, new MethodAdapter());
		gson = gsonBuilder.create();
	}

	public ModuleDescriptor loadFromFile(Path file) throws FileNotFoundException,
			IOException {
		return loadFromInputStream(Files.newInputStream(file));
	}

	public ModuleDescriptor loadFromClasspath(String resourceName) throws IOException {
		return loadFromInputStream(this.getClass().getResourceAsStream(
				resourceName));
	}

	private ModuleDescriptor loadFromInputStream(InputStream is) throws IOException {
		String moduleString = loadTextFromInputStream(is);
		ModuleDescriptor module = gson.fromJson(moduleString, ModuleDescriptor.class);
		return module;
	}

	private String loadTextFromInputStream(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}

		return sb.toString();
	}

	public void writeToFile(ModuleDescriptor module, File file)
			throws FileNotFoundException {

		String jsonModuleString = gson.toJson(module);

		PrintWriter writer = new PrintWriter(file);
		writer.println(jsonModuleString);
		writer.close();
	}

}
