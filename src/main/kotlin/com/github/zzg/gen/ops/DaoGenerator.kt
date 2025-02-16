package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.*


class DaoGenerator(
    override val context: Context
) : Generator {
    override fun updateClassPkg(psiElementFactory: PsiElementFactory, pkg: String, psiClass: PsiClass) {
        super.updateClassPkg(psiElementFactory, "$pkg.dao", psiClass)
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "${metadata.className}Dao"
            // 创建类并继承 DataProvider
            val psiClass = psiElementFactory.createClass(className).apply {
                val extendsRef = psiElementFactory.createReferenceFromText(
                    "pengesoft.db.DataProvider<${metadata.className}>",
                    this
                )
                extendsList?.add(extendsRef)
            }
            // 添加 @Repository 注解
            psiClass.modifierList?.addAnnotation("org.springframework.stereotype.Repository")
            // 创建文件内容
            val psiFile = PsiFileFactory.getInstance(module.project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, psiClass.text) as PsiJavaFile
            // 添加到目标目录
            getOrCreateDirectory(metadata.pkg+".dao", module).add(psiFile)
        }
        return findOrCreateClass(module, context.metadata)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        return findClassInModule(module, metadata.pkg+".dao", "${metadata.className}Dao")
            ?: createNewClass(module, metadata)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
        // 添加默认的 SQL 语句 ID 注释
        addDefaultStatementIds(psiClass, metadata, psiElementFactory)
    }

    override fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        // Update class comment
        val classCommentText = "/**\n" +
                " * ${metadata.desc} 数据访问接口基本实现类\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaDaoImp),一般不应直接修改此文件.\n" +
                " * Copyright (C) 2008 - 鹏业软件公司\n" +
                " */"
        val classComment = psiElementFactory.createCommentFromText(
            classCommentText, psiClass
        )
        // 检查是否已经存在类似的类注释
        val existingClassComment = psiClass.firstChild as? PsiComment

        // 如果不存在类注释，或者现有的注释内容与新的注释内容不同，则添加或更新
        if (existingClassComment == null || existingClassComment.text != classCommentText) {
            if (existingClassComment != null) {
                // 如果存在类注释但内容不同，则替换
                existingClassComment.replace(classComment)
            } else {
                // 如果不存在类注释，则添加
                psiClass.addBefore(classComment, psiClass.firstChild)
            }
        }
    }

    private fun addDefaultStatementIds(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val commentText = """
            /*
                default statement ids:
                InsertStatementId: ${metadata.pkg}.${metadata.className}Dao.insert${metadata.className}.
                UpdateStatementId: ${metadata.pkg}.${metadata.className}Dao.update${metadata.className}.
                DeleteStatementId: ${metadata.pkg}.${metadata.className}Dao.delete${metadata.className}.
                GetHasDetailStatementId: ${metadata.pkg}.${metadata.className}Dao.get${metadata.className}.
                GetNoDetailStatementId: ${metadata.pkg}.${metadata.className}Dao.getBase${metadata.className}.
                QueryCountStatementId: ${metadata.pkg}.${metadata.className}Dao.query${metadata.className}Count.
                QueryHasDetailListStatementId: ${metadata.pkg}.${metadata.className}Dao.query${metadata.className}List.
                QueryNoDetailListStatementId: ${metadata.pkg}.${metadata.className}Dao.queryBase${metadata.className}List.
                If you need to change, you can rewrite the corresponding get method.
             */
        """.trimIndent()
        val comment = factory.createCommentFromText(commentText, psiClass)
        psiClass.add(comment)
    }
}
