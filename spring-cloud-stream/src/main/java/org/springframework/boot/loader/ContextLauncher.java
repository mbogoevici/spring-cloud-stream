/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * @author Marius Bogoevici
 */
public class ContextLauncher extends ExecutableArchiveLauncher {

	private static final String SPRING_APPLICATION = "org.springframework.boot.SpringApplication";

	private static final AsciiBytes LIB = new AsciiBytes("lib/");


	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		return !entry.isDirectory() && entry.getName().startsWith(LIB);
	}

	public static void main(String[] args) throws Exception {
		new ContextLauncher().launchContext(args);
	}

	@Override
	protected void launch(final String[] args, final String mainClass, final ClassLoader classLoader) throws Exception {
		final Class<?> springApplicationClass = classLoader.loadClass(SPRING_APPLICATION);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
					try {
						Constructor<?> constructor = springApplicationClass.getConstructor(Class[].class);
						Object springApplication = constructor.newInstance(classLoader.loadClass(mainClass));
						Method run = springApplicationClass.getMethod("run", String[].class);
						run.invoke(springApplication,args);
					} finally {
						Thread.currentThread().setContextClassLoader(originalClassloader);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).run();

	}

	private void launchContext(String[] args) throws Exception {
		new SpringApplication(this.getArchive().getManifest().getAttributes("Context-Class")).run(args);
	}
}
