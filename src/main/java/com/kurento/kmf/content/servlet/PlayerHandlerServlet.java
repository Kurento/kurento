package com.kurento.kmf.content.servlet;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;

@WebServlet(asyncSupported = true)
public class PlayerHandlerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory
			.getLogger(PlayerHandlerServlet.class);

	@Autowired
	private PlayerHandler playerHandler;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(
					this, this.getServletContext());
		} catch (IllegalStateException e) {
			throw new ServletException(
					"Application did not load the Spring ServletApplicationContext",
					e);
		}
	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		log.debug("Request received: " + req.getRequestURI());

		if (!req.isAsyncSupported()) {
			// Async context could not be created. It is not necessary to
			// complete it. Just send error message to
			resp.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"AsyncContext could not be started. The application should add \"asyncSupported = true\" in all "
							+ this.getClass().getName()
							+ " instances and in all filters in the associated chain");
			return;
		}
		if (playerHandler == null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Application must implement a PlayerHandler Spring bean");
			return;
		}

		String contentId = req.getPathInfo();
		if (contentId != null) {
			contentId = contentId.substring(1);
		}

		AsyncContext asyncCtx = req.startAsync();

		// Add listener for managing error conditions
		asyncCtx.addListener(new ContentAsyncListener());

		PlayRequest playRequest = new PlayRequest(asyncCtx, contentId);

		ThreadPoolExecutor executor = (ThreadPoolExecutor) req
				.getServletContext().getAttribute(
						ContentServletContextListener.EXECUTOR);
		Future<?> future = executor.submit(new AsyncPlayerRequestProcessor(
				playerHandler, playRequest));

		// Store future for using it in case of error
		req.setAttribute(ContentAsyncListener.FUTURE_REQUEST_ATT_NAME, future);
	}
}
