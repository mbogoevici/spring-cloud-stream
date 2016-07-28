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

package org.springframework.cloud.stream.schema.avro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.StringConvertingContentTypeResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 * @author Vinicius Carvalho
 */
@Configuration
@ConditionalOnClass(name = "org.apache.avro.Schema")
@EnableConfigurationProperties(AvroMessageConverterProperties.class)
public class AvroMessageConverterAutoConfiguration {

	@Autowired
	private AvroMessageConverterProperties avroMessageConverterProperties;

	@Bean
	public AvroSchemaMessageConverter avroSchemaMessageConverter(SchemaRegistryClient schemaRegistryClient) {
		AvroSchemaMessageConverter avroSchemaMessageConverter = new AvroSchemaMessageConverter(schemaRegistryClient);
		avroSchemaMessageConverter.setDynamicSchemaGenerationEnabled(
				this.avroMessageConverterProperties.isDynamicSchemaGenerationEnabled());
		avroSchemaMessageConverter.setContentTypeResolver(new StringConvertingContentTypeResolver());
		if (this.avroMessageConverterProperties.getReaderSchema() != null) {
			avroSchemaMessageConverter.setReaderSchema(this.avroMessageConverterProperties.getReaderSchema());
		}
		if (StringUtils.hasText(this.avroMessageConverterProperties.getSchemaLocations())) {
			avroSchemaMessageConverter.setSchemaLocations(this.avroMessageConverterProperties.getSchemaLocations());
		}
		return avroSchemaMessageConverter;
	}
}
