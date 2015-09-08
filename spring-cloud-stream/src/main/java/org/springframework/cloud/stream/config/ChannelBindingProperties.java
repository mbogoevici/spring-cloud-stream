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

import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderProperties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Dave Syer
 * @author Marius Bogoevici
 */
@ConfigurationProperties("spring.cloud.stream")
@JsonInclude(Include.NON_DEFAULT)
public class ChannelBindingProperties {

	public static final String TARGET = "target";

	public static final String PARTITIONED = "partitioned";

	@Value("${INSTANCE_INDEX:${CF_INSTANCE_INDEX:0}}")
	private int instanceIndex;

	@Value("${INSTANCE_COUNT:1}")
	private int instanceCount;

	@Value("${PARTITION_COUNT:1}")
	private int partitionCount;

	private Properties consumerProperties = new Properties();

	private Properties producerProperties = new Properties();

	private Map<String,Object> bindings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private Properties getConsumerProperties() {
		return this.consumerProperties;
	}

	public void setConsumerProperties(Properties consumerProperties) {
		this.consumerProperties = consumerProperties;
	}

	public Properties getProducerProperties() {
		return this.producerProperties;
	}

	public void setProducerProperties(Properties producerProperties) {
		this.producerProperties = producerProperties;
	}

	public Map<String, Object> getBindings() {
		return bindings;
	}

	public void setBindings(Map<String, Object> bindings) {
		this.bindings = bindings;
	}


	public int getInstanceIndex() {
		return instanceIndex;
	}

	public void setInstanceIndex(int instanceIndex) {
		this.instanceIndex = instanceIndex;
	}

	public int getInstanceCount() {
		return instanceCount;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public int getPartitionCount() {
		return partitionCount;
	}

	public void setPartitionCount(int partitionCount) {
		this.partitionCount = partitionCount;
	}

	public String getBindingTarget(String channelName) {
		Object binding = bindings.get(channelName);
		// we may shortcut directly to the path
		if (binding != null) {
			if (binding instanceof String) {
				return (String) binding;
			}
			else if (binding instanceof Map) {
				Map<?, ?> bindingProperties = (Map<?, ?>) binding;
				Object bindingPath = bindingProperties.get(TARGET);
				if (bindingPath != null) {
					return bindingPath.toString();
				}
			}
		}
		// just return the channel name if not found
		return channelName;
	}

	public boolean isPartitioned(String channelName) {
		Object binding = bindings.get(channelName);
		// if the setting is just a target shortcut
		if (binding == null || binding instanceof String) {
			return false;
		}
		else if (binding instanceof Map) {
			Map<?, ?> bindingProperties = (Map<?, ?>) binding;
			Object bindingPath = bindingProperties.get(PARTITIONED);
			if (bindingPath != null) {
				return Boolean.valueOf(bindingPath.toString());
			}
		}
		// just return the channel name if not found
		return false;
	}

	public String getTapChannelName(String channelName) {
		return "tap:" + getBindingTarget(channelName);
	}


	public Properties getConsumerProperties(String inputChannelName) {
		if (isPartitioned(inputChannelName)) {
			Properties channelConsumerProperties = new Properties();
			if (getConsumerProperties() == null) {
				channelConsumerProperties.putAll(getConsumerProperties());
			}
			channelConsumerProperties.put(BinderProperties.COUNT, getInstanceCount());
			channelConsumerProperties.put(BinderProperties.PARTITION_INDEX, getInstanceIndex());
			channelConsumerProperties.put(BinderProperties.PARTITIONED, true);
			return channelConsumerProperties;
		}
		else {
			return getConsumerProperties();
		}
	}

	public Properties getProducerProperties(String outputChannelName) {
		if (isPartitioned(outputChannelName)) {
			Properties channelProducerProperties = new Properties();
			if (getProducerProperties() == null) {
				channelProducerProperties.putAll(getConsumerProperties());
			}
			channelProducerProperties.put(BinderProperties.MIN_PARTITION_COUNT, getPartitionCount());
			channelProducerProperties.put(BinderProperties.NEXT_MODULE_COUNT, getPartitionCount());
			channelProducerProperties.put(BinderProperties.PARTITIONED, true);
			return channelProducerProperties;
		}
		else {
			return getProducerProperties();
		}
	}

}
