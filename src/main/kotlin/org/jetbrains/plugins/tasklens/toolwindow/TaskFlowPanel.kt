package org.jetbrains.plugins.tasklens.toolwindow

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.plugins.tasklens.model.DaoCallInfo
import org.jetbrains.plugins.tasklens.model.MyBatisSqlInfo
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
    private val refreshButton = JButton("Refresh")
    private val projectService = project.service<TaskFlowProjectService>()

    init {
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
        refreshButton.isEnabled = false
        Thread {
            try {
                val tasks = ReadAction.compute<List<ScheduledTaskInfo>, Throwable> {
                    projectService.analyze()
                }
                SwingUtilities.invokeLater { updateTree(tasks) }
            } finally {
                SwingUtilities.invokeLater { refreshButton.isEnabled = true }
            }
        }.start()
    }

    private fun updateTree(tasks: List<ScheduledTaskInfo>) {
        val root = DefaultMutableTreeNode("Scheduled Tasks (${tasks.size})")

        for ((className, classTasks) in tasks.groupBy { it.className }) {
            val classNode = DefaultMutableTreeNode(
                NodeData.TaskClass(classTasks.first(), className)
            )

            for (task in classTasks) {
                val scheduleLabel = buildScheduleLabel(task)
                val taskNode = DefaultMutableTreeNode(
                    NodeData.Task(task, "${task.methodName} [$scheduleLabel]")
                )

                for (serviceCall in task.serviceCalls) {
                    val serviceNode = DefaultMutableTreeNode(
                        NodeData.Service(serviceCall, "Service: ${serviceCall.className}.${serviceCall.methodName}")
                    )

                    for (daoCall in serviceCall.daoCalls) {
                        val daoNode = DefaultMutableTreeNode(
                            NodeData.Dao(daoCall, "${daoCall.className}.${daoCall.methodName}()")
                        )
                        daoCall.mybatisSqlInfo?.let { sql ->
                            daoNode.add(
                                DefaultMutableTreeNode(
                                    NodeData.MyBatisSql(sql, "${sql.sqlType} ${sql.sqlId} [${sql.xmlFileName}]")
                                )
                            )
                        }
                        serviceNode.add(daoNode)
                    }

                    taskNode.add(serviceNode)
                }

                classNode.add(taskNode)
            }

            root.add(classNode)
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
            is NodeData.TaskClass -> data.task.navigationElement.element?.containingClass?.navigate(true)
            is NodeData.Task -> data.task.navigationElement.element?.navigate(true)
            is NodeData.Service -> data.serviceCall.navigationElement.element?.navigate(true)
            is NodeData.Dao -> data.daoCall.navigationElement.element?.navigate(true)
            is NodeData.MyBatisSql -> {
                val element = data.sqlInfo.navigationElement.element ?: return
                val vFile = element.containingFile?.virtualFile ?: return
                OpenFileDescriptor(project, vFile, element.textOffset).navigate(true)
            }
        }
    }

    private sealed class NodeData(val label: String) {
        override fun toString() = label

        class TaskClass(val task: ScheduledTaskInfo, label: String) : NodeData(label)
        class Task(val task: ScheduledTaskInfo, label: String) : NodeData(label)
        class Service(val serviceCall: ServiceCallInfo, label: String) : NodeData(label)
        class Dao(val daoCall: DaoCallInfo, label: String) : NodeData(label)
        class MyBatisSql(val sqlInfo: MyBatisSqlInfo, label: String) : NodeData(label)
    }
}