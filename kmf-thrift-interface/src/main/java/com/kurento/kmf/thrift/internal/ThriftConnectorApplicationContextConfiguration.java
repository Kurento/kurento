///*
// * (C) Copyright 2013 Kurento (http://kurento.org/)
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU Lesser General Public License
// * (LGPL) version 2.1 which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/lgpl-2.1.html
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// */
//package com.kurento.kmf.thrift.internal;
//
//import java.net.InetSocketAddress;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Scope;
//
//import com.kurento.kmf.thrift.ThriftServer;
//import com.kurento.kmf.thrift.pool.MediaServerAsyncClientFactory;
//import com.kurento.kmf.thrift.pool.MediaServerAsyncClientPool;
//import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
//import com.kurento.kmf.thrift.pool.MediaServerSyncClientFactory;
//import com.kurento.kmf.thrift.pool.MediaServerSyncClientPool;
//import com.kurento.kms.thrift.api.KmsMediaHandlerService.Processor;
//
//@Configuration
//public class ThriftConnectorApplicationContextConfiguration {
//
//	@Bean
//	ThriftInterfaceExecutorService executorService() {
//		return new ThriftInterfaceExecutorService();
//	}
//
//	@Bean
//	MediaServerAsyncClientFactory mediaServerAsyncClientFactory() {
//		return new MediaServerAsyncClientFactory();
//	}
//
//	@Bean
//	MediaServerSyncClientFactory mediaServerSyncClientFactory() {
//		return new MediaServerSyncClientFactory();
//	}
//
//	@Bean
//	MediaServerClientPoolService mediaServerClientPoolService() {
//		return new MediaServerClientPoolService();
//	}
//
//	@Bean
//	MediaServerSyncClientPool mediaServerSyncClientPool() {
//		return new MediaServerSyncClientPool();
//	}
//
//	@Bean
//	MediaServerAsyncClientPool mediaServerAsyncClientPool() {
//		return new MediaServerAsyncClientPool();
//	}
//
//	@Bean
//	@Scope("prototype")
//	ThriftServer mediaHandlerServer(Processor<?> processor,
//			InetSocketAddress address) {
//		return new ThriftServer(processor, executorService(), address);
//	}
//
// }
