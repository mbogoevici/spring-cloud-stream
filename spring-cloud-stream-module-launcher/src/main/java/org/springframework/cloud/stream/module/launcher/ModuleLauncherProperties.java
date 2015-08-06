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

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Contains configuration properties for the module launcher.
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 */
@ConfigurationProperties
public class ModuleLauncherProperties {

	/**
	 * True if aggregating multiple modules when launched together
	 */
	private boolean aggregateOnLaunch;

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private File localRepository = new File(System.getProperty("user.home")
			+ File.separator + ".m2" + File.separator + "repository");

	/**
	 * Location of comma separated remote maven repositories from which modules will be downloaded, if not available locally.
	 */
	private String[] remoteRepositories = new String[] {"https://repo.spring.io/libs-snapshot"};

	public boolean isAggregateOnLaunch() {
		return aggregateOnLaunch;
	}

	public void setAggregateOnLaunch(boolean aggregateOnLaunch) {
		this.aggregateOnLaunch = aggregateOnLaunch;
	}

	public void setRemoteRepositories(String[] remoteRepositories) {
		this.remoteRepositories = remoteRepositories;
	}

	protected String[] getRemoteRepositories() {
		return remoteRepositories;
	}

	public void setLocalRepository(File localRepository) {
		this.localRepository = localRepository;
	}

	protected File getLocalRepository() {
		return localRepository;
	}

}
