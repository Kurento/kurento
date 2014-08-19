package org.kurento.modulecreator.json;

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

import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.Param;
import org.kurento.modulecreator.definition.Property;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.TypeRef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

	public ModuleDefinition loadFromFile(Path file)
			throws FileNotFoundException, IOException {
		return loadFromInputStream(Files.newInputStream(file));
	}

	public ModuleDefinition loadFromClasspath(String resourceName)
			throws IOException {
		return loadFromInputStream(this.getClass().getResourceAsStream(
				resourceName));
	}

	private ModuleDefinition loadFromInputStream(InputStream is)
			throws IOException {
		String moduleString = loadTextFromInputStream(is);
		ModuleDefinition module = gson.fromJson(moduleString,
				ModuleDefinition.class);
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

	public void writeToFile(ModuleDefinition module, File file)
			throws FileNotFoundException {

		String jsonModuleString = gson.toJson(module);

		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		PrintWriter writer = new PrintWriter(file);
		writer.println(jsonModuleString);
		writer.close();
	}

}
