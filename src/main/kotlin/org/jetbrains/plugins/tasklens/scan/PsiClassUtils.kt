package org.jetbrains.plugins.tasklens.scan

import com.intellij.psi.PsiClass

private const val SERVICE_ANNOTATION = "org.springframework.stereotype.Service"

fun isServiceClass(psiClass: PsiClass): Boolean {
    val name = psiClass.name ?: return false
    if (name.endsWith("Service") || name.endsWith("ServiceImpl")) return true
    if (psiClass.getAnnotation(SERVICE_ANNOTATION) != null) return true
    return psiClass.interfaces.any { iface -> iface.name?.endsWith("Service") == true }
}
