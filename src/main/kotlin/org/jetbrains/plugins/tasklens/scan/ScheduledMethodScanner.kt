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
        private const val UNSET_NUMERIC = "0"
        private const val DISABLED_NUMERIC = "-1"
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
                            val fixedDelay = resolveScheduleValue(
                                annotation.findAttributeValue("fixedDelay")?.text,
                                annotation.findAttributeValue("fixedDelayString")?.text?.extractStringValue()
                            )
                            val fixedRate = resolveScheduleValue(
                                annotation.findAttributeValue("fixedRate")?.text,
                                annotation.findAttributeValue("fixedRateString")?.text?.extractStringValue()
                            )

                            results.add(
                                ScheduledTaskInfo(
                                    className = psiClass.name ?: "Unknown",
                                    methodName = method.name,
                                    cron = cron?.takeIf { it.isNotBlank() },
                                    fixedDelay = fixedDelay,
                                    fixedRate = fixedRate,
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

    private fun resolveScheduleValue(numericText: String?, stringText: String?): String? =
        numericText?.takeIf { it != UNSET_NUMERIC && it != DISABLED_NUMERIC && it.isNotBlank() }
            ?: stringText?.takeIf { it.isNotBlank() }

    private fun String.extractStringValue(): String = trim().removeSurrounding("\"")
}