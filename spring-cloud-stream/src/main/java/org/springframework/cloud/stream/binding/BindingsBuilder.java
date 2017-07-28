/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.stream.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 */
public class BindingsBuilder {

	private final Map<String, BindingTargetFactory> bindingTargetFactories;

	private Map<String, Class<?>> inputs = new HashMap<>();

	private Map<String, Class<?>> outputs = new HashMap<>();

	public BindingsBuilder(Map<String, BindingTargetFactory> bindingTargetFactories) {
		this.bindingTargetFactories = bindingTargetFactories;
	}

	public BindingsBuilder withInputs(Map<String, Class<?>> inputs) {
		validateBindingsNotRegistered(inputs);
		this.inputs.putAll(inputs);
		return this;
	}

	public BindingsBuilder withInput(String name, Class<?> bindingType) {
		return this.withInputs(Collections.<String, Class<?>>singletonMap(name, bindingType));
	}

	public BindingsBuilder withOutputs(Map<String, Class<?>> outputs) {
		validateBindingsNotRegistered(outputs);
		this.outputs.putAll(outputs);
		return this;
	}

	public BindingsBuilder withOutput(String name, Class<?> bindingType) {
		return this.withOutputs(Collections.<String, Class<?>>singletonMap(name, bindingType));
	}

	private void validateBindingsNotRegistered(Map<String, Class<?>> inputs) {
		for (String name : inputs.keySet()) {
			Assert.isTrue(!this.inputs.containsKey(name), "An input for '" + name + "' has already been registered.");
			Assert.isTrue(!this.outputs.containsKey(name), "An output for '" + name + "' has already been registered.");
		}
	}

	private BindingTargetFactory getBindingTargetFactory(Class<?> bindingTargetType) {
		List<String> candidateBindingTargetFactories = new ArrayList<>();
		for (Map.Entry<String, BindingTargetFactory> bindingTargetFactoryEntry : this.bindingTargetFactories
				.entrySet()) {
			if (bindingTargetFactoryEntry.getValue().canCreate(bindingTargetType)) {
				candidateBindingTargetFactories.add(bindingTargetFactoryEntry.getKey());
			}
		}
		if (candidateBindingTargetFactories.size() == 1) {
			return this.bindingTargetFactories.get(candidateBindingTargetFactories.get(0));
		}
		else {
			if (candidateBindingTargetFactories.size() == 0) {
				throw new IllegalStateException("No factory found for binding target type: "
						+ bindingTargetType.getName() + " among registered factories: "
						+ StringUtils.collectionToCommaDelimitedString(bindingTargetFactories.keySet()));
			}
			else {
				throw new IllegalStateException(
						"Multiple factories found for binding target type: " + bindingTargetType.getName() + ": "
								+ StringUtils.collectionToCommaDelimitedString(candidateBindingTargetFactories));
			}
		}
	}

	public class ReferenceHoldingBindable implements Bindable {

		private final Map<String, ?> inputs;

		private final Map<String, ?> outputs;

		public ReferenceHoldingBindable(Map<String, ?> inputs, Map<String, ?> outputs) {
			this.inputs = inputs;
			this.outputs = outputs;
		}

		public <T> T getInput(String name, Class<T> inputType) {
			return (T) this.inputs.get(name);
		}

		public <T> T getOutput(String name, Class<T> outputType) {
			return (T) this.outputs.get(name);
		}

		@Override
		public void bindInputs(BindingService bindingService) {
			for (Map.Entry<String, ?> inputEntry : inputs.entrySet()) {
				bindingService.bindConsumer(inputEntry.getValue(), inputEntry.getKey());
			}
		}

		@Override
		public void bindOutputs(BindingService bindingService) {
			for (Map.Entry<String, ?> outputEntry : outputs.entrySet()) {
				bindingService.bindProducer(outputEntry.getValue(), outputEntry.getKey());
			}
		}

		@Override
		public void unbindInputs(BindingService bindingService) {
			for (Map.Entry<String, ?> inputEntry : inputs.entrySet()) {
				bindingService.unbindConsumers(inputEntry.getKey());
			}
		}

		@Override
		public void unbindOutputs(BindingService bindingService) {
			for (Map.Entry<String, ?> outputEntry : outputs.entrySet()) {
				bindingService.unbindProducers(outputEntry.getKey());
			}
		}

		@Override
		public Set<String> getInputs() {
			return inputs.keySet();
		}

		@Override
		public Set<String> getOutputs() {
			return outputs.keySet();
		}
	}

	public ReferenceHoldingBindable build() {
		Map<String, Object> inputInstances = new HashMap<>();
		for (Map.Entry<String, Class<?>> inputEntry : inputs.entrySet()) {
			inputInstances.put(inputEntry.getKey(),
					getBindingTargetFactory(inputEntry.getValue()).createInput(inputEntry.getKey()));
		}
		Map<String, Object> outputInstances = new HashMap<>();
		for (Map.Entry<String, Class<?>> outputEntry : outputs.entrySet()) {
			outputInstances.put(outputEntry.getKey(),
					getBindingTargetFactory(outputEntry.getValue()).createOutput(outputEntry.getKey()));
		}
		return new ReferenceHoldingBindable(inputInstances, outputInstances);
	}
}
