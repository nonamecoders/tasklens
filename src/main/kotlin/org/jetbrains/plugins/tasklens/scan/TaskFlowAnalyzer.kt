package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.tasklens.model.ScheduledTaskInfo

class TaskFlowAnalyzer(private val project: Project) {

    private val scheduledMethodScanner = ScheduledMethodScanner(project)
    private val serviceCallResolver = ServiceCallResolver(project)
    private val daoCallResolver = DaoCallResolver(project)

    fun analyze(): List<ScheduledTaskInfo> {
        val tasks = scheduledMethodScanner.scan()

        return tasks.map { task ->
            val method = task.navigationElement.element ?: return@map task
            val serviceCalls = serviceCallResolver.resolve(method).map { serviceCall ->
                val serviceMethod = serviceCall.navigationElement.element
                val daoCalls = if (serviceMethod != null) daoCallResolver.resolve(serviceMethod) else emptyList()
                serviceCall.copy(daoCalls = daoCalls)
            }
            task.copy(serviceCalls = serviceCalls)
        }
    }
}