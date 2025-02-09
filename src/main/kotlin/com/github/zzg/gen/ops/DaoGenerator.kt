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


class DaoGenerator(
    override val context: Context
) : Generator {
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
            getOrCreateDirectory(metadata.pkg, module).add(psiFile)
        }
        return findOrCreateClass(module, context.metadata)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        return findClassInModule(module, metadata.pkg, "${metadata.className}Dao")
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
        psiClass.addBefore(comment, psiClass.firstChild)
    }
}
