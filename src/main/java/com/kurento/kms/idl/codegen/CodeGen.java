package com.kurento.kms.idl.codegen;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.kurento.kms.idl.model.Model;
import com.kurento.kms.idl.model.Type;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

public class CodeGen {

	private final File templatesFolder;
	private final File outputFolder;
	private final Configuration cfg;

	public CodeGen(File templatesFolder, File outputFolder) throws IOException {

		this.templatesFolder = templatesFolder;
		this.outputFolder = outputFolder;

		cfg = new Configuration();

		// Specify the data source where the template files come from. Here I
		// set a
		// plain directory for it, but non-file-system are possible too:
		cfg.setDirectoryForTemplateLoading(templatesFolder);

		// Specify how templates will see the data-model. This is an advanced
		// topic...
		// for now just use this:
		cfg.setObjectWrapper(new DefaultObjectWrapper());

		// Set your preferred charset template files are stored in. UTF-8 is
		// a good choice in most applications:
		cfg.setDefaultEncoding("UTF-8");

		// Sets how errors will appear. Here we assume we are developing HTML
		// pages.
		// For production systems TemplateExceptionHandler.RETHROW_HANDLER is
		// better.
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

		// At least in new projects, specify that you want the fixes that aren't
		// 100% backward compatible too (these are very low-risk changes as far
		// as the
		// 1st and 2nd version number remains):
		cfg.setIncompatibleImprovements(new Version(2, 3, 20)); // FreeMarker
																// 2.3.20

	}

	public void generateCode(Model model) throws IOException, TemplateException {

		File[] files = templatesFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".ftl");
			}
		});

		for (File file : files) {

			String name = file.getName();
			name = name.substring(0, name.length() - 4);
			String modelType = name.split("_")[0];

			generateCode(file.getName(), model, modelType);
		}
	}

	private void generateCode(String templateName, Model model, String modelType)
			throws TemplateException, IOException {

		Template temp = cfg.getTemplate(templateName);

		List<? extends Type> types;
		if (modelType.equals("remoteClass")) {
			types = model.getRemoteClasses();
		} else if (modelType.equals("complexType")) {
			types = model.getComplexTypes();
		} else if (modelType.equals("event")) {
			types = model.getEvents();
		} else if (modelType.equals("model")) {
			types = null;
		} else {
			throw new RuntimeException(
					"Unknown model element: '"
							+ modelType
							+ "'. It should be 'remoteClass' or 'complexType' or 'event'");
		}

		Map<String, Object> root = new HashMap<String, Object>();
		root.put("getJavaObjectType", new JavaObjectType());
		root.put("getCppObjectType", new CppObjectType());
		root.put("camelToUnderscore", new CamelToUnderscore());

		root.put("model", model);

		if (types == null) {
			generateFile(temp, root);
		} else {

			for (Type type : types) {

				if (modelType.equals("remoteClass")) {
					root.put("remoteClass", type);
				} else if (modelType.equals("complexType")) {
					root.put("complexType", type);
				} else if (modelType.equals("event")) {
					root.put("event", type);
				}

				generateFile(temp, root);

			}
		}
	}

	private void generateFile(Template temp, Map<String, Object> root)
			throws TemplateException, IOException {

		StringWriter out = new StringWriter();
		temp.process(root, out);
		String tempOutput = out.toString();

		StringTokenizer st = new StringTokenizer(tempOutput);

		String fileName = st.nextToken();

		File outputFile = new File(outputFolder, fileName);

		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
		}

		Writer writer = new FileWriter(outputFile);
		String sourceCode = tempOutput.substring(fileName.length() + 1,
				tempOutput.length());
		writer.write(sourceCode);
		writer.close();

		System.out.println("File: " + fileName);
		System.out.println();
		System.out.println(sourceCode);
		System.out.println("---------------------------------------");
	}

}
