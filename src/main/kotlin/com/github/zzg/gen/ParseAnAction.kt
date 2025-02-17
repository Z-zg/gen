package com.github.zzg.gen

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil

class ParseAnAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        // 获取当前文件
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if (psiFile == null) {
            Messages.showInfoMessage("No file found.", "Error")
            return
        }

        // 获取编辑器
        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            Messages.showInfoMessage("No editor found.", "Error")
            return
        }

        val project = event.project!!
        // 获取光标偏移量
        val offset = editor.caretModel.offset

        // 获取光标位置的 PsiElement
        val element = psiFile.findElementAt(offset)
        if (element == null) {
            Messages.showInfoMessage("No element found at the cursor position.", "Info")
            return
        }

        // 获取当前类的 PsiClass
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        if (psiClass == null) {
            Messages.showInfoMessage("No class found at the cursor position.", "Info")
            return
        }
        val desc = Parser.parseEntityDescByPsiClass(psiClass)
        println(desc)
    }
}