package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.*

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

    override fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        // Update class comment
        val classCommentText = "/**\n" +
                " * ${metadata.desc} 数据访问接口\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaDaoIntf),一般不应直接修改此文件.\n" +
                " * Copyright (C) 2008 - 鹏业软件公司\n" +
                " */"
        updateClassComment(psiElementFactory, classCommentText, psiClass)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
    }
}