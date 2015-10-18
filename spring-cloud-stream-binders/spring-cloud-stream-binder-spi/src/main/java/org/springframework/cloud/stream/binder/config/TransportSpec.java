package org.springframework.cloud.stream.binder.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.stream.binder")
public class TransportSpec {

	private Map<String, TransportConfiguration> transports = new HashMap<>();
	
	public Map<String, TransportConfiguration> getTransports() {
		return transports;
	}

	public void setTransports(Map<String, TransportConfiguration> transports) {
		this.transports = transports;
	}

	public static class TransportConfiguration {
		
		private String type;
		
		private Properties properties;

		public String getType() {
			return type;
		}

		public void setType(String name) {
			this.type = name;
		}

		public Properties getProperties() {
			return properties;
		}

		public void setProperties(Properties properties) {
			this.properties = properties;
		}
		
	}
}
