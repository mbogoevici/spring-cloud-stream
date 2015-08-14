/*
 * Copyright 2015 the original author or authors.
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

package sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableModule;
import org.springframework.cloud.stream.annotation.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;

/**
 * @author Dave Syer
 *
 */
@EnableModule(Sink.class)
public class LogSink {

	private static Logger logger = LoggerFactory.getLogger(LogSink.class);

	@ServiceActivator(inputChannel=Sink.INPUT)
	public void loggerSink(Object payload) {
		logger.info("Received: " + payload);
	}

	@GlobalChannelInterceptor @Bean
	public ChannelInterceptor globalInterceptor() {
		return new ChannelInterceptorAdapter() {
			@Override
			public boolean preReceive(MessageChannel channel) {
				logger.info("PreReceive");
				return super.preReceive(channel);
			}

			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				logger.info("PreSend");
				return super.preSend(message, channel);
			}
		};
	}

}
