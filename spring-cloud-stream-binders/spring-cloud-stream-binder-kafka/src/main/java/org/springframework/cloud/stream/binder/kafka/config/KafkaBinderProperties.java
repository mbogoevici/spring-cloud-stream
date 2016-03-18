/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.KafkaMessageChannelBinder;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
@ConfigurationProperties(prefix = "spring.cloud.stream.binder.kafka")
public class KafkaBinderProperties {

	private boolean autoCommitOffset = true;

	private int batchTimeout = 0;

	private String[] brokers = new String[] {"localhost"};

	private int bufferSize = 16384;

	private ProducerMetadata.CompressionType compressionType = ProducerMetadata.CompressionType.none;

	private String defaultBrokerPort = "9092";

	private String defaultZkPort = "2181";

	private int fetchSize = 1024 * 1024;

	private String[] headers;

	private int maxWait = 100;

	private int minPartitionCount = 1;

	private KafkaMessageChannelBinder.Mode mode = KafkaMessageChannelBinder.Mode.embeddedHeaders;

	private int offsetUpdateCount = 0;

	private int offsetUpdateShutdownTimeout = 2000;

	private int offsetUpdateTimeWindow = 10000;

	private int queueSize = 8192;

	private int replicationFactor = 1;

	private int requiredAcks = 1;

	private boolean resetOffsets = false;

	private int socketBufferSize = 2 * 1024 * 1024;

	private KafkaMessageChannelBinder.StartOffset startOffset = null;

	private boolean syncProducer = false;

	/**
	 * ZK Connection timeout in milliseconds.
	 */
	private int zkConnectionTimeout = 10000;

	private String[] zkNodes = new String[] {"localhost"};

	/**
	 * ZK session timeout in milliseconds.
	 */
	private int zkSessionTimeout = 10000;

	public boolean isAutoCommitOffset() {
		return autoCommitOffset;
	}

	public void setAutoCommitOffset(boolean autoCommitOffset) {
		this.autoCommitOffset = autoCommitOffset;
	}

	public int getBatchTimeout() {
		return batchTimeout;
	}

	public void setBatchTimeout(int batchTimeout) {
		this.batchTimeout = batchTimeout;
	}

	public String[] getBrokers() {
		return brokers;
	}

	public void setBrokers(String[] brokers) {
		this.brokers = brokers;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public ProducerMetadata.CompressionType getCompressionType() {
		return compressionType;
	}

	public void setCompressionType(
			ProducerMetadata.CompressionType compressionType) {
		this.compressionType = compressionType;
	}

	public String getDefaultBrokerPort() {
		return defaultBrokerPort;
	}

	public void setDefaultBrokerPort(String defaultBrokerPort) {
		this.defaultBrokerPort = defaultBrokerPort;
	}

	public String getDefaultZkPort() {
		return defaultZkPort;
	}

	public void setDefaultZkPort(String defaultZkPort) {
		this.defaultZkPort = defaultZkPort;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public String[] getHeaders() {
		return headers;
	}

	public void setHeaders(String[] headers) {
		this.headers = headers;
	}

	public int getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}

	public int getMinPartitionCount() {
		return minPartitionCount;
	}

	public void setMinPartitionCount(int minPartitionCount) {
		this.minPartitionCount = minPartitionCount;
	}

	public KafkaMessageChannelBinder.Mode getMode() {
		return mode;
	}

	public void setMode(KafkaMessageChannelBinder.Mode mode) {
		this.mode = mode;
	}

	public int getOffsetUpdateCount() {
		return offsetUpdateCount;
	}

	public void setOffsetUpdateCount(int offsetUpdateCount) {
		this.offsetUpdateCount = offsetUpdateCount;
	}

	public int getOffsetUpdateShutdownTimeout() {
		return offsetUpdateShutdownTimeout;
	}

	public void setOffsetUpdateShutdownTimeout(int offsetUpdateShutdownTimeout) {
		this.offsetUpdateShutdownTimeout = offsetUpdateShutdownTimeout;
	}

	public int getOffsetUpdateTimeWindow() {
		return offsetUpdateTimeWindow;
	}

	public void setOffsetUpdateTimeWindow(int offsetUpdateTimeWindow) {
		this.offsetUpdateTimeWindow = offsetUpdateTimeWindow;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getReplicationFactor() {
		return replicationFactor;
	}

	public void setReplicationFactor(int replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	public int getRequiredAcks() {
		return requiredAcks;
	}

	public void setRequiredAcks(int requiredAcks) {
		this.requiredAcks = requiredAcks;
	}

	public boolean isResetOffsets() {
		return resetOffsets;
	}

	public void setResetOffsets(boolean resetOffsets) {
		this.resetOffsets = resetOffsets;
	}

	public int getSocketBufferSize() {
		return socketBufferSize;
	}

	public void setSocketBufferSize(int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public KafkaMessageChannelBinder.StartOffset getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(
			KafkaMessageChannelBinder.StartOffset startOffset) {
		this.startOffset = startOffset;
	}

	public boolean isSyncProducer() {
		return syncProducer;
	}

	public void setSyncProducer(boolean syncProducer) {
		this.syncProducer = syncProducer;
	}

	public int getZkConnectionTimeout() {
		return zkConnectionTimeout;
	}

	public void setZkConnectionTimeout(int zkConnectionTimeout) {
		this.zkConnectionTimeout = zkConnectionTimeout;
	}

	public String[] getZkNodes() {
		return zkNodes;
	}

	public void setZkNodes(String[] zkNodes) {
		this.zkNodes = zkNodes;
	}

	public int getZkSessionTimeout() {
		return zkSessionTimeout;
	}

	public void setZkSessionTimeout(int zkSessionTimeout) {
		this.zkSessionTimeout = zkSessionTimeout;
	}

	public String getZkConnectionString() {
		return toConnectionString(this.zkNodes, this.defaultZkPort);
	}

	public String getKafkaConnectionString() {
		return toConnectionString(this.brokers, this.defaultBrokerPort);
	}

	/**
	 * Converts an array of host values to a comma-separated String.
	 *
	 * It will append the default port value, if not already specified.
	 */
	private String toConnectionString(String[] hosts, String defaultPort) {
		String[] fullyFormattedHosts = new String[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i].contains(":") || StringUtils.isEmpty(defaultPort)) {
				fullyFormattedHosts[i] = hosts[i];
			}
			else {
				fullyFormattedHosts[i] = hosts[i] + ":" + defaultPort;
			}
		}
		return StringUtils.arrayToCommaDelimitedString(fullyFormattedHosts);
	}

}
