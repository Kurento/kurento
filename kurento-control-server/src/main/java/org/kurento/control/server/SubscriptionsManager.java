package org.kurento.control.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.kurento.jsonrpc.Session;

public class SubscriptionsManager {

	private final ConcurrentMap<String, Session> sessionsBySubsId = new ConcurrentHashMap<>();

	private final Multimap<String, Session> sessionsByObjAndType = Multimaps
			.synchronizedSetMultimap(HashMultimap.<String, Session> create());

	public void removeSession(Session session) {
		removeSessionFromIterator(sessionsByObjAndType.entries().iterator(),
				session);
		removeSessionFromIterator(sessionsBySubsId.entrySet().iterator(),
				session);
	}

	private void removeSessionFromIterator(Iterator<Entry<String, Session>> it,
			Session session) {
		while (it.hasNext()) {
			Entry<String, Session> value = it.next();
			if (value.getValue() == session) {
				it.remove();
			}
		}
	}

	public void addSubscription(String subscriptionId, String objectAndType,
			Session session) {

		sessionsBySubsId.put(subscriptionId, session);
		sessionsByObjAndType.put(objectAndType, session);
	}

	public Collection<Session> getSessionsByObjAndType(String objectAndType) {
		return sessionsByObjAndType.get(objectAndType);
	}

	public Collection<Session> getSessionsBySubscription(String subscriptionId) {
		Session session = sessionsBySubsId.get(subscriptionId);
		if (session == null) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(session);
		}
	}
}
