package com.github.zzg.gen

import com.github.zzg.gen.Parser.parseEntityDescAnnotation
import com.github.zzg.gen.config.MyPluginSettings
import com.github.zzg.gen.ops.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class GenAnAction : AnAction() {
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

        // 检查是否包含目标注解
        val targetAnnotation = "org.zq.EntityDesc" // 替换为你的目标注解
        val hasAnnotation = hasAnnotation(psiClass, targetAnnotation)
        // 显示结果
        if (hasAnnotation) {
            // 需要解析
            val metadata = parseEntityDescAnnotation(psiClass)!!

            val context = Context(MyPluginSettings(), metadata, psiClass, project)
            val file = EntityGenerator(context).generate()!!
//            val project = psiFile.project
            val qp = EntityQueryParaGenerator(context).generate()!!

            val li = QueryListGenerator(context).generate()!!

            val ibsvr = IBaseSvrGenerator(context).generate()!!

            val bsvr = BaseSvrGenerator(context).generate()!!

            // 在写操作上下文中执行格式化
            WriteCommandAction.runWriteCommandAction(project) {
                // 获取 CodeStyleManager 实例
                val codeStyleManager = CodeStyleManager.getInstance(project)
                // 对整个 PsiFile 进行格式化
                codeStyleManager.reformat(file)
                codeStyleManager.reformat(qp)
                codeStyleManager.reformat(li)
                codeStyleManager.reformat(ibsvr)
                codeStyleManager.reformat(bsvr)
            }
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