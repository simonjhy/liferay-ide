/*
 * Copyright (c) 2016 the original author or authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.liferay.ide.maven.ui.goals;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.gradleware.tooling.toolingmodel.OmniProjectTask;
import com.gradleware.tooling.toolingmodel.OmniTaskSelector;
import com.gradleware.tooling.toolingmodel.util.Maybe;

import java.util.List;

/**
 * Tree node in the {@link TaskView} representing a task group.
 */
public final class PhaseGroupNode {

    private static final String DEFAULT_NAME = "other";

    private final List<GoalNode> goalNodes;
    private final ProjectNode projectNode;
    private final String name;

    private PhaseGroupNode(ProjectNode projectNode, String name) {
        this.projectNode = Preconditions.checkNotNull(projectNode);
        this.name = Preconditions.checkNotNull(name);
        this.goalNodes = createTaskNodes(projectNode);
    }

    private List<GoalNode> createTaskNodes(ProjectNode projectNode) {
        List<GoalNode> taskNodes = Lists.newArrayList();
        for (OmniProjectTask projectTask : getProjectTasks()) {
            taskNodes.add(new ProjectTaskNode(projectNode, projectTask));
        }
        for (OmniTaskSelector taskSelector : getTaskSelectors()) {
            taskNodes.add(new TaskSelectorNode(projectNode, taskSelector));
        }
        return taskNodes;
    }

    private List<OmniProjectTask> getProjectTasks() {
        List<OmniProjectTask> projectTasks = Lists.newArrayList();
        for (OmniProjectTask projectTask : this.getProjectNode().getGradleProject().getProjectTasks()) {
            if (this.contains(projectTask)) {
                projectTasks.add(projectTask);
            }
        }
        return projectTasks;
    }

    private List<OmniTaskSelector> getTaskSelectors() {
        List<OmniTaskSelector> taskSelectors = Lists.newArrayList();
        for (OmniTaskSelector taskSelector : this.getProjectNode().getGradleProject().getTaskSelectors()) {
            if (this.contains(taskSelector)) {
                taskSelectors.add(taskSelector);
            }
        }
        return taskSelectors;
    }

    public ProjectNode getProjectNode() {
        return this.projectNode;
    }

    public String getName() {
        return this.name;
    }

    public boolean contains(OmniProjectTask projectTask) {
        return matches(projectTask.getGroup());
    }

    public boolean contains(OmniTaskSelector taskSelector) {
        return matches(taskSelector.getGroup());
    }

    private boolean matches(Maybe<String> group) {
        if (!group.isPresent()) {
            return isDefault();
        }
        String name = group.get();
        if (name == null) {
            return isDefault();
        } else {
            return normalizeGroupName(name).equals(this.name);
        }
    }

    public List<TaskNode> getTaskNodes() {
        return this.goalNodes;
    };

    private boolean isDefault() {
        return DEFAULT_NAME.equals(this.name);
    }

    @Override
    public String toString() {
        return "Task group '" + this.name + "'";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        PhaseGroupNode that = (PhaseGroupNode) other;
        return Objects.equal(this.projectNode, that.projectNode) && Objects.equal(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.projectNode, this.name);
    }

    public static PhaseGroupNode getDefault(ProjectNode projectNode) {
        return new PhaseGroupNode(projectNode, DEFAULT_NAME);
    }

    public static PhaseGroupNode forName(ProjectNode projectNode, Maybe<String> groupName) {
        Preconditions.checkNotNull(groupName);
        String name = null;
        if (groupName.isPresent()) {
            name = groupName.get();
        }
        if (name == null) {
            name = PhaseGroupNode.DEFAULT_NAME;
        }
        return new PhaseGroupNode(projectNode, normalizeGroupName(name));
    }

    private static String normalizeGroupName(String groupName) {
        //see https://issues.gradle.org/browse/GRADLE-3429
        return groupName.toLowerCase();
    }
}
