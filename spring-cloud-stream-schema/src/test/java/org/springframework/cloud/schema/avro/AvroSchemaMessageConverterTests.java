/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.schema.avro;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.avro.reflect.Nullable;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.schema.avro.SchemaRegistryClient;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marius Bogoevici
 */
public class AvroSchemaMessageConverterTests {

	static StubSchemaRegistryClient stubSchemaRegistryClient = new StubSchemaRegistryClient();

	@Test
	public void testSendMessage() throws Exception {
		ConfigurableApplicationContext sourceContext = SpringApplication.run(AvroSourceApplication.class,
				"--server.port=0",
				"--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/*+avro",
				"--spring.cloud.stream.schema.avro.dynamicSchemaGenerationEnabled=true");
		Source source = sourceContext.getBean(Source.class);
		FooPojo originalOutboundMessage = new FooPojo();
		originalOutboundMessage.setFoo("foo" + UUID.randomUUID().toString());
		originalOutboundMessage.setBar("foo" + UUID.randomUUID().toString());
		source.output().send(MessageBuilder.withPayload(originalOutboundMessage).build());
		MessageCollector sourceMessageCollector = sourceContext.getBean(MessageCollector.class);
		Message<?> outboundMessage = sourceMessageCollector.forChannel(source.output()).poll(1000,
				TimeUnit.MILLISECONDS);

		assertThat(outboundMessage).isNotNull();

		ConfigurableApplicationContext sinkContext = SpringApplication.run(AvroSinkApplication.class,
				"--server.port=0", "--spring.jmx.enabled=false");
		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(outboundMessage);
		List<FooPojo> receivedPojos = sinkContext.getBean(AvroSinkApplication.class).receivedPojos;
		assertThat(receivedPojos).hasSize(1);
		assertThat(receivedPojos.get(0)).isNotSameAs(originalOutboundMessage);
		assertThat(receivedPojos.get(0).getFoo()).isEqualTo(originalOutboundMessage.getFoo());
		assertThat(receivedPojos.get(0).getBar()).isEqualTo(originalOutboundMessage.getBar());

		sourceContext.close();
	}

	@EnableBinding(Source.class)
	@EnableAutoConfiguration
	public static class AvroSourceApplication {

		@Bean
		public SchemaRegistryClient schemaRegistryClient() {
			return stubSchemaRegistryClient;
		}
	}

	@EnableBinding(Sink.class)
	@EnableAutoConfiguration
	public static class AvroSinkApplication {

		public List<FooPojo> receivedPojos = new ArrayList<>();

		@StreamListener(Sink.INPUT)
		public void listen(FooPojo fooPojo) {
			receivedPojos.add(fooPojo);
		}

		@Bean
		public SchemaRegistryClient schemaRegistryClient() {
			return stubSchemaRegistryClient;
		}

	}

	public static class FooPojo {

		@Nullable
		String foo;

		@Nullable
		String bar;

		public FooPojo() {
		}

		public FooPojo(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}
}
