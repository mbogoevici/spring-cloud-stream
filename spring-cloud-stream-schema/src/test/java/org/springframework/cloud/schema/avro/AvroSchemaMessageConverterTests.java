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
import org.springframework.cloud.stream.schema.SchemaRegistryClient;
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
		FooPojo firstOutboundFoo = new FooPojo();
		firstOutboundFoo.setFoo("foo" + UUID.randomUUID().toString());
		firstOutboundFoo.setBar("foo" + UUID.randomUUID().toString());
		source.output().send(MessageBuilder.withPayload(firstOutboundFoo).build());
		MessageCollector sourceMessageCollector = sourceContext.getBean(MessageCollector.class);
		Message<?> outboundMessage = sourceMessageCollector.forChannel(source.output()).poll(1000,
				TimeUnit.MILLISECONDS);


		ConfigurableApplicationContext barSourceContext = SpringApplication.run(AvroSourceApplication.class,
				"--server.port=0",
				"--spring.jmx.enabled=false",
				"--spring.cloud.stream.bindings.output.contentType=application/vnd.org.springframework.cloud.schema.avro.avroschemamessageconvertertests$.foopojo.v1+avro",
				"--spring.cloud.stream.schema.avro.dynamicSchemaGenerationEnabled=true");
		Source barSource = barSourceContext.getBean(Source.class);
		BarPojo firstOutboundBarPojo = new BarPojo();
		firstOutboundBarPojo.setFoo("foo" + UUID.randomUUID().toString());
		firstOutboundBarPojo.setBar("foo" + UUID.randomUUID().toString());
		barSource.output().send(MessageBuilder.withPayload(firstOutboundBarPojo).build());
		MessageCollector barSourceMessageCollector = barSourceContext.getBean(MessageCollector.class);
		Message<?> barOutboundMessage = barSourceMessageCollector.forChannel(barSource.output()).poll(1000,
				TimeUnit.MILLISECONDS);

		assertThat(barOutboundMessage).isNotNull();


		BarPojo secondBarOutboundPojo = new BarPojo();
		secondBarOutboundPojo.setFoo("foo" + UUID.randomUUID().toString());
		secondBarOutboundPojo.setBar("foo" + UUID.randomUUID().toString());
		source.output().send(MessageBuilder.withPayload(secondBarOutboundPojo).build());
		Message<?> secondBarOutboundMessage = sourceMessageCollector.forChannel(source.output()).poll(1000,
				TimeUnit.MILLISECONDS);


		ConfigurableApplicationContext sinkContext = SpringApplication.run(AvroSinkApplication.class,
				"--server.port=0", "--spring.jmx.enabled=false");
		Sink sink = sinkContext.getBean(Sink.class);
		sink.input().send(outboundMessage);
		sink.input().send(barOutboundMessage);
		sink.input().send(secondBarOutboundMessage);
		List<FooPojo> receivedPojos = sinkContext.getBean(AvroSinkApplication.class).receivedPojos;
		assertThat(receivedPojos).hasSize(3);
		assertThat(receivedPojos.get(0)).isNotSameAs(firstOutboundFoo);
		assertThat(receivedPojos.get(0).getFoo()).isEqualTo(firstOutboundFoo.getFoo());
		assertThat(receivedPojos.get(0).getBar()).isEqualTo(firstOutboundFoo.getBar());

		assertThat(receivedPojos.get(1)).isNotSameAs(firstOutboundBarPojo);
		assertThat(receivedPojos.get(1).getFoo()).isEqualTo(firstOutboundBarPojo.getFoo());
		assertThat(receivedPojos.get(1).getBar()).isEqualTo(firstOutboundBarPojo.getBar());

		assertThat(receivedPojos.get(2)).isNotSameAs(secondBarOutboundPojo);
		assertThat(receivedPojos.get(2).getFoo()).isEqualTo(secondBarOutboundPojo.getFoo());
		assertThat(receivedPojos.get(2).getBar()).isEqualTo(secondBarOutboundPojo.getBar());

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

	public static class BarPojo {

		@Nullable
		String foo;

		@Nullable
		String bar;

		public BarPojo() {
		}

		public BarPojo(String foo, String bar) {
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
