package com.kurento.kms.idl.codegen;

import java.io.File;
import java.io.IOException;

import com.kurento.kms.idl.json.JsonModel;
import com.kurento.kms.idl.model.Model;

import freemarker.template.TemplateException;

public class Main {

	public static void main(String[] args) throws IOException,
			TemplateException {

		Model model = new JsonModel().loadFromFile(new File("model.json"));

		CodeGen codeGen = new CodeGen(new File("templates"), new File("codegen"));
		
		codeGen.generateCode(model);
		
		System.out.println("Generación de código finalizada");
		
	}

}
