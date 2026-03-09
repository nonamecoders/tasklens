package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.SmartPointerManager
import org.jetbrains.plugins.tasklens.model.DaoCallInfo

class DaoCallResolver(private val project: Project) {

    companion object {
        private const val REPOSITORY_ANNOTATION = "org.springframework.stereotype.Repository"
    }

    fun resolve(method: PsiMethod): List<DaoCallInfo> {
        val results = mutableListOf<DaoCallInfo>()
        val smartPointerManager = SmartPointerManager.getInstance(project)
        val visited = mutableSetOf<PsiMethod>()

        fun scanMethod(target: PsiMethod) {
            if (!visited.add(target)) return
            target.body?.accept(object : JavaRecursiveElementVisitor() {
                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(expression)
                    val resolvedMethod = expression.resolveMethod() ?: return
                    val containingClass = resolvedMethod.containingClass ?: return

                    if (isPersistenceClass(containingClass)) {
                        results.add(
                            DaoCallInfo(
                                className = containingClass.name ?: "Unknown",
                                methodName = resolvedMethod.name,
                                navigationElement = smartPointerManager.createSmartPsiElementPointer(resolvedMethod)
                            )
                        )
                    } else if (containingClass.qualifiedName == method.containingClass?.qualifiedName) {
                        // 같은 클래스 내 다른 메서드(private 위임 메서드 등)도 한 단계 추적
                        scanMethod(resolvedMethod)
                    }
                }
            })
        }

        scanMethod(method)
        return results
    }

    private fun isPersistenceClass(psiClass: PsiClass): Boolean {
        val name = psiClass.name ?: return false
        return name.endsWith("Mapper") ||
                name.endsWith("Repository") ||
                name.endsWith("Dao") ||
                psiClass.getAnnotation(REPOSITORY_ANNOTATION) != null
    }
}