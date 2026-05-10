package org.jetbrains.plugins.tasklens.navigation

import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import com.intellij.psi.JavaPsiFacade

class MyBatisXmlReferenceContributor : PsiReferenceContributor() {

    companion object {
        private val SQL_TAGS = setOf("select", "insert", "update", "delete")
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            XmlPatterns.xmlAttributeValue()
                .withParent(XmlPatterns.xmlAttribute().withLocalName("id")),
            MyBatisIdReferenceProvider()
        )
    }

    private inner class MyBatisIdReferenceProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val attrValue = element as? XmlAttributeValue ?: return PsiReference.EMPTY_ARRAY
            val sqlTag = (attrValue.parent as? XmlAttribute)?.parent as? XmlTag ?: return PsiReference.EMPTY_ARRAY
            if (sqlTag.name !in SQL_TAGS) return PsiReference.EMPTY_ARRAY
            val mapperTag = sqlTag.parent as? XmlTag ?: return PsiReference.EMPTY_ARRAY
            if (mapperTag.name != "mapper") return PsiReference.EMPTY_ARRAY
            mapperTag.getAttributeValue("namespace") ?: return PsiReference.EMPTY_ARRAY
            return arrayOf(MyBatisIdReference(attrValue))
        }
    }

    private class MyBatisIdReference(element: XmlAttributeValue) :
        PsiReferenceBase<XmlAttributeValue>(element, ElementManipulators.getValueTextRange(element)) {

        override fun resolve(): PsiElement? {
            val methodName = element.value.takeIf { it.isNotBlank() } ?: return null
            val sqlTag = (element.parent as? XmlAttribute)?.parent as? XmlTag ?: return null
            val mapperTag = sqlTag.parent as? XmlTag ?: return null
            val namespace = mapperTag.getAttributeValue("namespace") ?: return null

            val mapperClass = JavaPsiFacade.getInstance(element.project)
                .findClass(namespace, GlobalSearchScope.allScope(element.project))
                ?: return null

            return mapperClass.methods.firstOrNull { it.name == methodName }
        }

        override fun getVariants(): Array<Any> = emptyArray()
    }
}
