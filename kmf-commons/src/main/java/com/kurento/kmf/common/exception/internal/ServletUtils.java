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
package com.kurento.kmf.common.exception.internal;

import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletUtils {

	public static void sendHttpError(HttpServletRequest request,
			HttpServletResponse response, int statusCode, String message)
			throws ServletException {
		try {
			response.setStatus(statusCode);
			response.setContentType("text/html;charset=utf-8");

			// Automatically chooses utf-8 given that we have set the charset
			PrintWriter pw = response.getWriter();

			pw.print("<html><head><title>Kurento Media Framework - Error Report</title>");
			pw.print("<style><!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} ");
			pw.print("H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} ");
			pw.print("H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} ");
			pw.print("BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} ");
			pw.print("B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} ");
			pw.print("P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}");
			pw.print("A {color : black;} A.name {color : black;}");
			pw.print("HR {color : #525D76;}--></style> ");
			pw.print("</head><body><h1>Status HTTP "
					+ String.valueOf(statusCode) + "-"
					+ request.getRequestURI() + "</h1>");
			pw.print("<HR size=\"1\" noshade=\"noshade\"><p><b>type</b>Request URI:</p><p><b>URI</b> <u>"
					+ request.getRequestURI());
			pw.print("</u></p><p><b>Error description</b> <u>" + message
					+ "</u></p><HR size=\"1\" noshade=\"noshade\">");
			pw.print("<h3>Kurento Media Framework</h3></body></html>");
			pw.flush();
		} catch (Throwable t) {
			throw new ServletException(t);
		}
	}
}
