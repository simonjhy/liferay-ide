/*
 * Copyright (c) 2015 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Etienne Studer & Donát Csikós (Gradle Inc.) - initial API and implementation and initial documentation
 */

package com.liferay.ide.maven.ui.goals;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;

/**
 * Tree node in the {@link TaskView} representing a Gradle project.
 */
public final class ProjectNode extends BaseProjectNode {

    private final ProjectNode parentProjectNode;
    private final MavenProject mavenProject;
    private final boolean includedProject;


    public ProjectNode(ProjectNode parentProjectNode, MavenProject mavenProject, Optional<IProject> workspaceProject, boolean includedProject) {
        super(workspaceProject);
        this.parentProjectNode = parentProjectNode; // is null for root project
        this.mavenProject = Preconditions.checkNotNull(mavenProject);
        this.includedProject = includedProject;
    }

    public ProjectNode getRootProjectNode() {
        ProjectNode root = this;
        while (root.getParentProjectNode() != null) {
            root = root.getParentProjectNode();
        }
        return root;
    }

    public ProjectNode getParentProjectNode() {
        return this.parentProjectNode;
    }

    public MavenProject getMavenProject() {
        return this.mavenProject;
    }

    public boolean isIncludedProject() {
        return this.includedProject;
    }

    @Override
    public String toString() {
        return this.mavenProject.getName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ProjectNode that = (ProjectNode) other;
        return Objects.equal(this.parentProjectNode, that.parentProjectNode)
                && Objects.equal(this.mavenProject, that.mavenProject)
                && Objects.equal(this.includedProject, that.includedProject);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getWorkspaceProject(), this.parentProjectNode, this.mavenProject, this.includedProject);
    }
}
