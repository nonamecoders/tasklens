package org.jetbrains.plugins.tasklens.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.tasklens.model.ScheduledTaskInfo
import org.jetbrains.plugins.tasklens.scan.TaskFlowAnalyzer

@Service(Service.Level.PROJECT)
class TaskFlowProjectService(private val project: Project) {

    private var cachedResults: List<ScheduledTaskInfo> = emptyList()

    fun analyze(): List<ScheduledTaskInfo> {
        cachedResults = TaskFlowAnalyzer(project).analyze()
        return cachedResults
    }

    fun getCachedResults(): List<ScheduledTaskInfo> = cachedResults
}