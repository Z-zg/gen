package com.github.zzg.gen


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiTreeUtil


class GenAnAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val psiFile: PsiFile = event.getData(CommonDataKeys.PSI_FILE)!!
//        val element: PsiElement = event.getData(CommonDataKeys.PSI_ELEMENT)!!

        // 获取当前类的 PsiClass
        val psiClass = PsiTreeUtil.getParentOfType(psiFile, PsiClass::class.java)
        if (psiClass == null) {
            Messages.showInfoMessage("No class found at the cursor position.", "Info")
            return
        }

        // 检查是否包含目标注解
        val targetAnnotation = "java.lang.Deprecated" // 替换为你的目标注解
        val hasAnnotation = hasAnnotation(psiClass, targetAnnotation)

        // 显示结果
        if (hasAnnotation) {
            Messages.showInfoMessage("The class contains the annotation: $targetAnnotation", "Info")
        } else {
            Messages.showInfoMessage("The class does not contain the annotation: $targetAnnotation", "Info")
        }
    }
    private fun hasAnnotation(psiClass: PsiClass, annotationQualifiedName: String): Boolean {
        val annotations = psiClass.annotations
        for (annotation in annotations) {
            if (annotationQualifiedName == annotation.qualifiedName) {
                return true
            }
        }
        return false
    }

}