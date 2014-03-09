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
package com.kurento.kmf.content;

/**
 * 
 * Defines the events associated to the HTTP play operation (
 * {@link #onContentRequest(ContentSession)},
 * {@link #onSessionTerminated(ContentSession,int,String)}, and
 * {@link #onSessionError(ContentSession,int,String)}); the implementation of
 * the PlayerHandler should be used in conjunction with
 * {@link HttpPlayerService} annotation. The following snippet shows an skeleton
 * with the implementation of a Player:
 * 
 * <pre>
 * <code>
 * &#064;PlayerService(name = &quot;MyPlayerHandlerName&quot;, path = &quot;/my-player&quot;, redirect = &quot;true&quot;, useControlProtocol = &quot;false&quot;)
 * public class MyPlayerHandler implements PlayerHandler {
 * 
 * 	&#064;Override
 * 	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentPlayed(PlayRequest playRequest) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentError(PlayRequest playRequest,
 * 			ContentException exception) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </code>
 * </pre>
 * 
 * @see HttpPlayerService
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public abstract class HttpPlayerHandler extends
		ContentHandler<HttpPlayerSession> {
}
