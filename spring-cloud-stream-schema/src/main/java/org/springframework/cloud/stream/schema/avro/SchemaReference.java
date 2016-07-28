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

import org.springframework.util.Assert;

/**
 * References a schema through its subject and version.
 *
 * @author Marius Bogoevici
 */
public class SchemaReference {

	private String subject;

	private int version;

	public SchemaReference(String subject, int version) {
		Assert.hasText(subject, "cannot be empty");
		Assert.isTrue(version > 0, "must be a positive integer");
		this.subject = subject;
		this.version = version;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		Assert.hasText(subject, "cannot be empty");
		this.subject = subject;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		Assert.isTrue(version > 0, "must be a positive integer");
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SchemaReference that = (SchemaReference) o;

		if (version != that.version) {
			return false;
		}
		return subject.equals(that.subject);

	}

	@Override
	public int hashCode() {
		int result = subject.hashCode();
		result = 31 * result + version;
		return result;
	}
}
