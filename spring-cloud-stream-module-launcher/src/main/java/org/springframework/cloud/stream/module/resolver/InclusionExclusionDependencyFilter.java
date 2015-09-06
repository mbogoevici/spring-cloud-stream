/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.module.resolver;

import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;

import org.springframework.util.ObjectUtils;

/**
 * @author Marius Bogoevici
 */
public class InclusionExclusionDependencyFilter implements DependencyFilter {

	private final PatternExclusionsDependencyFilter patternExclusionsDependencyFilter;

	private Artifact[] includes;

	public InclusionExclusionDependencyFilter(Artifact[] includes, String... excludes) {
		this.patternExclusionsDependencyFilter = new PatternExclusionsDependencyFilter(excludes);
		this.includes = includes != null ? includes : new Artifact[0];
	}

	@Override
	public boolean accept(DependencyNode node, List<DependencyNode> parents) {
		if (excludedAndNotIncludedDirectly(node, parents)) {
			return false;
		}
		else {
			// check if one of its parents needs to be excluded directly
			// TODO: check if this actually necessary. Traversal rules may prevent actually getting here
			for (DependencyNode parent : parents) {
				if (excludedAndNotIncludedDirectly(parent, null)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean excludedAndNotIncludedDirectly(DependencyNode node,
			List<DependencyNode> parents) {
		// nodes included explicitly are always accepted
		if (isIncludedDirectly(node)) {
			return false;
		}
		return !patternExclusionsDependencyFilter.accept(node, parents);
	}

	private boolean isIncludedDirectly(DependencyNode node) {
		for (Artifact include : includes) {
			Artifact nodeArtifact = node.getArtifact();
			// we check if this was a specifically included artifact by checking its group, artifactId, extension and
			// classifier. The version is left out in the case when resolution produces a different artifact version
			// (there cannot be two artifacts with the same group, artifactId, extension and classifier but different
			// version in the resolved group)
			// TODO: validate this strategy
			if (ObjectUtils.nullSafeEquals(include.getGroupId(), nodeArtifact.getGroupId())
					&& ObjectUtils.nullSafeEquals(include.getArtifactId(), nodeArtifact.getArtifactId())
					&& ObjectUtils.nullSafeEquals(include.getClassifier(), nodeArtifact.getClassifier())
					&& ObjectUtils.nullSafeEquals(include.getExtension(), nodeArtifact.getExtension())) {
				return true;
			}
		}
		return false;
	}
}
