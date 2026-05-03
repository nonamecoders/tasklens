package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import org.jetbrains.plugins.tasklens.model.ScheduledTaskInfo

class ScheduledMethodScanner(private val project: Project) {

    companion object {
        private const val SCHEDULED_FQN = "org.springframework.scheduling.annotation.Scheduled"
        private const val UNSET_NUMERIC = "0"
        private const val DISABLED_NUMERIC = "-1"
    }

    fun scan(): List<ScheduledTaskInfo> {
        val smartPointerManager = SmartPointerManager.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val annotationClass = JavaPsiFacade.getInstance(project).findClass(SCHEDULED_FQN, scope)
            ?: return emptyList()

        return AnnotatedMembersSearch.search(annotationClass, scope)
            .filterIsInstance<PsiMethod>()
            .mapNotNull { method ->
                val psiClass = method.containingClass ?: return@mapNotNull null
                val annotation = method.getAnnotation(SCHEDULED_FQN) ?: return@mapNotNull null

                val cron = annotation.findAttributeValue("cron")?.text?.extractStringValue()
                val fixedDelay = resolveScheduleValue(
                    annotation.findAttributeValue("fixedDelay")?.text,
                    annotation.findAttributeValue("fixedDelayString")?.text?.extractStringValue()
                )
                val fixedRate = resolveScheduleValue(
                    annotation.findAttributeValue("fixedRate")?.text,
                    annotation.findAttributeValue("fixedRateString")?.text?.extractStringValue()
                )

                ScheduledTaskInfo(
                    className = psiClass.name ?: "Unknown",
                    methodName = method.name,
                    cron = cron?.takeIf { it.isNotBlank() },
                    fixedDelay = fixedDelay,
                    fixedRate = fixedRate,
                    serviceCalls = emptyList(),
                    navigationElement = smartPointerManager.createSmartPsiElementPointer(method)
                )
            }
    }

    private fun resolveScheduleValue(numericText: String?, stringText: String?): String? {
        val normalized = numericText?.trimEnd('L', 'l')
        return if (normalized != null && normalized != UNSET_NUMERIC && normalized != DISABLED_NUMERIC && normalized.isNotBlank())
            normalized
        else
            stringText?.takeIf { it.isNotBlank() }
    }

    private fun String.extractStringValue(): String = trim().removeSurrounding("\"")
}
