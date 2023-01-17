/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.repository.internal.repoimpl.mongo;

import org.kurento.repository.RepositoryApiConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

@Configuration
public class MongoConfiguration extends AbstractMongoConfiguration {

  @Autowired
  RepositoryApiConfiguration config;

  @Override
  public MongoClient mongoClient() {
    return new MongoClient(new MongoClientURI(config.getMongoUrlConnection()));
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
