package org.jetbrains.plugins.tasklens.model

import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer

data class DaoCallInfo(
    val className: String,
    val methodName: String,
    val navigationElement: SmartPsiElementPointer<PsiMethod>
)