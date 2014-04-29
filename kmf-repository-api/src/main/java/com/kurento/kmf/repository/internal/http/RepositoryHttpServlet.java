/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package com.kurento.kmf.repository.internal.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.kurento.kmf.repository.RepositoryApiConfiguration;
import com.kurento.kmf.repository.RepositoryItem;
import com.kurento.kmf.repository.RepositoryItemAttributes;
import com.kurento.kmf.repository.internal.RepositoryHttpEndpointImpl;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

@WebServlet(value = "/repository_servlet/*", loadOnStartup = 1)
public class RepositoryHttpServlet extends HttpServlet {

	protected static class Range {

		public long start;
		public long end;
		public long length;

		/**
		 * Validate range.
		 */
		public boolean validate() {
			if (length != -1 && end >= length) {
				end = length - 1;
			}
			return (start >= 0) && (end >= 0) && (start <= end)
					&& (length == -1 || length > 0);
		}
	}

	private static Logger log = LoggerFactory
			.getLogger(RepositoryHttpServlet.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Full range constant.
	 */
	protected static final List<Range> FULL = new ArrayList<Range>();

	/**
	 * MIME multipart separation string
	 */
	protected static final String MIME_SEPARATION = "KURENTO_MIME_BOUNDARY";

	/**
	 * Size of file transfer buffer in bytes.
	 */
	protected static final int FILE_BUFFER_SIZE = 4096;

	/**
	 * The input buffer size to use when serving resources.
	 */
	private static final int INPUT_BUFFER_SIZE = 2048;

	/**
	 * The output buffer size to use when serving resources.
	 */
	private static final int OUTPUT_BUFFER_SIZE = 2048;

	/**
	 * The debugging detail level for this servlet.
	 */
	protected int debug = 0;

	/**
	 * RepoItemHttpElems
	 */
	@Autowired
	protected transient RepositoryHttpManager repoHttpManager = null;

	@Autowired
	private RepositoryApiConfiguration config;

	/**
	 * Finalize this servlet.
	 */
	@Override
	public void destroy() {
		// NOOP
	}

	/**
	 * Initialize this servlet.
	 */
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {

		super.init(servletConfig);

		configureKurentoAppContext(servletConfig);
		configureServletMapping(servletConfig);
		configureWebappPublicURL(servletConfig);

		if (servletConfig.getInitParameter("debug") != null) {
			debug = Integer.parseInt(getServletConfig().getInitParameter(
					"debug"));
		}
	}

	private String configureWebappPublicURL(ServletConfig servletConfig) {
		String webappURL = config.getWebappPublicURL();
		if (webappURL == null || webappURL.trim().isEmpty()) {
			webappURL = servletConfig.getServletContext().getContextPath();
		} else {
			if (webappURL.endsWith("/")) {
				webappURL = webappURL.substring(0, webappURL.length() - 1);
			}
		}

		repoHttpManager.setWebappPublicURL(webappURL);
		return webappURL;
	}

	private String configureServletMapping(ServletConfig servletConfig) {
		Collection<String> mappings = servletConfig.getServletContext()
				.getServletRegistration(servletConfig.getServletName())
				.getMappings();

		if (mappings.isEmpty()) {
			throw new RuntimeException("There is no mapping for servlet "
					+ RepositoryHttpServlet.class.getName());
		}

		String mapping = mappings.iterator().next();

		// TODO: Document this. We assume a mapping starting with / and ending
		// with /*
		mapping = mapping.substring(0, mapping.length() - 1);

		repoHttpManager.setServletPath(mapping);

		return mapping;
	}

	private void configureKurentoAppContext(ServletConfig servletConfig) {

		AnnotationConfigApplicationContext appCtx = KurentoApplicationContextUtils
				.getKurentoApplicationContext();

		if (appCtx == null) {
			appCtx = KurentoApplicationContextUtils
					.createKurentoApplicationContext(servletConfig
							.getServletContext());
		}

		KurentoApplicationContextUtils
				.processInjectionBasedOnKurentoApplicationContext(this);
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		logRequest(req);
		super.service(req, resp);
		logResponse(resp);
	}

	/**
	 * Override default implementation to ensure that TRACE is correctly
	 * handled.
	 * 
	 * @param req
	 *            the {@link HttpServletRequest} object that contains the
	 *            request the client made of the servlet
	 * 
	 * @param resp
	 *            the {@link HttpServletResponse} object that contains the
	 *            response the servlet returns to the client
	 * 
	 * @exception IOException
	 *                if an input or output error occurs while the servlet is
	 *                handling the OPTIONS request
	 * 
	 * @exception ServletException
	 *                if the request for the OPTIONS cannot be handled
	 */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		resp.setHeader("Allow", "GET, HEAD, POST, PUT, OPTIONS");
	}

	/**
	 * Process a HEAD request for the specified resource.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doHead(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		// Serve the requested resource, without the data content
		serveResource(request, response, false);

	}

	/**
	 * Process a POST request for the specified resource.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		doPut(request, response);
	}

	/**
	 * Process a PUT request for the specified resource.
	 * 
	 * @param req
	 *            The servlet request we are processing
	 * @param resp
	 *            The servlet response we are creating
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		uploadContent(req, resp);
	}

	/**
	 * Process a GET request for the specified resource.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		serveResource(request, response, true);
	}

	protected void uploadContent(HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {

		String sessionId = extractSessionId(req);

		RepositoryHttpEndpointImpl elem = repoHttpManager
				.getHttpRepoItemElem(sessionId);

		if (elem == null) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		elem.stopCurrentTimer();
		elem.fireStartedEventIfFirstTime();

		InputStream requestInputStream = req.getInputStream();

		try {

			OutputStream repoItemOutputStrem = elem.getRepoItemOutputStream();

			Range range = parseContentRange(req, resp);

			if (range != null) {

				if (range.start > elem.getWrittenBytes()) {
					resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
					resp.getOutputStream().println(
							"The server doesn't support writing ranges "
									+ "ahead of previously written bytes");
					return;
				}

				if (range.end == elem.getWrittenBytes()) {

					// TODO We assume that the put range is the same than the
					// previous one. Do we need to check this?

					resp.setStatus(HttpServletResponse.SC_OK);
					resp.getOutputStream()
							.println(
									"The server has detected that the submited range "
											+ "has already submited in a previous request");
					return;
				}

				if (range.start < elem.getWrittenBytes()
						&& range.end > elem.getWrittenBytes()) {

					Range copyRange = new Range();
					copyRange.start = elem.getWrittenBytes() - range.start;
					copyRange.end = range.end - range.start;

					copyStreamsRange(requestInputStream, repoItemOutputStrem,
							copyRange);

					resp.setStatus(HttpServletResponse.SC_OK);
					return;
				}

				if (range.start == elem.getWrittenBytes()) {

					IOUtils.copy(requestInputStream, repoItemOutputStrem);

					resp.setStatus(HttpServletResponse.SC_OK);

					return;
				}

			} else {

				boolean isMultipart = ServletFileUpload.isMultipartContent(req);

				if (isMultipart) {

					uploadMultipart(req, resp, repoItemOutputStrem);

				} else {

					try {

						log.info("Start to receive bytes (estimated "
								+ req.getContentLength() + " bytes)");
						int bytes = IOUtils.copy(requestInputStream,
								repoItemOutputStrem);
						resp.setStatus(HttpServletResponse.SC_OK);
						log.info("Bytes received: " + bytes);

					} catch (Exception e) {

						log.info("Exception when uploading content");

						elem.fireSessionErrorEvent(e);
						resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					}
				}
			}

		} finally {

			requestInputStream.close();
			elem.stopInTimeout();
		}
	}

	private void uploadMultipart(HttpServletRequest req,
			HttpServletResponse resp, OutputStream repoItemOutputStrem)
			throws IOException {

		log.info("Multipart detected");

		ServletFileUpload upload = new ServletFileUpload();

		try {

			// Parse the request
			FileItemIterator iter = upload.getItemIterator(req);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String name = item.getFieldName();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					// TODO What to do with this?
					log.info("Form field " + name + " with value "
							+ Streams.asString(stream) + " detected.");
				} else {

					// TODO Must we support multiple files uploading?
					log.info("File field " + name + " with file name "
							+ item.getName() + " detected.");

					log.info("Start to receive bytes (estimated "
							+ req.getContentLength() + " bytes)");
					int bytes = IOUtils.copy(stream, repoItemOutputStrem);
					resp.setStatus(HttpServletResponse.SC_OK);
					log.info("Bytes received: " + bytes);
				}
			}

		} catch (FileUploadException e) {
			throw new IOException(e);
		}
	}

	private void logRequest(HttpServletRequest req) {
		log.info("Request received " + req.getRequestURL());
		log.info("  Method: " + req.getMethod());

		Enumeration<String> headerNames = req.getHeaderNames();
		while (headerNames.hasMoreElements()) {

			String headerName = headerNames.nextElement();
			Enumeration<String> values = req.getHeaders(headerName);
			List<String> valueList = new ArrayList<String>();
			while (values.hasMoreElements()) {
				valueList.add(values.nextElement());
			}
			log.info("  Header " + headerName + ": " + valueList);
		}
	}

	private void logResponse(HttpServletResponse resp) {

		Collection<String> headerNames = resp.getHeaderNames();
		for (String headerName : headerNames) {
			Collection<String> values = resp.getHeaders(headerName);
			log.info("  Header " + headerName + ": " + values);
		}
	}

	/**
	 * Return the sessionId from the request.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 */
	protected String extractSessionId(HttpServletRequest request) {

		// Path info without leading "/"
		String pathInfo = request.getPathInfo();

		if (pathInfo != null && pathInfo.length() >= 1) {
			return pathInfo.substring(1);
		} else {
			return null;
		}
	}

	/**
	 * Handle a partial PUT. New content specified in request is appended to
	 * existing content in oldRevisionContent (if present). This code does not
	 * support simultaneous partial updates to the same resource.
	 */
	protected File executePartialPut(HttpServletRequest req, Range range,
			String sessionId) throws IOException {

		// TODO: Change this implementation to avoid Files. Try to
		// make the work on the repository implementation.

		// Append data specified in ranges to existing content for this
		// resource - create a temp. file on the local filesystem to
		// perform this operation
		// Assume just one range is specified for now

		File tempDir = (File) getServletContext().getAttribute(
				ServletContext.TEMPDIR);

		// Convert all '/' characters to '.' in resourcePath
		String convertedResourcePath = sessionId.replace('/', '.');

		File contentFile = new File(tempDir, convertedResourcePath);

		if (contentFile.createNewFile()) {
			// Clean up contentFile when Tomcat is terminated
			contentFile.deleteOnExit();
		}

		RandomAccessFile randAccessContentFile = new RandomAccessFile(
				contentFile, "rw");

		RepositoryHttpEndpointImpl repoItemHttpElem = repoHttpManager
				.getHttpRepoItemElem(sessionId);

		// Copy data in oldRevisionContent to contentFile
		if (repoItemHttpElem != null) {

			BufferedInputStream bufOldRevStream = new BufferedInputStream(
					repoItemHttpElem.createRepoItemInputStream(),
					FILE_BUFFER_SIZE);

			int numBytesRead;
			byte[] copyBuffer = new byte[FILE_BUFFER_SIZE];
			while ((numBytesRead = bufOldRevStream.read(copyBuffer)) != -1) {
				randAccessContentFile.write(copyBuffer, 0, numBytesRead);
			}

			bufOldRevStream.close();
		}

		randAccessContentFile.setLength(range.length);

		// Append data in request input stream to contentFile
		randAccessContentFile.seek(range.start);

		int numBytesRead;

		byte[] transferBuffer = new byte[FILE_BUFFER_SIZE];

		BufferedInputStream requestBufInStream = new BufferedInputStream(
				req.getInputStream(), FILE_BUFFER_SIZE);

		while ((numBytesRead = requestBufInStream.read(transferBuffer)) != -1) {
			randAccessContentFile.write(transferBuffer, 0, numBytesRead);
		}

		randAccessContentFile.close();
		requestBufInStream.close();

		return contentFile;
	}

	/**
	 * Check if the conditions specified in the optional If headers are
	 * satisfied.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            The resource information
	 * @return boolean true if the resource meets all the specified conditions,
	 *         and false if any of the conditions is not satisfied, in which
	 *         case request processing is stopped
	 */
	protected boolean checkIfHeaders(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) throws IOException {

		// TODO Investigate how to load properties for RepositoryItem (Mongo or
		// Filesystem)

		return checkIfMatch(request, response, resourceAttributes)
				&& checkIfModifiedSince(request, response, resourceAttributes)
				&& checkIfNoneMatch(request, response, resourceAttributes)
				&& checkIfUnmodifiedSince(request, response, resourceAttributes);
	}

	/**
	 * Serve the specified resource, optionally including the data content.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param content
	 *            Should the content be included?
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	protected void serveResource(HttpServletRequest request,
			HttpServletResponse response, boolean content) throws IOException,
			ServletException {

		boolean serveContent = content;

		// Identify the requested resource path
		String sessionId = extractSessionId(request);

		RepositoryHttpEndpointImpl elem = repoHttpManager
				.getHttpRepoItemElem(sessionId);

		if (elem == null) {

			if (debug > 0) {
				log("Resource with sessionId '" + sessionId + "' not found");
			}

			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					request.getRequestURI());
			return;
		}

		elem.fireStartedEventIfFirstTime();

		RepositoryItem repositoryItem = elem.getRepositoryItem();
		RepositoryItemAttributes attributes = repositoryItem.getAttributes();

		if (debug > 0) {
			if (serveContent) {
				log("Serving resource with sessionId '"
						+ sessionId
						+ "' headers and data. This resource corresponds to repository item '"
						+ repositoryItem.getId() + "'");
			} else {
				log("Serving resource with sessionId '"
						+ sessionId
						+ "' headers only. This resource corresponds to repository item '"
						+ repositoryItem.getId() + "'");
			}
		}

		boolean malformedRequest = response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST;

		if (!malformedRequest && !checkIfHeaders(request, response, attributes)) {
			return;
		}

		String contentType = getContentType(elem, attributes);

		List<Range> ranges = null;

		if (!malformedRequest) {

			response.setHeader("Accept-Ranges", "bytes");
			response.setHeader("ETag", attributes.getETag());
			response.setHeader("Last-Modified",
					attributes.getLastModifiedHttp());

			ranges = parseRange(request, response, attributes);
		}

		long contentLength = attributes.getContentLength();

		// Special case for zero length files, which would cause a
		// (silent) ISE when setting the output buffer size
		if (contentLength == 0L) {
			serveContent = false;
		}

		// Check to see if a Filter, Valve of wrapper has written some content.
		// If it has, disable range requests and setting of a content length
		// since neither can be done reliably.
		boolean contentWritten = response.isCommitted();

		if (contentWritten) {
			ranges = FULL;
		}

		boolean noRanges = (ranges == null || ranges.isEmpty());

		if (malformedRequest
				|| (noRanges && request.getHeader("Range") == null)
				|| ranges == FULL) {

			setContentType(response, contentType);

			if (contentLength >= 0) {
				// Don't set a content length if something else has already
				// written to the response.
				if (!contentWritten) {
					setContentLength(response, contentLength);
				}
			}

			// Copy the input stream to our output stream (if requested)
			if (serveContent) {
				copy(elem, response);
			}

		} else {

			if (noRanges) {
				return;
			}

			// Partial content response.
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

			if (ranges.size() == 1) {

				Range range = ranges.get(0);

				response.addHeader("Content-Range", "bytes " + range.start
						+ "-" + range.end + "/" + range.length);

				long length = range.end - range.start + 1;

				setContentLength(response, length);
				setContentType(response, contentType);

				if (serveContent) {
					copy(elem, response, range);
				}

			} else {

				response.setContentType("multipart/byteranges; boundary="
						+ MIME_SEPARATION);

				if (serveContent) {
					copy(elem, response, ranges, contentType);
				}
			}
		}

		elem.stopInTimeout();

	}

	private String getContentType(RepositoryHttpEndpointImpl repoItemHttpElem,
			RepositoryItemAttributes attributes) {

		String contentType = attributes.getMimeType();
		if (contentType == null) {
			contentType = getServletContext().getMimeType(
					repoItemHttpElem.getRepositoryItem().getId());
			attributes.setMimeType(contentType);
		}
		return contentType;
	}

	private void setContentType(HttpServletResponse response, String contentType) {
		if (contentType != null) {
			if (debug > 0) {
				log("contentType='" + contentType + "'");
			}
			response.setContentType(contentType);
		}
	}

	private void setContentLength(HttpServletResponse response, long length) {
		if (debug > 0) {
			log("contentLength=" + length);
		}

		if (length < Integer.MAX_VALUE) {
			response.setContentLength((int) length);
		} else {
			// Set the content-length as String to be able to use a long
			response.setHeader("content-length", "" + length);
		}
	}

	/**
	 * Parse the content-range header.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @return Range
	 */
	protected Range parseContentRange(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// Retrieving the content-range header (if any is specified
		String rangeHeader = request.getHeader("Content-Range");

		if (rangeHeader == null) {
			return null;
		}

		// bytes is the only range unit supported
		if (!rangeHeader.startsWith("bytes")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		rangeHeader = rangeHeader.substring(6).trim();

		int dashPos = rangeHeader.indexOf('-');
		int slashPos = rangeHeader.indexOf('/');

		if (dashPos == -1) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		if (slashPos == -1) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		Range range = new Range();

		try {
			range.start = Long.parseLong(rangeHeader.substring(0, dashPos));
			range.end = Long.parseLong(rangeHeader.substring(dashPos + 1,
					slashPos));

			String lengthString = rangeHeader.substring(slashPos + 1,
					rangeHeader.length());

			if (lengthString.equals("*")) {
				range.length = -1;
			} else {
				range.length = Long.parseLong(lengthString);
			}

		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		if (!range.validate()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		return range;
	}

	/**
	 * Parse the range header.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @return Vector of ranges
	 */
	protected List<Range> parseRange(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) throws IOException {

		// Checking If-Range
		String headerValue = request.getHeader("If-Range");

		if (headerValue != null) {

			long headerValueTime = -1L;
			try {
				headerValueTime = request.getDateHeader("If-Range");
			} catch (IllegalArgumentException e) {
				// Ignore
			}

			String eTag = resourceAttributes.getETag();
			long lastModified = resourceAttributes.getLastModified();

			if (headerValueTime == -1L) {

				// If the ETag the client gave does not match the entity
				// etag, then the entire entity is returned.
				if (!eTag.equals(headerValue.trim())) {
					return FULL;
				}

			} else {

				// If the timestamp of the entity the client got is older than
				// the last modification date of the entity, the entire entity
				// is returned.
				if (lastModified > (headerValueTime + 1000)) {
					return FULL;
				}

			}
		}

		long fileLength = resourceAttributes.getContentLength();

		if (fileLength == 0) {
			return null;
		}

		// Retrieving the range header (if any is specified
		String rangeHeader = request.getHeader("Range");

		if (rangeHeader == null) {
			return null;
		}
		// bytes is the only range unit supported (and I don't see the point
		// of adding new ones).
		if (!rangeHeader.startsWith("bytes")) {
			response.addHeader("Content-Range", "bytes */" + fileLength);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return null;
		}

		rangeHeader = rangeHeader.substring(6);

		// Vector which will contain all the ranges which are successfully
		// parsed.
		List<Range> result = new ArrayList<Range>();
		StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

		// Parsing the range list
		while (commaTokenizer.hasMoreTokens()) {

			String rangeDefinition = commaTokenizer.nextToken().trim();

			Range currentRange = new Range();
			currentRange.length = fileLength;

			int dashPos = rangeDefinition.indexOf('-');

			if (dashPos == -1) {
				response.addHeader("Content-Range", "bytes */" + fileLength);
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return null;
			}

			if (dashPos == 0) {

				try {
					long offset = Long.parseLong(rangeDefinition);
					currentRange.start = fileLength + offset;
					currentRange.end = fileLength - 1;
				} catch (NumberFormatException e) {
					response.addHeader("Content-Range", "bytes */" + fileLength);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					return null;
				}

			} else {

				try {
					currentRange.start = Long.parseLong(rangeDefinition
							.substring(0, dashPos));
					if (dashPos < rangeDefinition.length() - 1) {
						currentRange.end = Long.parseLong(rangeDefinition
								.substring(dashPos + 1,
										rangeDefinition.length()));
					} else {
						currentRange.end = fileLength - 1;
					}
				} catch (NumberFormatException e) {
					response.addHeader("Content-Range", "bytes */" + fileLength);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					return null;
				}

			}

			if (!currentRange.validate()) {
				response.addHeader("Content-Range", "bytes */" + fileLength);
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return null;
			}

			result.add(currentRange);
		}

		return result;
	}

	/**
	 * Check if the if-match condition is satisfied.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfMatch(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) throws IOException {

		String eTag = resourceAttributes.getETag();
		String headerValue = request.getHeader("If-Match");

		if (headerValue != null) {
			if (headerValue.indexOf('*') == -1) {

				StringTokenizer commaTokenizer = new StringTokenizer(
						headerValue, ",");
				boolean conditionSatisfied = false;

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag)) {
						conditionSatisfied = true;
					}
				}

				// If none of the given ETags match, 412 Precodition failed is
				// sent back
				if (!conditionSatisfied) {
					response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check if the if-modified-since condition is satisfied.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfModifiedSince(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) {

		try {
			long headerValue = request.getDateHeader("If-Modified-Since");
			long lastModified = resourceAttributes.getLastModified();
			if (headerValue != -1) {

				// If an If-None-Match header has been specified, if modified
				// since
				// is ignored.
				if ((request.getHeader("If-None-Match") == null)
						&& (lastModified < headerValue + 1000)) {
					// The entity has not been modified since the date
					// specified by the client. This is not an error case.
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					response.setHeader("ETag", resourceAttributes.getETag());

					return false;
				}
			}
		} catch (IllegalArgumentException illegalArgument) {
			return true;
		}

		return true;
	}

	/**
	 * Check if the if-none-match condition is satisfied.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfNoneMatch(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) throws IOException {

		String eTag = resourceAttributes.getETag();
		String headerValue = request.getHeader("If-None-Match");

		if (headerValue != null) {

			boolean conditionSatisfied = false;

			if (!headerValue.equals("*")) {

				StringTokenizer commaTokenizer = new StringTokenizer(
						headerValue, ",");

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag)) {
						conditionSatisfied = true;
					}
				}

			} else {
				conditionSatisfied = true;
			}

			if (conditionSatisfied) {

				// For GET and HEAD, we should respond with
				// 304 Not Modified.
				// For every other method, 412 Precondition Failed is sent
				// back.
				if (("GET".equals(request.getMethod()))
						|| ("HEAD".equals(request.getMethod()))) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					response.setHeader("ETag", eTag);

					return false;
				}
				response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if the if-unmodified-since condition is satisfied.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfUnmodifiedSince(HttpServletRequest request,
			HttpServletResponse response,
			RepositoryItemAttributes resourceAttributes) throws IOException {

		try {
			long lastModified = resourceAttributes.getLastModified();
			long headerValue = request.getDateHeader("If-Unmodified-Since");
			if (headerValue != -1) {
				if (lastModified >= (headerValue + 1000)) {
					// The entity has not been modified since the date
					// specified by the client. This is not an error case.
					response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
					return false;
				}
			}
		} catch (IllegalArgumentException illegalArgument) {
			return true;
		}

		return true;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 * 
	 * @param repoItemHttpElem
	 *            The cache entry for the source resource
	 * @param response
	 *            The HttpResponse where the resource will be copied
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(RepositoryHttpEndpointImpl repoItemHttpElem,
			HttpServletResponse response) throws IOException {
		copy(repoItemHttpElem, response, null);
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 * 
	 * @param repoItemHttpElem
	 *            The cache entry for the source resource
	 * @param response
	 *            The response we are writing to
	 * @param range
	 *            Range asked by the client
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(RepositoryHttpEndpointImpl repoItemHttpElem,
			HttpServletResponse response, Range range) throws IOException {

		try {
			response.setBufferSize(OUTPUT_BUFFER_SIZE);
		} catch (IllegalStateException e) {
			// Silent catch
		}

		ServletOutputStream ostream = response.getOutputStream();

		InputStream istream = new BufferedInputStream(
				repoItemHttpElem.createRepoItemInputStream(), INPUT_BUFFER_SIZE);

		IOException exception;
		if (range != null) {
			exception = copyStreamsRange(istream, ostream, range);
		} else {
			exception = copyStreams(istream, ostream);
		}

		// Clean up the input stream
		istream.close();

		// Rethrow any exception that has occurred
		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 * 
	 * @param repoItemHttpElem
	 *            The cache entry for the source resource
	 * @param response
	 *            The response we are writing to
	 * @param ranges
	 *            Enumeration of the ranges the client wanted to retrieve
	 * @param contentType
	 *            Content type of the resource
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(RepositoryHttpEndpointImpl repoItemHttpElem,
			HttpServletResponse response, List<Range> ranges, String contentType)
			throws IOException {

		try {
			response.setBufferSize(OUTPUT_BUFFER_SIZE);
		} catch (IllegalStateException e) {
			// Silent catch
		}

		IOException exception = null;
		ServletOutputStream ostream = response.getOutputStream();

		for (Range currentRange : ranges) {

			InputStream istream = new BufferedInputStream(
					repoItemHttpElem.createRepoItemInputStream(),
					INPUT_BUFFER_SIZE);

			// Writing MIME header.
			ostream.println();
			ostream.println("--" + MIME_SEPARATION);

			if (contentType != null) {
				ostream.println("Content-Type: " + contentType);
			}

			ostream.println("Content-Range: bytes " + currentRange.start + "-"
					+ currentRange.end + "/" + currentRange.length);
			ostream.println();

			exception = copyStreamsRange(istream, ostream, currentRange);

			istream.close();

			if (exception != null) {
				break;
			}
		}

		ostream.println();
		ostream.print("--" + MIME_SEPARATION + "--");

		// Rethrow any exception that has occurred
		if (exception != null) {
			throw exception;
		}

	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 * 
	 * @param istream
	 *            The input stream to read from
	 * @param ostream
	 *            The output stream to write to
	 * @return Exception which occurred during processing
	 */
	protected IOException copyStreams(InputStream istream, OutputStream ostream) {

		// Copy the input stream to the output stream
		IOException exception = null;

		byte buffer[] = new byte[INPUT_BUFFER_SIZE];

		int len = buffer.length;
		while (true) {
			try {
				len = istream.read(buffer);
				if (len == -1) {
					break;
				}
				ostream.write(buffer, 0, len);

				log.debug(len + " bytes has been written to item");

			} catch (IOException e) {
				exception = e;
				len = -1;
				break;
			}
		}

		return exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 * 
	 * @param istream
	 *            The input stream to read from
	 * @param ostream
	 *            The output stream to write to
	 * @param range
	 *            Range we are copying
	 *
	 * @return Exception which occurred during processing
	 */
	protected IOException copyStreamsRange(InputStream istream,
			OutputStream ostream, Range range) {

		long start = range.start;
		long end = range.end;

		if (debug > 10) {
			log("Serving bytes:" + start + "-" + end);
		}

		long skipped = 0;
		try {
			skipped = istream.skip(start);
		} catch (IOException e) {
			return e;
		}
		if (skipped < start) {
			return new IOException("Has been skiped " + skipped + " when "
					+ start + " is required");
		}

		IOException exception = null;
		long remBytes = end - start + 1;

		byte buffer[] = new byte[INPUT_BUFFER_SIZE];
		int readBytes = buffer.length;
		while (remBytes > 0) {
			try {
				readBytes = istream.read(buffer);
				if (readBytes == -1) {
					break;
				} else if (readBytes <= remBytes) {
					ostream.write(buffer, 0, readBytes);
					remBytes -= readBytes;
				} else {
					ostream.write(buffer, 0, (int) remBytes);
					break;
				}
			} catch (IOException e) {
				exception = e;
				break;
			}
		}

		// int len = buffer.length;
		// while ((bytesToRead > 0) && (len >= buffer.length)) {
		// try {
		// len = istream.read(buffer);
		// if (bytesToRead >= len) {
		// ostream.write(buffer, 0, len);
		// bytesToRead -= len;
		// } else {
		// ostream.write(buffer, 0, (int) bytesToRead);
		// bytesToRead = 0;
		// }
		// } catch (IOException e) {
		// exception = e;
		// len = -1;
		// }
		// if (len < buffer.length) {
		// break;
		// }
		// }

		return exception;
	}

}
