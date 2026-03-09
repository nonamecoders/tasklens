package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import org.jetbrains.plugins.tasklens.model.ScheduledTaskInfo

class ScheduledMethodScanner(private val project: Project) {

    companion object {
        private const val SCHEDULED_FQN = "org.springframework.scheduling.annotation.Scheduled"
    }

    fun scan(): List<ScheduledTaskInfo> {
        val results = mutableListOf<ScheduledTaskInfo>()
        val psiManager = PsiManager.getInstance(project)
        val smartPointerManager = SmartPointerManager.getInstance(project)
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex

        fileIndex.iterateContent { vFile ->
            if (vFile.extension == "java" && fileIndex.isInSourceContent(vFile)) {
                val psiFile = psiManager.findFile(vFile) as? PsiJavaFile
                psiFile?.classes?.forEach { psiClass ->
                    psiClass.methods.forEach { method ->
                        val annotation = method.getAnnotation(SCHEDULED_FQN)
                        if (annotation != null) {
                            val cron = annotation.findAttributeValue("cron")?.text?.extractStringValue()
                            val fixedDelay = annotation.findAttributeValue("fixedDelay")?.text
                            val fixedRate = annotation.findAttributeValue("fixedRate")?.text
                            val fixedDelayString = annotation.findAttributeValue("fixedDelayString")?.text?.extractStringValue()
                            val fixedRateString = annotation.findAttributeValue("fixedRateString")?.text?.extractStringValue()

                            results.add(
                                ScheduledTaskInfo(
                                    className = psiClass.name ?: "Unknown",
                                    methodName = method.name,
                                    cron = cron?.takeIf { it.isNotBlank() },
                                    fixedDelay = (fixedDelay?.takeIf { it != "0" && it != "-1" && it.isNotBlank() }
                                        ?: fixedDelayString?.takeIf { it.isNotBlank() }),
                                    fixedRate = (fixedRate?.takeIf { it != "0" && it != "-1" && it.isNotBlank() }
                                        ?: fixedRateString?.takeIf { it.isNotBlank() }),
                                    serviceCalls = emptyList(),
                                    navigationElement = smartPointerManager.createSmartPsiElementPointer(method)
                                )
                            )
                        }
                    }
                }
            }
            true
        }

        return results
    }

    private fun String.extractStringValue(): String = trim().removeSurrounding("\"")
}