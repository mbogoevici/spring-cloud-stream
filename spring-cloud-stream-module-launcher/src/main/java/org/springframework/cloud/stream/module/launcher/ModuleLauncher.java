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

package org.springframework.cloud.stream.module.launcher;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.ModuleJarLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.cloud.stream.module.utils.ClassloaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A component that launches one or more modules, delegating their resolution to an
 * underlying {@link ModuleResolver}.
 *
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
public class ModuleLauncher {

	public static final String MODULE_AGGREGATOR_CLASS = "org.springframework.cloud.stream.aggregate.ModuleAggregationUtils";

	public static final String MODULE_AGGREGATOR_METHOD = "runAggregated";

	public static final String SPRING_CLOUD_STREAM_ARG_PREFIX = "--spring.cloud.stream";

	private Log log = LogFactory.getLog(ModuleLauncher.class);

	private static final String DEFAULT_EXTENSION = "jar";

	private static final String DEFAULT_CLASSIFIER = "exec";

	private static final Pattern COORDINATES_PATTERN =
			Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

	private final ModuleResolver moduleResolver;

	private boolean aggregateOnLaunch = true;

	/**
	 * Creates a module launcher using the provided module resolver
	 * @param moduleResolver the module resolver instance to use
	 */
	public ModuleLauncher(ModuleResolver moduleResolver) {
		this.moduleResolver = moduleResolver;
	}

	public void setAggregateOnLaunch(boolean aggregateOnLaunch) {
		this.aggregateOnLaunch = aggregateOnLaunch;
	}

	/**
	 * Launches one or more modules, with the corresponding arguments, if any.
	 *
	 * The format of each module must conform to the <a href="http://www.eclipse.org/aether">Aether</a> convention:
	 * <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;</code>
	 *
	 * To pass arguments to a module, prefix with the module name and a dot. The arg name will be de-qualified and passed along.
	 * For example: <code>---Dorg.springframework.cloud.stream.module:time-source:1.0.0.BUILD-SNAPSHOT.bar=123</code> becomes <code>--bar=123</code> and is only passed to the 'org.springframework.cloud.stream.module:time-source:1.0.0.BUILD-SNAPSHOT' module.
	 *
	 * @param modules a list of modules
	 * @param args a list of arguments, prefixed with the module name
	 */
	public void launch(String[] modules, String[] args) {
		if (modules.length == 1 || !aggregateOnLaunch) {
			launchModulesIndividually(modules, args);
		}
		else {
			aggregateAndLaunchModules(modules, args);
		}
	}

	public void aggregateAndLaunchModules(String[] modules, final String args[]) {
		try {
			List<String> mainClassNames = new ArrayList<>();
			List<URL> jarURLs = new ArrayList<>();
			List<String> seenArchives = new ArrayList<>();
			final List<String[]> arguments = new ArrayList<>();
			// aggregate jars from all modules and extract their main Classes
			for (String module : modules) {
				Resource resource = resolveModule(module);
				JarFileArchive jarFileArchive = new JarFileArchive(resource.getFile());
				jarURLs.add(jarFileArchive.getUrl());
				for (Archive archive : jarFileArchive.getNestedArchives(ArchiveMatchingEntryFilter.FILTER)) {
					// avoid duplication based on unique JAR names
					// TODO - read the metadata from the JARs, do proper version resolution on merge
					String urlAsString = archive.getUrl().toString();
					String urlWithoutLastPart = urlAsString.substring(0,urlAsString.lastIndexOf("!/"));
					String jarName = urlWithoutLastPart.substring(urlWithoutLastPart.lastIndexOf("/") + 1);
					if (!seenArchives.contains(jarName)) {
						seenArchives.add(jarName);
						jarURLs.add(archive.getUrl());
					}
				}
				mainClassNames.add(jarFileArchive.getMainClass());
				List<String> filteredArgs = new ArrayList<>();
				for (String arg : args) {
					if (arg.startsWith("--" + module + ".")) {
						filteredArgs.add(arg.substring(module.length() + 3));
					}
				}
				arguments.add(filteredArgs.toArray(new String[filteredArgs.size()]));
			}
			final ClassLoader classLoader = new LaunchedURLClassLoader(jarURLs.toArray(new URL[0]),
					ClassloaderUtils.getExtensionClassloader());
			final List<Class<?>> mainClasses = new ArrayList<>();
			for (String mainClass : mainClassNames) {
				mainClasses.add(ClassUtils.forName(mainClass, classLoader));
			}
			// generic argument are passed directly to the aggregating parent
			// so that the launcher can configure the binder and binding settings
			final List<String> parentArguments = new ArrayList<>();
			for (String arg : args) {
				if (arg.startsWith(SPRING_CLOUD_STREAM_ARG_PREFIX)) {
					parentArguments.add(arg.substring(2));
				}
			}
			Runnable runner = new Runnable() {
				@Override
				public void run() {
					try {
						// we expect the class and method to be found on the module classpath
						Class<?> moduleAggregatorClass = ClassUtils.forName(MODULE_AGGREGATOR_CLASS, classLoader);
						Method aggregateMethod = ReflectionUtils.findMethod(moduleAggregatorClass, MODULE_AGGREGATOR_METHOD, String[].class, Class[].class, String[][].class);
						aggregateMethod.invoke(null,
								parentArguments.toArray(new String[parentArguments.size()]),
								mainClasses.toArray(new Class<?>[mainClasses.size()]) ,
								arguments.toArray(new String[][]{}));
					} catch (Exception e) {
						log.error("Cannot start module group ", e);
						throw new RuntimeException(e);
					}
				}
			};

			Thread runnerThread = new Thread(runner);
			runnerThread.setContextClassLoader(classLoader);
			runnerThread.setName(Thread.currentThread().getName());
			runnerThread.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void launchModulesIndividually(String[] modules, String[] args) {
		for (String module: modules) {
			List<String> moduleArgs = new ArrayList<>();
			for (String arg : args) {
				if (arg.startsWith("--" + module + ".")) {
					moduleArgs.add("--" + arg.substring(module.length() + 3));
				}
			}
			moduleArgs.add("--spring.jmx.default-domain=" + module.replace("/", ".").replace(":", "."));
			launchModule(module, moduleArgs.toArray(new String[moduleArgs.size()]));
		}
	}

	private void launchModule(String module, String[] args) {
		try {
			Resource resource = resolveModule(module);
			JarFileArchive jarFileArchive = new JarFileArchive(resource.getFile());
			ModuleJarLauncher jarLauncher = new ModuleJarLauncher(jarFileArchive);
			jarLauncher.launch(args);
		}
		catch (IOException e) {
			throw new RuntimeException("failed to launch module: " + module, e);
		}
	}

	private Resource resolveModule(String coordinates) {
		Matcher matcher = COORDINATES_PATTERN.matcher(coordinates);
		Assert.isTrue(matcher.matches(), "Bad artifact coordinates " + coordinates
				+ ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
		String groupId = matcher.group(1);
		String artifactId = matcher.group(2);
		String extension = StringUtils.hasLength(matcher.group(4)) ? matcher.group(4) : DEFAULT_EXTENSION;
		String classifier = StringUtils.hasLength(matcher.group(6)) ? matcher.group(6) : DEFAULT_CLASSIFIER;
		String version = matcher.group(7);
		return this.moduleResolver.resolve(groupId, artifactId, extension, classifier, version);
	}

}
