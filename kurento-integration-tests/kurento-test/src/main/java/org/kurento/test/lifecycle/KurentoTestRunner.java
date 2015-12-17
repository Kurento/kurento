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

package org.kurento.test.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkField;
import org.kurento.test.services.Service;

/**
 * Custom runner in Kurento Testing Framework.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class KurentoTestRunner extends Parameterized {

  private static boolean shutdownHook = false;

  public KurentoTestRunner(Class<?> clazz) throws Throwable {
    super(clazz);
  }

  private static KurentoTestListener listener;

  @Override
  public void run(RunNotifier notifier) {

    if (listener != null) {
      notifier.removeListener(listener);
    }

    List<FrameworkField> services = this.getTestClass().getAnnotatedFields(Service.class);

    ArrayList<FrameworkField> sortedServices = new ArrayList<>(services);
    Collections.sort(sortedServices, new Comparator<FrameworkField>() {
      @Override
      public int compare(FrameworkField o1, FrameworkField o2) {
        return Integer.compare(o1.getAnnotation(Service.class).value(),
            o2.getAnnotation(Service.class).value());
      }
    });

    listener = new KurentoTestListener(sortedServices);
    notifier.addListener(listener);
    listener.testRunStarted(getDescription());

    if (!shutdownHook) {
      shutdownHook = true;
      Runtime.getRuntime().addShutdownHook(new Thread("app-shutdown-hook") {
        @Override
        public void run() {
          listener.testSuiteFinished();
        }
      });
    }

    // TODO Remove this if we change service management
    notifier = new KurentoRunNotifier(notifier);

    super.run(notifier);
  }

}
