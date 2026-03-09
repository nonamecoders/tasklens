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
        if (name.endsWith("Service") || name.endsWith("ServiceImpl")) return true
        if (psiClass.getAnnotation(SERVICE_ANNOTATION) != null) return true
        // interface 타입으로 선언된 필드를 통해 호출할 때 구현체가 아닌 인터페이스로 resolve되는 경우 처리
        return psiClass.interfaces.any { iface -> iface.name?.endsWith("Service") == true }
    }
}