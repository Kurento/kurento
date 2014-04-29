package com.kurento.tool.rom.test;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.MapMaker;
import com.kurento.tool.rom.client.RemoteObject;
import com.kurento.tool.rom.client.RomClientObjectManager;

public class RomClientWeakRemoteObjects {

	@Test
	public void testWeakReference() throws Exception {

		WeakReference<Object> weakReference = new WeakReference<>(new Object());

		if (null == weakReference.get()) {
			Assert.fail("Reference should NOT be null");
		}

		try {
			@SuppressWarnings("unused")
			Object[] ignored = new Object[(int) Runtime.getRuntime()
					.maxMemory()];
		} catch (Throwable e) {
			// Ignore OME
		}

		if (null != weakReference.get()) {
			Assert.fail("Reference should be null");
		}
	}

	@Test
	public void testWeakRefsMap() throws Exception {

		ConcurrentMap<String, Object> objects = new MapMaker().weakValues()
				.makeMap();

		objects.put("xxx", new Object());

		if (null == objects.get("xxx")) {
			Assert.fail("Reference should NOT be null");
		}

		try {
			@SuppressWarnings("unused")
			Object[] ignored = new Object[(int) Runtime.getRuntime()
					.maxMemory()];
		} catch (Throwable e) {
			// Ignore OME
		}

		if (null != objects.get("xxx")) {
			Assert.fail("Reference should be null");
		}
	}

	@Test
	public void testRomClientObjectManager() {

		RomClientObjectManager manager = new RomClientObjectManager(null);
		new RemoteObject("xxx", null, null, manager);

		if (null == manager.getRemoteObject("xxx")) {
			Assert.fail("Reference should NOT be null");
		}

		try {
			@SuppressWarnings("unused")
			Object[] ignored = new Object[(int) Runtime.getRuntime()
					.maxMemory()];
		} catch (Throwable e) {
			// Ignore OME
		}

		if (null != manager.getRemoteObject("xxx")) {
			Assert.fail("Reference should be null");
		}
	}

	@Test
	public void testRomClientObjectManager2() {

		RomClientObjectManager manager = new RomClientObjectManager(null);
		RemoteObject obj = new RemoteObject("xxx", null, null, manager);

		if (obj != manager.getRemoteObject("xxx")) {
			Assert.fail("Reference should be equals to inserted remote object");
		}

		obj = null;

		try {
			@SuppressWarnings("unused")
			Object[] ignored = new Object[(int) Runtime.getRuntime()
					.maxMemory()];
		} catch (Throwable e) {
			// Ignore OME
		}

		if (null != manager.getRemoteObject("xxx")) {
			Assert.fail("Reference should be null");
		}
	}
}
