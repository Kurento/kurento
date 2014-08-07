/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package org.kurento.jsonrpc.internal.server.config;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Strings;
import com.woorea.openstack.keystone.Keystone;
import com.woorea.openstack.keystone.model.Access;
import com.woorea.openstack.keystone.model.Authentication;
import com.woorea.openstack.keystone.model.authentication.UsernamePassword;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
public class OAuthFiWareFilter extends OncePerRequestFilter {

	private static final String X_AUTH_HEADER = "X-Auth-Token";

	private static final Logger log = LoggerFactory
			.getLogger(OAuthFiWareFilter.class);

	@Autowired
	private JsonRpcProperties props;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String fullUrl = request.getRequestURL().append('?')
				.append(request.getQueryString()).toString();

		log.trace("Client trying to stablish new websocket session with {}",
				fullUrl);

		if (!Strings.isNullOrEmpty(props.getKeystoneHost())) {
			String accessToken = parseAccessToken(request);
			if (Strings.isNullOrEmpty(accessToken)) {
				log.warn("Request from {} without OAuth token",
						request.getRemoteAddr());
				response.sendError(SC_UNAUTHORIZED,
						"Access token not found in request");
			} else if (isTokenValid(accessToken)) {
				log.trace("The request from {} was authorized",
						request.getRemoteAddr());
				filterChain.doFilter(request, response);
			} else {
				response.sendError(SC_UNAUTHORIZED, "Unathorized request");
			}
		} else {
			log.trace(
					"Request from {} authorized: no keystone host configured",
					request.getRemoteAddr());
			filterChain.doFilter(request, response);
		}
	}

	/**
	 * @param accessToken
	 * @return
	 */
	private boolean isTokenValid(String accessToken) {
		Response oauthResp = validateTokenWithServer(accessToken);

		if (!oauthResp.isSuccessful() && oauthResp.getCode() == 401) {
			props.setAuthToken(obtainFilterToken());
			oauthResp = validateTokenWithServer(accessToken);
		}

		if (!oauthResp.isSuccessful()) {
			String msg = "OAuth server returns error code: "
					+ oauthResp.getCode() + " and message '"
					+ oauthResp.getMessage() + '\'';

			log.warn("There was a request with a unauthorized OAuth token. {}",
					msg);
		}

		return oauthResp.isSuccessful();
	}

	private Response validateTokenWithServer(String accessToken) {
		String authToken = props.getAuthToken();

		String url = props.getKeystoneHost() + ':' + props.getKeystonePort()
				+ props.getKeystonePath() + accessToken;

		OAuthRequest oauthReq = new OAuthRequest(Verb.GET, url);
		oauthReq.addHeader(X_AUTH_HEADER, authToken);

		return oauthReq.send();
	}

	private String obtainFilterToken() {
		Keystone keystone = new Keystone(props.getKeystoneHost() + ':'
				+ props.getKeystonePort() + '/' + props.getOAuthVersion());
		Authentication authentication = new UsernamePassword(
				props.getKeystoneProxyUser(), props.getKeystoneProxyPass());

		Access access = keystone.tokens().authenticate(authentication)
				.execute();

		return access.getToken().getId();
	}

	/**
	 * Obtains the access token from the request, either form the X-Auth-Header
	 * or from the request parameters
	 * 
	 * @param request
	 * @return The access token. Null if none was found
	 */
	private String parseAccessToken(HttpServletRequest request) {
		String accessToken = request.getHeader(X_AUTH_HEADER);

		if (accessToken == null) {
			accessToken = request.getParameter(OAuthConstants.ACCESS_TOKEN);
		}

		return accessToken;
	}
}
