package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile

class IDaoGenerator(
    override val context: Context
) : Generator {
    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "I${metadata.className}Dao"
            // 创建接口并继承 IDataProvider
            val psiClass = psiElementFactory.createInterface(className).apply {
                val extendsRef = psiElementFactory.createReferenceFromText(
                    "pengesoft.db.IDataProvider<${metadata.className}>",
                    this
                )
                extendsList?.add(extendsRef)
            }
            // 创建文件内容
            val psiFile = PsiFileFactory.getInstance(module.project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, psiClass.text) as PsiJavaFile
            // 添加到目标目录
            getOrCreateDirectory(metadata.pkg, module).add(psiFile)
        }
        return findOrCreateClass(module, context.metadata)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        return findClassInModule(module, metadata.pkg, "I${metadata.className}Dao")
            ?: createNewClass(module, metadata)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
    }
}