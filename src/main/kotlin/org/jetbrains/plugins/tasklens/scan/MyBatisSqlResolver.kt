package org.jetbrains.plugins.tasklens.scan

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import org.jetbrains.plugins.tasklens.model.MyBatisSqlInfo

class MyBatisSqlResolver(private val project: Project) {

    private val SQL_TAGS = setOf("select", "insert", "update", "delete")

    fun resolve(mapperClass: PsiClass, method: PsiMethod): MyBatisSqlInfo? {
        val qualifiedName = mapperClass.qualifiedName ?: return null
        val methodName = method.name
        val psiManager = PsiManager.getInstance(project)
        val smartPointerManager = SmartPointerManager.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)

        val xmlFiles = FilenameIndex.getAllFilesByExt(project, "xml", scope)
        for (vFile in xmlFiles) {
            val psiFile = psiManager.findFile(vFile) as? XmlFile ?: continue
            val rootTag = psiFile.rootTag ?: continue
            if (rootTag.name != "mapper") continue
            val namespace = rootTag.getAttributeValue("namespace") ?: continue
            if (namespace != qualifiedName) continue

            for (child in rootTag.subTags) {
                if (child.name !in SQL_TAGS) continue
                val id = child.getAttributeValue("id") ?: continue
                if (id != methodName) continue

                return MyBatisSqlInfo(
                    sqlType = child.name.uppercase(),
                    sqlId = id,
                    xmlFileName = vFile.name,
                    navigationElement = smartPointerManager.createSmartPsiElementPointer(child)
                )
            }
        }
        return null
    }
}