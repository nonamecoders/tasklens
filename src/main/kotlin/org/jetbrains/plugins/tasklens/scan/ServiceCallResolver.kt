package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.SmartPointerManager
import org.jetbrains.plugins.tasklens.model.ServiceCallInfo

class ServiceCallResolver(private val project: Project) {

    companion object {
        private const val SERVICE_ANNOTATION = "org.springframework.stereotype.Service"
    }

    fun resolve(method: PsiMethod): List<ServiceCallInfo> {
        val results = mutableListOf<ServiceCallInfo>()
        val smartPointerManager = SmartPointerManager.getInstance(project)

        method.body?.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                val resolvedMethod = expression.resolveMethod() ?: return
                val containingClass = resolvedMethod.containingClass ?: return

                if (isServiceClass(containingClass)) {
                    results.add(
                        ServiceCallInfo(
                            className = containingClass.name ?: "Unknown",
                            methodName = resolvedMethod.name,
                            daoCalls = emptyList(),
                            navigationElement = smartPointerManager.createSmartPsiElementPointer(resolvedMethod)
                        )
                    )
                }
            }
        })

        return results
    }

    private fun isServiceClass(psiClass: PsiClass): Boolean {
        val name = psiClass.name ?: return false
        return name.endsWith("Service") || psiClass.getAnnotation(SERVICE_ANNOTATION) != null
    }
}