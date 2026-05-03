package org.jetbrains.plugins.tasklens.model

import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlTag

data class MyBatisSqlInfo(
    val sqlType: String,
    val sqlId: String,
    val xmlFileName: String,
    val navigationElement: SmartPsiElementPointer<XmlTag>
)