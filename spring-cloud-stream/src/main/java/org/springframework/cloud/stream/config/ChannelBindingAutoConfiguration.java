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

package org.springframework.cloud.stream.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.BinderType;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderFactory;
import org.springframework.cloud.stream.binder.DefaultBinderTypeRegistry;
import org.springframework.cloud.stream.binding.Bindable;
import org.springframework.cloud.stream.binding.ChannelBindingService;
import org.springframework.cloud.stream.endpoint.ChannelsEndpoint;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration class with some useful beans for {@link MessageChannel} binding and
 * general Spring Integration infrastructure.
 *
 * @author Dave Syer
 */
@Configuration
@ConditionalOnBean(ChannelBindingService.class)
@EnableConfigurationProperties(DefaultPollerProperties.class)
public class ChannelBindingAutoConfiguration {

	@Autowired
	private DefaultPollerProperties poller;

	@Bean(name = PollerMetadata.DEFAULT_POLLER)
	@ConditionalOnMissingBean(PollerMetadata.class)
	public PollerMetadata defaultPoller() {
		return this.poller.getPollerMetadata();
	}

	@Bean
	@Autowired(required=false)
	public ChannelsEndpoint channelsEndpoint(List<Bindable> adapters, ChannelBindingServiceProperties properties) {
		return new ChannelsEndpoint(adapters, properties);
	}

}
