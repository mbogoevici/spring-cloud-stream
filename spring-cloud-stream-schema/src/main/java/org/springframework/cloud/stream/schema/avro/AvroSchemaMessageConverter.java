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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javafx.binding.StringFormatter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericContainer;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 * @author Vinicius Carvalho
 */
public class AvroSchemaMessageConverter extends AbstractMessageConverter implements ApplicationContextAware,
		InitializingBean {

	public static final String AVRO_FORMAT = "avro";

	private static Pattern SCHEMA_WITH_VERSION = Pattern.compile(
			"application/vnd\\.([\\p{Alnum}\\$\\.]+)\\.v(\\p{Digit}+)\\+avro");

	private ApplicationContext applicationContext;

	private boolean dynamicSchemaGenerationEnabled;

	private Map<String, Schema> localSchemaMap = new HashMap<>();

	private Schema readerSchema;

	private String schemaLocations;

	private SchemaRegistryClient schemaRegistryClient;

	public AvroSchemaMessageConverter(SchemaRegistryClient schemaRegistryClient) {
		super(Arrays.asList(new MimeType("application", "avro"), new MimeType("application", "*+avro")));
		Assert.notNull(schemaRegistryClient, "cannot be null");
		this.schemaRegistryClient = schemaRegistryClient;
	}

	public void setDynamicSchemaGenerationEnabled(boolean dynamicSchemaGenerationEnabled) {
		this.dynamicSchemaGenerationEnabled = dynamicSchemaGenerationEnabled;
	}

	public boolean isDynamicSchemaGenerationEnabled() {
		return this.dynamicSchemaGenerationEnabled;
	}

	public void setSchemaLocations(String schemaLocations) {
		Assert.hasText(schemaLocations, "cannot be empty");
		this.schemaLocations = schemaLocations;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.hasText(this.schemaLocations)) {
			this.logger.info("Scanning avro schema resources on classpath");
			Schema.Parser parser = new Schema.Parser();
			try {
				Resource[] resources = this.applicationContext.getResources(this.schemaLocations);
				if (this.logger.isInfoEnabled()) {
					this.logger.info(StringFormatter.format("Found %d schemas on classpath", resources.length));
				}
				for (Resource r : resources) {
					Schema schema = parser.parse(r.getInputStream());
					this.logger.info(StringFormatter
							.format("Resource %schema parsed into schema %schema.%schema", r.getFilename(),
									schema.getNamespace(),
									schema.getName()));
					this.schemaRegistryClient.register(toSubject(schema), schema);
					this.logger.info("Schema " + schema.getName() + " registered with id " + schema);
					this.localSchemaMap.put(schema.getNamespace() + "." + schema.getName(), schema);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected String toSubject(Schema schema) {
		return schema.getFullName().toLowerCase();
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// we support all types
		return true;
	}

	@Override
	protected boolean supportsMimeType(MessageHeaders headers) {
		if (super.supportsMimeType(headers)) {
			return true;
		}
		MimeType mimeType = getContentTypeResolver().resolve(headers);
		return (MimeType.valueOf("application/avro").includes(mimeType)
				|| MimeType.valueOf("application/*+avro").includes(mimeType));
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			MimeType expectedFinalContentType = null;
			if (conversionHint instanceof MimeType) {
				expectedFinalContentType = (MimeType) conversionHint;
			}
			Schema schema;
			SchemaReference schemaReference = getSchemaReference(expectedFinalContentType);
			// the mimeType does not contain a schema reference
			if (schemaReference == null) {
				schema = extractSchemaForWriting(payload);
				SchemaRegistrationResponse schemaRegistrationResponse = this.schemaRegistryClient.register(
						toSubject(schema), schema);
				schemaReference = schemaRegistrationResponse.getSchemaReference();
			}
			else {
				schema = this.schemaRegistryClient.fetch(schemaReference);
			}

			DatumWriter<Object> writer = getDatumWriter(payload.getClass(), schema);
			Encoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
			writer.write(payload, encoder);
			encoder.flush();
			if (headers instanceof MutableMessageHeaders) {
				headers.put(MessageHeaders.CONTENT_TYPE,
						"application/vnd." + schemaReference.getSubject() + ".v" + schemaReference
								.getVersion() + "+avro");
			}
		}
		catch (IOException e) {
			return null;
		}
		return baos.toByteArray();
	}

	private SchemaReference getSchemaReference(MimeType mimeType) {
		SchemaReference schemaReference = null;
		Matcher schemaMatcher = SCHEMA_WITH_VERSION.matcher(mimeType.toString());
		if (schemaMatcher.find()) {
			String subject = schemaMatcher.group(1);
			Integer version = Integer.parseInt(schemaMatcher.group(2));
			schemaReference = new SchemaReference(subject, version, AVRO_FORMAT);
		}
		return schemaReference;
	}

	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return super.canConvertFrom(message, targetClass) && (message.getPayload() instanceof byte[]);
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		try {
			byte[] payload = (byte[]) message.getPayload();
			ByteBuffer buf = ByteBuffer.wrap(payload);
			MimeType mimeType = getContentTypeResolver().resolve(message.getHeaders());
			if (mimeType == null) {
				if (conversionHint instanceof MimeType) {
					mimeType = (MimeType) conversionHint;
				}
				else {
					return null;
				}
			}
			buf.get(payload);
			SchemaReference schemaReference = getSchemaReference(mimeType);
			if (schemaReference == null) {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Cannot extract schema reference from " + mimeType);
				}
				return null;
			}
			Schema schema = this.schemaRegistryClient.fetch(schemaReference);
			DatumReader<Object> reader = getDatumReader(targetClass, schema);
			Decoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
			return reader.read(null, decoder);
		}
		catch (IOException e) {
			return null;
		}
	}

	private DatumWriter<Object> getDatumWriter(Class<?> type, Schema schema) {
		DatumWriter<Object> writer = null;
		this.logger.debug("Finding correct DatumWriter for type " + type.getName());
		if (SpecificRecord.class.isAssignableFrom(type)) {
			writer = new SpecificDatumWriter<>(schema);
		}
		else if (GenericRecord.class.isAssignableFrom(type)) {
			writer = new GenericDatumWriter<>(schema);
		}
		else {
			writer = new ReflectDatumWriter<>(schema);
		}
		this.logger.debug("DatumWriter of type " + writer.getClass().getName() + " selected");
		return writer;
	}

	private DatumReader<Object> getDatumReader(Class<?> type, Schema writer) {
		DatumReader<Object> reader = null;
		if (SpecificRecord.class.isAssignableFrom(type)) {
			reader = new SpecificDatumReader<>(writer, getReaderSchema(writer));
		}
		else if (GenericRecord.class.isAssignableFrom(type)) {
			reader = new GenericDatumReader<>(writer, getReaderSchema(writer));
		}
		else {
			reader = new ReflectDatumReader<>((Class<Object>) type);
			reader.setSchema(getReaderSchema(writer));
		}
		return reader;
	}


	private Schema getReaderSchema(Schema writerSchema) {
		return this.readerSchema != null ? this.readerSchema : writerSchema;
	}

	public void setReaderSchema(Resource readerSchema) {
		Assert.notNull(readerSchema, "cannot be null");
		try {
			this.readerSchema = new Schema.Parser().parse(readerSchema.getInputStream());
		}
		catch (IOException e) {
			throw new BeanInitializationException("Cannot initialize reader schema", e);
		}
	}

	private Schema extractSchemaForWriting(Object payload) {
		Schema schema = null;
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Obtaining schema for class " + payload.getClass());
		}
		if (GenericContainer.class.isAssignableFrom(payload.getClass())) {
			schema = ((GenericContainer) payload).getSchema();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Avro type detected, using schema from object");
			}
		}
		else {
			schema = this.localSchemaMap.get(payload.getClass().getName());
			if (schema == null) {
				if (!isDynamicSchemaGenerationEnabled()) {
					throw new SchemaNotFoundException(
							String.format("No schema found in the local cache for %s, and dynamic schema generation " +
									"is not enabled", payload.getClass()));
				}
				else {
					schema = ReflectData.get().getSchema(payload.getClass());
					this.schemaRegistryClient.register(toSubject(schema), schema);
				}
				this.localSchemaMap.put(payload.getClass().getName(), schema);
			}
		}
		return schema;
	}
}
