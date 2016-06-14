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

package org.springframework.cloud.stream.binder;

import java.util.Map;

import org.springframework.context.Lifecycle;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.MediaType;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractBinder} that serves as base class for {@link MessageChannel} - based implementations.
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageChannelBinder<C extends ConsumerProperties, P extends ProducerProperties>
		extends AbstractBinder<MessageChannel, C, P> {

	protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new
			EmbeddedHeadersMessageConverter();

	protected static boolean isAnonymous(String group) {
		return !StringUtils.hasText(group);
	}

	@Override
	public Binding<MessageChannel> doBindProducer(String name, MessageChannel outputChannel,
			P producerProperties) {
		Assert.isInstanceOf(SubscribableChannel.class, outputChannel);
		MessageHandler messageHandler = null;
		try {
			createProducerDestinationIfNecessary(name, producerProperties);
			messageHandler = buildOutboundEndpoint(name, producerProperties);
			return doRegisterProducer(name, outputChannel, messageHandler, producerProperties);
		}
		catch (Exception e) {
			if (messageHandler instanceof Lifecycle) {
				((Lifecycle) messageHandler).stop();
			}
			throw new BinderException("Exception thrown while building outbound endpoint", e);
		}
	}


	@Override
	public final Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel inputChannel,
			C properties) {
		Object queue = createConsumerDestinationIfNecessary(name, group, properties);

		final AbstractEndpoint consumerEndpoint = createConsumerEndpoint(name, group, queue, inputChannel, properties);

		return createBinding(name, group, inputChannel, properties, consumerEndpoint);
	}

	protected Binding<MessageChannel> createBinding(String name, String group, MessageChannel inputChannel,
			C properties, AbstractEndpoint consumerEndpoint) {
		return new DefaultBinding<>(name, group, inputChannel, consumerEndpoint);
	}

	protected abstract AbstractEndpoint createConsumerEndpoint(String name, String group, Object queue,
			MessageChannel inputChannel,
			C properties);

	protected abstract Object createConsumerDestinationIfNecessary(String name, String group, C properties);


	protected abstract void createProducerDestinationIfNecessary(String name, P properties);


	protected abstract MessageHandler buildOutboundEndpoint(String name, P producerProperties) throws Exception;

	protected abstract Binding<MessageChannel> doRegisterProducer(String name, MessageChannel outputChannel,
			MessageHandler endpoint,
			P producerProperties);

	public class ReceivingHandler extends AbstractReplyProducingMessageHandler {

		private final boolean handleEmbeddedHeaders;

		private final boolean deserializePayload;

		public ReceivingHandler(boolean handleEmbeddedHeaders, boolean deserializePayload) {
			this.handleEmbeddedHeaders = handleEmbeddedHeaders;
			this.deserializePayload = deserializePayload;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Object handleRequestMessage(Message<?> requestMessage) {
			if (!this.handleEmbeddedHeaders && !this.deserializePayload) {
				return requestMessage;
			}
			MessageValues messageValues1;
			// Here we must separate the condition for extracting the request message and doing the serialization
			// e.g Kafka raw lets the message pass through, but Rabbit deserializes
			// Maybe we can apply the MessageConverters here too ?
			if (this.handleEmbeddedHeaders) {
				try {
					messageValues1 = AbstractMessageChannelBinder.this.embeddedHeadersMessageConverter.extractHeaders(
							(Message<byte[]>) requestMessage, true);
				}
				catch (Exception e) {
					AbstractMessageChannelBinder.this.logger.error(
							EmbeddedHeadersMessageConverter.decodeExceptionMessage(
									requestMessage), e);
					messageValues1 = new MessageValues(requestMessage);
				}
				if (this.deserializePayload) {
					messageValues1 = deserializePayloadIfNecessary(messageValues1);
				}
			}
			else {
				messageValues1 = deserializePayloadIfNecessary(requestMessage);
			}
			return createMessage(messageValues1);
		}

		protected Message<?> createMessage(MessageValues messageValues) {
			return messageValues.toMessage();
		}


		@Override
		protected boolean shouldCopyRequestHeaders() {
			// prevent the message from being copied again in superclass
			return false;
		}
	}

	protected abstract class SendingHandler<D, P> extends AbstractMessageHandler implements Lifecycle {

		protected final D delegate;

		private final ProducerProperties producerProperties;

		private final PartitionHandler partitionHandler;

		private final boolean embedHeaders;

		private final boolean supportsHeaders;

		private final String[] embeddedHeaders;

		protected SendingHandler(D delegate, ProducerProperties properties, boolean supportsHeaders,
				String[] embeddedHeaders) {
			this.delegate = delegate;
			this.producerProperties = properties;
			this.supportsHeaders = supportsHeaders;
			this.setBeanFactory(AbstractMessageChannelBinder.this.getBeanFactory());
			this.partitionHandler = new PartitionHandler(AbstractMessageChannelBinder.this.getBeanFactory(),
					AbstractMessageChannelBinder.this.evaluationContext,
					AbstractMessageChannelBinder.this.partitionSelector, properties);
			this.embedHeaders = !supportsHeaders && HeaderMode.embeddedHeaders.equals(
					this.producerProperties.getHeaderMode());
			this.embeddedHeaders = embeddedHeaders;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			Integer targetPartition =
					this.producerProperties.isPartitioned() ? this.partitionHandler.determinePartition(message) : null;
			MessageValues transformed = serializePayloadIfNecessary(message);
			Object payload;
			if (this.embedHeaders) {
				payload = AbstractMessageChannelBinder.this.embeddedHeadersMessageConverter.embedHeaders(transformed,
						this.embeddedHeaders);
			}
			else {
				payload = transformed.getPayload();
			}
			if (!this.embedHeaders && !this.supportsHeaders) {
				Object contentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
				if (contentType != null && !contentType.equals(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
					this.logger.error(
							"Raw mode supports only " + MediaType.APPLICATION_OCTET_STREAM_VALUE + " content type"
									+ message.getPayload().getClass());
				}
				if (message.getPayload() instanceof byte[]) {
					payload = message.getPayload();
				}
				else {
					throw new BinderException("Raw mode supports only byte[] payloads but value sent was of type "
							+ message.getPayload().getClass());
				}
			}
			send(targetPartition, (P) payload, this.supportsHeaders ? transformed.getHeaders() : null);
		}

		protected abstract void send(Integer targetPartition, P payload, Map<String, Object> headers);

		@Override
		public void start() {
			if (this.delegate instanceof Lifecycle) {
				((Lifecycle) this.delegate).start();
			}
		}

		@Override
		public void stop() {
			if (this.delegate instanceof Lifecycle) {
				((Lifecycle) this.delegate).stop();
			}
		}

		@Override
		public boolean isRunning() {
			if (this.delegate instanceof Lifecycle) {
				return ((Lifecycle) this.delegate).isRunning();
			}
			return false;
		}
	}
}
