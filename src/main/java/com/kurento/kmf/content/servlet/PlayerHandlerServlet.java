package com.kurento.kmf.content.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kurento.kmf.content.PlayerHandler;

public abstract class PlayerHandlerServlet extends HttpServlet implements
		PlayerHandler {

	private static final long serialVersionUID = 1L;

	@Override
	protected final void doDelete(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		super.doDelete(req, resp);
	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doGet(req, resp);
	}

	@Override
	protected final void doHead(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doHead(req, resp);
	}

	@Override
	protected final void doOptions(HttpServletRequest arg0,
			HttpServletResponse arg1) throws ServletException, IOException {
		super.doOptions(arg0, arg1);
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doPost(req, resp);
	}

	@Override
	protected final void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doPut(req, resp);
	}

	@Override
	protected final void doTrace(HttpServletRequest arg0,
			HttpServletResponse arg1) throws ServletException, IOException {
		super.doTrace(arg0, arg1);
	}

	@Override
	protected final long getLastModified(HttpServletRequest req) {
		return super.getLastModified(req);
	}

	@Override
	protected final void service(HttpServletRequest arg0,
			HttpServletResponse arg1) throws ServletException, IOException {
		super.service(arg0, arg1);
	}

	@Override
	public final void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		super.service(req, res);
	}

}
