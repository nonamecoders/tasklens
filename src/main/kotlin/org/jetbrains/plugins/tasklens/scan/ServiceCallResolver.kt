package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
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

                if (!isServiceClass(containingClass)) return

                if (containingClass.isInterface) {
                    // 인터페이스로 resolve된 경우 프로젝트 내 모든 구현체를 찾아 각각 표시
                    val scope = GlobalSearchScope.projectScope(project)
                    val inheritors = ClassInheritorsSearch.search(containingClass, scope, true).findAll()
                    if (inheritors.isEmpty()) {
                        results.add(serviceCallInfo(containingClass, resolvedMethod, smartPointerManager))
                    } else {
                        inheritors.forEach { implClass ->
                            val implMethod = implClass.findMethodBySignature(resolvedMethod, true) ?: return@forEach
                            results.add(serviceCallInfo(implClass, implMethod, smartPointerManager))
                        }
                    }
                } else {
                    results.add(serviceCallInfo(containingClass, resolvedMethod, smartPointerManager))
                }
            }
        })

        return results
    }

    private fun serviceCallInfo(
        psiClass: PsiClass,
        psiMethod: PsiMethod,
        smartPointerManager: SmartPointerManager
    ) = ServiceCallInfo(
        className = psiClass.name ?: "Unknown",
        methodName = psiMethod.name,
        daoCalls = emptyList(),
        navigationElement = smartPointerManager.createSmartPsiElementPointer(psiMethod)
    )

    private fun isServiceClass(psiClass: PsiClass): Boolean {
        val name = psiClass.name ?: return false
        if (name.endsWith("Service") || name.endsWith("ServiceImpl")) return true
        if (psiClass.getAnnotation(SERVICE_ANNOTATION) != null) return true
        return psiClass.interfaces.any { iface -> iface.name?.endsWith("Service") == true }
    }
}