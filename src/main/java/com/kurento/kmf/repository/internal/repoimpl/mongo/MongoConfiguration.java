package com.kurento.kmf.repository.internal.repoimpl.mongo;

import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.kurento.kmf.repository.RepositoryApiConfiguration;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

@Configuration
// @EnableMongoRepositories
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
		return "com.kurento.kmf.repository.repoimpl.mongo.domain";
	}
}
