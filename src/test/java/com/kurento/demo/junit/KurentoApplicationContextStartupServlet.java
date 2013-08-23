package com.kurento.demo.junit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import com.kurento.kmf.spring.KurentoApplicationContextUtils;

@WebServlet(loadOnStartup = 1, urlPatterns = { "/testit" })
public class KurentoApplicationContextStartupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		super.init();
		KurentoApplicationContextUtils.createKurentoApplicationContext(this
				.getServletContext());
	}
}
