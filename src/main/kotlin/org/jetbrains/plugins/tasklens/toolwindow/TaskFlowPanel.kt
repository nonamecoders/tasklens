package org.jetbrains.plugins.tasklens.toolwindow

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.plugins.tasklens.model.DaoCallInfo
import org.jetbrains.plugins.tasklens.model.ScheduledTaskInfo
import org.jetbrains.plugins.tasklens.model.ServiceCallInfo
import org.jetbrains.plugins.tasklens.service.TaskFlowProjectService
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class TaskFlowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val tree = Tree()
    private val projectService = project.service<TaskFlowProjectService>()

    init {
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { refresh() }

        tree.isRootVisible = true
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) navigateToSelectedNode()
            }
        })

        add(refreshButton, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)

        updateTree(emptyList())
    }

    fun refresh() {
        val button = (getComponent(0) as JButton).also { it.isEnabled = false }
        Thread {
            try {
                val tasks = ReadAction.compute<List<ScheduledTaskInfo>, Throwable> {
                    projectService.analyze()
                }
                SwingUtilities.invokeLater { updateTree(tasks) }
            } finally {
                SwingUtilities.invokeLater { button.isEnabled = true }
            }
        }.start()
    }

    private fun updateTree(tasks: List<ScheduledTaskInfo>) {
        val root = DefaultMutableTreeNode("Scheduled Tasks (${tasks.size})")

        for (task in tasks) {
            val scheduleLabel = buildScheduleLabel(task)
            val taskNode = DefaultMutableTreeNode(
                TaskNodeData(task, "${task.className}.${task.methodName} [$scheduleLabel]")
            )

            for (serviceCall in task.serviceCalls) {
                val serviceNode = DefaultMutableTreeNode(
                    ServiceNodeData(serviceCall, "Service: ${serviceCall.className}.${serviceCall.methodName}")
                )

                for (daoCall in serviceCall.daoCalls) {
                    serviceNode.add(
                        DefaultMutableTreeNode(
                            DaoNodeData(daoCall, "DAO: ${daoCall.className}.${daoCall.methodName}")
                        )
                    )
                }

                taskNode.add(serviceNode)
            }

            root.add(taskNode)
        }

        tree.model = DefaultTreeModel(root)
        expandAll()
    }

    private fun buildScheduleLabel(task: ScheduledTaskInfo): String = when {
        task.cron != null -> "cron: ${task.cron}"
        task.fixedDelay != null -> "fixedDelay: ${task.fixedDelay}"
        task.fixedRate != null -> "fixedRate: ${task.fixedRate}"
        else -> "no schedule"
    }

    private fun expandAll() {
        var i = 0
        while (i < tree.rowCount) {
            tree.expandRow(i++)
        }
    }

    private fun navigateToSelectedNode() {
        val path = tree.selectionPath ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return

        when (val data = node.userObject) {
            is TaskNodeData -> data.task.navigationElement.element?.navigate(true)
            is ServiceNodeData -> data.serviceCall.navigationElement.element?.navigate(true)
            is DaoNodeData -> data.daoCall.navigationElement.element?.navigate(true)
        }
    }

    private data class TaskNodeData(val task: ScheduledTaskInfo, val label: String) {
        override fun toString() = label
    }

    private data class ServiceNodeData(val serviceCall: ServiceCallInfo, val label: String) {
        override fun toString() = label
    }

    private data class DaoNodeData(val daoCall: DaoCallInfo, val label: String) {
        override fun toString() = label
    }
}