package org.kurento.modulecreator.codegen;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class JsonFusioner {

	private static final Gson gson = new GsonBuilder().setPrettyPrinting()
			.create();

	private Path generatedXml;
	private Path customizerXml;
	private Path outputFile;

	private Set<String> addChildrenTags;
	private Set<String> replaceChildrenTags;

	public JsonFusioner(Path generatedXml, Path customizerXml, Path outputFile) {
		this(generatedXml, customizerXml, outputFile, null, null);
	}

	public JsonFusioner(Path generatedXml, Path customizerXml, Path outputFile,
			String[] addChildrenTags, String[] replaceChildrenTags) {
		super();
		this.generatedXml = generatedXml;
		this.customizerXml = customizerXml;
		this.outputFile = outputFile;
		this.addChildrenTags = new HashSet<String>(
				Arrays.asList(addChildrenTags));
		this.replaceChildrenTags = new HashSet<String>(
				Arrays.asList(replaceChildrenTags));
	}

	public void fusionJsons() throws ParserConfigurationException,
			SAXException, IOException, TransformerException {

		JsonObject generatedXmlDoc = loadJson(generatedXml);
		JsonObject customizedXmlDoc = loadJson(customizerXml);

		merge(generatedXmlDoc, customizedXmlDoc, new ArrayList<String>());

		writeJson(generatedXmlDoc);
	}

	private void merge(JsonObject genNode, JsonObject custNode,
			List<String> genPath) {

		for (Entry<String, JsonElement> entry : custNode.entrySet()) {

			JsonElement custChildNode = entry.getValue();

			JsonElement genChildNode = genNode.get(entry.getKey());

			if (genChildNode != null) {

				String nodePath = getPath(genPath, entry.getKey());

				if (replaceChildrenTags.contains(nodePath)) {

					if (custChildNode instanceof JsonObject
							&& genChildNode instanceof JsonObject) {

						List<String> newPath = new ArrayList<String>(genPath);
						newPath.add(entry.getKey());

						merge((JsonObject) genChildNode,
								(JsonObject) custChildNode, newPath);
					}

				} else if (addChildrenTags.contains(nodePath)) {

					addChildren(custChildNode, genChildNode);

				} else if (includedInReplaceOrAdd(nodePath)) {

					if (custChildNode instanceof JsonObject
							&& genChildNode instanceof JsonObject) {

						List<String> newPath = new ArrayList<String>(genPath);
						newPath.add(entry.getKey());

						merge((JsonObject) genChildNode,
								(JsonObject) custChildNode, newPath);
					}

				} else {

					// Replace entire node
					genNode.add(entry.getKey(), custChildNode);
				}

			} else {

				// Add new node
				genNode.add(entry.getKey(), custChildNode);
			}
		}
	}

	private void addChildren(JsonElement fromElement, JsonElement toElement) {

		if (fromElement instanceof JsonObject
				&& toElement instanceof JsonObject) {

			JsonObject fromObject = (JsonObject) fromElement;
			JsonObject toObject = (JsonObject) toElement;

			for (Entry<String, JsonElement> entry : fromObject.entrySet()) {
				toObject.add(entry.getKey(), entry.getValue());
			}

		} else if (fromElement instanceof JsonArray
				&& toElement instanceof JsonArray) {

			JsonArray fromArray = (JsonArray) fromElement;
			JsonArray toArray = (JsonArray) toElement;

			toArray.addAll(fromArray);
		}
	}

	private boolean includedInReplaceOrAdd(String nodePath) {

		for (String path : replaceChildrenTags) {
			if (path.startsWith(nodePath)) {
				return true;
			}
		}

		for (String path : addChildrenTags) {
			if (path.startsWith(nodePath)) {
				return true;
			}
		}

		return false;
	}

	private String getPath(List<String> path, String propName) {

		StringBuilder sb = new StringBuilder("/");
		for (String prop : path) {
			sb.append(prop).append("/");
		}
		sb.append(propName);
		return sb.toString();
	}

	private JsonObject loadJson(Path jsonPath) throws JsonSyntaxException,
			JsonIOException, IOException {

		return (JsonObject) gson.fromJson(
				Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8),
				JsonElement.class);
	}

	private void writeJson(JsonObject doc) throws IOException {

		String json = gson.toJson(doc);
		try (OutputStream os = Files.newOutputStream(outputFile)) {
			os.write(json.getBytes(StandardCharsets.UTF_8));
		}
	}

}
