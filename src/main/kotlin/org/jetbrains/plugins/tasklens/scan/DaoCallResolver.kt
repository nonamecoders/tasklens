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

        method.body?.accept(object : JavaRecursiveElementVisitor() {
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
                }
            }
        })

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