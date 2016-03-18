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

package org.springframework.cloud.stream.binder.rabbit.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * @author David Turanski
 */
@ConfigurationProperties(prefix = "spring.cloud.stream.binder.rabbit")
public class RabbitBinderProperties {

	private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO;

	private String[] addresses = new String[0];

	private String[] adminAdresses = new String[0];

	private boolean autoBindDlq = false;

	private int batchBufferLimit = 10000;

	private int batchSize = 100;

	private int batchTimeout = 5000;

	private boolean batchingEnabled = false;

	private boolean compress = false;

	private int compressionLevel;

	private MessageDeliveryMode deliveryMode = MessageDeliveryMode.PERSISTENT;

	private boolean durableSubscription = true;

	private int maxConcurrency = 1;

	private String[] nodes = new String[0];

	private String password;

	private int prefetch = 1;

	private String prefix = "";

	private boolean republishToDlq = false;

	private String[] requestHeaderPatterns = new String[] {"STANDARD_REQUEST_HEADERS", "*"};

	private String[] replyHeaderPatterns = new String[] {"STANDARD_REPLY_HEADERS", "*"};

	private boolean requeueRejected = true;

	private Resource sslPropertiesLocation;

	private boolean transacted = false;

	private int txSize = 1;

	private boolean useSSL;

	private String username;

	private String vhost;

	public AcknowledgeMode getAcknowledgeMode() {
		return acknowledgeMode;
	}

	public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public String[] getAddresses() {
		return addresses;
	}

	public void setAddresses(String[] addresses) {
		this.addresses = addresses;
	}

	public String[] getAdminAdresses() {
		return adminAdresses;
	}

	public void setAdminAdresses(String[] adminAdresses) {
		this.adminAdresses = adminAdresses;
	}

	public boolean isAutoBindDlq() {
		return autoBindDlq;
	}

	public void setAutoBindDlq(boolean autoBindDlq) {
		this.autoBindDlq = autoBindDlq;
	}

	public int getBatchBufferLimit() {
		return batchBufferLimit;
	}

	public void setBatchBufferLimit(int batchBufferLimit) {
		this.batchBufferLimit = batchBufferLimit;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getBatchTimeout() {
		return batchTimeout;
	}

	public void setBatchTimeout(int batchTimeout) {
		this.batchTimeout = batchTimeout;
	}

	public boolean isBatchingEnabled() {
		return batchingEnabled;
	}

	public void setBatchingEnabled(boolean batchingEnabled) {
		this.batchingEnabled = batchingEnabled;
	}

	public boolean isCompress() {
		return compress;
	}

	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	public int getCompressionLevel() {
		return compressionLevel;
	}

	public void setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
	}

	public MessageDeliveryMode getDeliveryMode() {
		return deliveryMode;
	}

	public void setDeliveryMode(MessageDeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	public boolean isDurableSubscription() {
		return durableSubscription;
	}

	public void setDurableSubscription(boolean durableSubscription) {
		this.durableSubscription = durableSubscription;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public String[] getNodes() {
		return nodes;
	}

	public void setNodes(String[] nodes) {
		this.nodes = nodes;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getPrefetch() {
		return prefetch;
	}

	public void setPrefetch(int prefetch) {
		this.prefetch = prefetch;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public boolean isRepublishToDlq() {
		return republishToDlq;
	}

	public void setRepublishToDlq(boolean republishToDlq) {
		this.republishToDlq = republishToDlq;
	}

	public String[] getReplyHeaderPatterns() {
		return replyHeaderPatterns;
	}

	public void setReplyHeaderPatterns(String[] replyHeaderPatterns) {
		this.replyHeaderPatterns = replyHeaderPatterns;
	}

	public String[] getRequestHeaderPatterns() {
		return requestHeaderPatterns;
	}

	public void setRequestHeaderPatterns(String[] requestHeaderPatterns) {
		this.requestHeaderPatterns = requestHeaderPatterns;
	}

	public boolean isRequeueRejected() {
		return requeueRejected;
	}

	public void setRequeueRejected(boolean requeueRejected) {
		this.requeueRejected = requeueRejected;
	}

	public Resource getSslPropertiesLocation() {
		return sslPropertiesLocation;
	}

	public void setSslPropertiesLocation(Resource sslPropertiesLocation) {
		this.sslPropertiesLocation = sslPropertiesLocation;
	}

	public boolean isTransacted() {
		return transacted;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	public int getTxSize() {
		return txSize;
	}

	public void setTxSize(int txSize) {
		this.txSize = txSize;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getVhost() {
		return vhost;
	}

	public void setVhost(String vhost) {
		this.vhost = vhost;
	}
}
