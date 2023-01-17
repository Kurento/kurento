/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.client.internal.test;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

import org.junit.Assert;
import org.junit.Test;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RomManager;

import com.google.common.collect.MapMaker;

public class RomClientWeakRemoteObjects {

  @Test
  public void testWeakReference() throws Exception {

    WeakReference<Object> weakReference = new WeakReference<>(new Object());

    if (null == weakReference.get()) {
      Assert.fail("Reference should NOT be null");
    }

    try {
      @SuppressWarnings("unused")
      Object[] ignored = new Object[(int) Runtime.getRuntime().maxMemory()];
    } catch (Throwable e) {
      // Ignore OME
    }

    if (null != weakReference.get()) {
      Assert.fail("Reference should be null");
    }
  }

  @Test
  public void testWeakRefsMap() throws Exception {

    ConcurrentMap<String, Object> objects = new MapMaker().weakValues().makeMap();

    objects.put("xxx", new Object());

    if (null == objects.get("xxx")) {
      Assert.fail("Reference should NOT be null");
    }

    try {
      @SuppressWarnings("unused")
      Object[] ignored = new Object[(int) Runtime.getRuntime().maxMemory()];
    } catch (Throwable e) {
      // Ignore OME
    }

    if (null != objects.get("xxx")) {
      Assert.fail("Reference should be null");
    }
  }

  @Test
  public void testRomClientObjectManager() {

    RomManager manager = new RomManager(null);

    new RemoteObject("xxx", null, manager);

    if (null == manager.getObjectManager().getRemoteObject("xxx")) {
      Assert.fail("Reference should NOT be null");
    }

    try {
      @SuppressWarnings("unused")
      Object[] ignored = new Object[(int) Runtime.getRuntime().maxMemory()];
    } catch (Throwable e) {
      // Ignore OME
    }

    if (null != manager.getObjectManager().getRemoteObject("xxx")) {
      Assert.fail("Reference should be null");
    }
  }

  @Test
  public void testRomClientObjectManager2() {

    RomManager manager = new RomManager(null);

    RemoteObject obj = new RemoteObject("xxx", null, manager);

    if (obj != manager.getObjectManager().getRemoteObject("xxx")) {
      Assert.fail("Reference should be equals to inserted remote object");
    }

    obj = null;

    try {
      @SuppressWarnings("unused")
      Object[] ignored = new Object[(int) Runtime.getRuntime().maxMemory()];
    } catch (Throwable e) {
      // Ignore OME
    }

    if (null != manager.getObjectManager().getRemoteObject("xxx")) {
      Assert.fail("Reference should be null");
    }
  }
}
