package org.jetbrains.plugins.tasklens.model

import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer

data class ServiceCallInfo(
    val className: String,
    val methodName: String,
    val daoCalls: List<DaoCallInfo>,
    val navigationElement: SmartPsiElementPointer<PsiMethod>
)