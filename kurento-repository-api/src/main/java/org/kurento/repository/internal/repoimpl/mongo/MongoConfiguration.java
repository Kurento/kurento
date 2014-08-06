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

package org.kurento.repository.internal.repoimpl.mongo;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.kurento.repository.RepositoryApiConfiguration;

import com.mongodb.Mongo;
import com.mongodb.MongoURI;

@Configuration
public class MongoConfiguration extends AbstractMongoConfiguration {

	@Autowired
	RepositoryApiConfiguration config;

	@Override
	public Mongo mongo() throws UnknownHostException {
		return new Mongo(new MongoURI(config.getMongoURLConnection()));
	}

	@Override
	protected String getDatabaseName() {
		return config.getMongoDatabaseName();
	}

	@Override
	protected String getMappingBasePackage() {
		return "org.kurento.repository.repoimpl.mongo.domain";
	}
}
