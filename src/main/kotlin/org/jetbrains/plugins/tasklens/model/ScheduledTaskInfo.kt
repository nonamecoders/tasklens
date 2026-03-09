package org.jetbrains.plugins.tasklens.model

import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer

data class ScheduledTaskInfo(
    val className: String,
    val methodName: String,
    val cron: String?,
    val fixedDelay: String?,
    val fixedRate: String?,
    val serviceCalls: List<ServiceCallInfo>,
    val navigationElement: SmartPsiElementPointer<PsiMethod>
)