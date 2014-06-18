package com.kurento.ktool.rom.processor.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kurento.ktool.rom.processor.model.Model;
import com.kurento.ktool.rom.processor.model.Param;
import com.kurento.ktool.rom.processor.model.Property;
import com.kurento.ktool.rom.processor.model.RemoteClass;
import com.kurento.ktool.rom.processor.model.TypeRef;

public class JsonModelSaverLoader {

	private static JsonModelSaverLoader INSTANCE = new JsonModelSaverLoader();

	public static JsonModelSaverLoader getInstance() {
		return INSTANCE;
	}

	private Gson gson;

	private JsonModelSaverLoader() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
		gsonBuilder.registerTypeAdapter(TypeRef.class, new TypeRefAdapter());
		gsonBuilder.registerTypeAdapter(Param.class, new DataItemAdapter());
		gsonBuilder.registerTypeAdapter(Property.class, new DataItemAdapter());
		gsonBuilder.registerTypeAdapter(RemoteClass.class,
				new RemoteClassAdapter());
		gsonBuilder.registerTypeAdapter(Method.class, new MethodAdapter());
		gson = gsonBuilder.create();
	}

	public Model loadFromFile(File file) throws FileNotFoundException,
			IOException {
		return loadFromInputStream(new FileInputStream(file));
	}

	public Model loadFromClasspath(String resourceName) throws IOException {
		return loadFromInputStream(this.getClass().getResourceAsStream(
				resourceName));
	}

	private Model loadFromInputStream(InputStream is) throws IOException {
		String modelString = loadTextFromInputStream(is);
		Model model = gson.fromJson(modelString, Model.class);
		return model;
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

	public void writeToFile(Model model, File file)
			throws FileNotFoundException {

		String jsonModelString = gson.toJson(model);

		PrintWriter writer = new PrintWriter(file);
		writer.println(jsonModelString);
		writer.close();
	}

}
