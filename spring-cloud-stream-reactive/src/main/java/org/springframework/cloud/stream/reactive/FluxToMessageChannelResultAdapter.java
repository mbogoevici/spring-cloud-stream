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

package org.springframework.cloud.stream.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.stream.binding.StreamListenerResultAdapter;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link org.springframework.cloud.stream.binding.StreamListenerResultAdapter} from a {@link Flux}
 * return type to a bound {@link MessageChannel}.
 *
 * @author Marius Bogoevici
 */
public class FluxToMessageChannelResultAdapter
		implements StreamListenerResultAdapter<Flux<?>, MessageChannel>, SmartLifecycle {

	private final Log log = LogFactory.getLog(FluxToMessageChannelResultAdapter.class);

	private final Object lifecycleMonitor = new Object();

	private volatile ExecutorService executor;

	private volatile boolean autoStartup = true;

	private volatile boolean running;

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable runnable) {
		if (this.running) {
			synchronized (lifecycleMonitor) {
				if (this.running) {
					this.executor.shutdown();
					this.executor = null;
					this.running = false;
				}
				if (runnable != null) {
					runnable.run();
				}
			}
		}
	}

	@Override
	public void start() {
		if (!this.running) {
			synchronized (lifecycleMonitor) {
				if (!this.running) {
					this.executor = Executors.newSingleThreadExecutor();
				}
			}
		}
	}

	@Override
	public void stop() {
		this.stop(null);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public boolean supports(Class<?> resultType, Class<?> boundType) {
		return Flux.class.isAssignableFrom(resultType) && MessageChannel.class.isAssignableFrom(boundType);
	}

	public void adapt(Flux<?> streamListenerResult, MessageChannel boundElement) {
		streamListenerResult
				.doOnError(e -> this.log.error("Error while processing result", e))
				.retry()
				.onBackpressureBuffer()
				.subscribe(
						result -> {
							if (result instanceof Message<?>) {
								executor.submit(() -> boundElement.send((Message<?>) result));
							}
							else {
								executor.submit(() -> boundElement.send(MessageBuilder.withPayload(result).build()));
							}
						});
	}
}
