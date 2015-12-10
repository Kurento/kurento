/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.test.services;

import static org.kurento.commons.PropertiesManager.getProperty;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_DEFAULT;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_AUTOSTART_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_LOGIN_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_PASSWD_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_PEM_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_PROP;
import static org.kurento.test.config.TestConfiguration.FAKE_KMS_WS_URI_PROP_EXPORT;

/**
 * Fake Kurento Media Server service.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class FakeKmsService extends KmsService {

  public FakeKmsService() {
    this.kmsLoginProp = FAKE_KMS_LOGIN_PROP;
    this.kmsPasswdProp = FAKE_KMS_PASSWD_PROP;
    this.kmsPemProp = FAKE_KMS_PEM_PROP;
    this.kmsAutostartProp = FAKE_KMS_AUTOSTART_PROP;
    this.kmsAutostartDefault = FAKE_KMS_AUTOSTART_DEFAULT;
    this.kmsWsUriProp = FAKE_KMS_WS_URI_PROP;
    this.kmsWsUriExportProp = FAKE_KMS_WS_URI_PROP_EXPORT;

    setWsUri(getProperty(kmsWsUriProp));
  }

}
