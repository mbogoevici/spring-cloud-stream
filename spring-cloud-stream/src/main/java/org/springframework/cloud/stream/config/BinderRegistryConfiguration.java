/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderConfigurationRegistry;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderConfigurationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;

/**
 * @author Marius Bogoevici
 */
@Configuration
public class BinderRegistryConfiguration {

	@Bean
	@ConditionalOnMissingBean(BinderConfigurationRegistry.class)
	@ConditionalOnPrefix("spring.cloud.stream.binders")
	public BinderConfigurationRegistry binderConfigurationRegistry(BinderTypeRegistry binderTypeRegistry,
	                                                               ChannelBindingServiceProperties channelBindingServiceProperties) {
		Map<String, BinderConfiguration> binderConfigurations = new HashMap<>();
		for (Map.Entry<String, BinderProperties> binderEntry :
				channelBindingServiceProperties.getBinders().entrySet()) {
			BinderProperties binderProperties = binderEntry.getValue();
			if (binderTypeRegistry.get(binderEntry.getKey()) != null) {
				binderConfigurations.put(binderEntry.getKey(),
						new BinderConfiguration(binderTypeRegistry.get(binderEntry.getKey()),
								binderProperties.getProperties()));
			}
			else {
				Assert.hasText(binderProperties.getType(), "No 'type' property present for custom " +
						"binder " + binderEntry.getKey());
				binderConfigurations.put(binderEntry.getKey(),
						new BinderConfiguration(binderTypeRegistry.get(binderProperties.getType()),
								binderProperties.getProperties()));
			}
		}
		return new DefaultBinderConfigurationRegistry(binderConfigurations);
	}

	@Conditional(PrefixCondition.class)
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface ConditionalOnPrefix {

		String value() default "";

		boolean present() default true;
	}

	public static class PrefixCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String configuredPrefix = (String) metadata
					.getAnnotationAttributes(ConditionalOnPrefix.class.getName())
					.get("value");
			Boolean checkForPresence = (Boolean) metadata
					.getAnnotationAttributes(ConditionalOnPrefix.class.getName())
					.get("checkForPresence");
			if (checkForPresence == null) {
				checkForPresence = false;
			}
			String prefix = context.getEnvironment().resolvePlaceholders(configuredPrefix);
			if (!ConfigurableEnvironment.class.isAssignableFrom(context.getEnvironment().getClass())) {
				return ConditionOutcome.noMatch("Cannot scan environment, not a ConfigurableEnvironment instance");
			}
			ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();
			for (PropertySource<?> propertySource : environment.getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					for (String propertyName : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
						if (propertyName.startsWith(prefix)) {
							String message = "Found: " + propertyName;
							return checkForPresence ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
						}
					}
				}
			}
			String message = "Not found";
			return checkForPresence ? ConditionOutcome.noMatch(message) : ConditionOutcome.match(message);
		}
	}
}


