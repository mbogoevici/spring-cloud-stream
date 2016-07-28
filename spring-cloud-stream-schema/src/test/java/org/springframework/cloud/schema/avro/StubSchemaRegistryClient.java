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

package org.springframework.cloud.schema.avro;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.avro.Schema;

import org.springframework.cloud.stream.schema.avro.AvroSchemaMessageConverter;
import org.springframework.cloud.stream.schema.avro.SchemaNotFoundException;
import org.springframework.cloud.stream.schema.avro.SchemaReference;
import org.springframework.cloud.stream.schema.avro.SchemaRegistrationResponse;
import org.springframework.cloud.stream.schema.avro.SchemaRegistryClient;

/**
 * @author Marius Bogoevici
 */
public class StubSchemaRegistryClient implements SchemaRegistryClient {

	private Map<String, Map<Integer, Schema>> storedSchemas = new HashMap<>();

	@Override
	public SchemaRegistrationResponse register(String subject, Schema schema) {
		if (!this.storedSchemas.containsKey(subject)) {
			this.storedSchemas.put(subject, new TreeMap<Integer, Schema>());
		}
		Map<Integer, Schema> schemaVersions = this.storedSchemas.get(subject);
		for (Map.Entry<Integer, Schema> integerSchemaEntry : schemaVersions.entrySet()) {

			if (integerSchemaEntry.getValue().equals(schema)) {
				SchemaRegistrationResponse schemaRegistrationResponse = new SchemaRegistrationResponse();
				schemaRegistrationResponse.setId(0);
				schemaRegistrationResponse.setSchemaReference(
						new SchemaReference(subject, integerSchemaEntry.getKey(),
								AvroSchemaMessageConverter.AVRO_FORMAT));
				return schemaRegistrationResponse;
			}
		}
		int nextVersion = schemaVersions.size() + 1;
		schemaVersions.put(nextVersion, schema);
		SchemaRegistrationResponse schemaRegistrationResponse = new SchemaRegistrationResponse();
		schemaRegistrationResponse.setId(0);
		schemaRegistrationResponse.setSchemaReference(
				new SchemaReference(subject, nextVersion, AvroSchemaMessageConverter.AVRO_FORMAT));
		return schemaRegistrationResponse;
	}

	@Override
	public Schema fetch(SchemaReference schemaReference) {
		if (!AvroSchemaMessageConverter.AVRO_FORMAT.equals(schemaReference.getFormat())) {
			throw new IllegalArgumentException("Only 'avro' is supported by this client");
		}
		if (!this.storedSchemas.containsKey(schemaReference.getSubject())) {
			throw new SchemaNotFoundException("Not found: " + schemaReference);
		}
		if (!this.storedSchemas.get(schemaReference.getSubject()).containsKey(schemaReference.getVersion())) {
			throw new SchemaNotFoundException("Not found: " + schemaReference);
		}
		return this.storedSchemas.get(schemaReference.getSubject()).get(schemaReference.getVersion());
	}
}
