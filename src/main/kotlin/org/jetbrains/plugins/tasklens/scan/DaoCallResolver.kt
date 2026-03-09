package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.plugins.tasklens.model.DaoCallInfo

class DaoCallResolver(private val project: Project) {

    companion object {
        private const val REPOSITORY_ANNOTATION = "org.springframework.stereotype.Repository"
        private const val SERVICE_ANNOTATION = "org.springframework.stereotype.Service"
    }

    fun resolve(method: PsiMethod): List<DaoCallInfo> {
        val results = mutableListOf<DaoCallInfo>()
        val smartPointerManager = SmartPointerManager.getInstance(project)
        val visited = mutableSetOf<String>()

        fun methodKey(m: PsiMethod) = "${m.containingClass?.qualifiedName}#${m.name}#${m.parameterList.parametersCount}"

        fun scanMethod(target: PsiMethod) {
            if (!visited.add(methodKey(target))) return
            target.body?.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    val resolvedMethod = expression.resolveMethod() ?: return
                    val containingClass = resolvedMethod.containingClass ?: return

                    when {
                        isPersistenceClass(containingClass) -> {
                            results.add(
                                DaoCallInfo(
                                    className = containingClass.name ?: "Unknown",
                                    methodName = resolvedMethod.name,
                                    callSitePointer = smartPointerManager.createSmartPsiElementPointer(expression),
                                    navigationElement = smartPointerManager.createSmartPsiElementPointer(resolvedMethod)
                                )
                            )
                        }
                        containingClass.qualifiedName == method.containingClass?.qualifiedName -> {
                            // 같은 클래스 내 위임 메서드 추적
                            scanMethod(resolvedMethod)
                        }
                        isServiceClass(containingClass) -> {
                            // 다른 ServiceImpl 호출 추적
                            if (containingClass.isInterface) {
                                val scope = GlobalSearchScope.projectScope(project)
                                ClassInheritorsSearch.search(containingClass, scope, true).findAll()
                                    .forEach { implClass ->
                                        val implMethod = implClass.findMethodBySignature(resolvedMethod, true)
                                            ?: return@forEach
                                        scanMethod(implMethod)
                                    }
                            } else {
                                scanMethod(resolvedMethod)
                            }
                        }
                    }
                }
            })
        }

        scanMethod(method)
        return results
    }

    private fun isServiceClass(psiClass: PsiClass): Boolean {
        val name = psiClass.name ?: return false
        if (name.endsWith("Service") || name.endsWith("ServiceImpl")) return true
        if (psiClass.getAnnotation(SERVICE_ANNOTATION) != null) return true
        return psiClass.interfaces.any { iface -> iface.name?.endsWith("Service") == true }
    }

    private fun isPersistenceClass(psiClass: PsiClass): Boolean {
        val name = psiClass.name ?: return false
        return name.endsWith("Mapper") ||
                name.endsWith("Repository") ||
                name.endsWith("Dao") ||
                psiClass.getAnnotation(REPOSITORY_ANNOTATION) != null
    }
}