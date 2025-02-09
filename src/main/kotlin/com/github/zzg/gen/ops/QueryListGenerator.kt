package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.*

class QueryListGenerator(
    override val context: Context
) : Generator {

    override fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        // Update class comment
        val classCommentText = "/**\n" +
                " * ${metadata.desc}列表 的摘要说明\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaListAdv),一般不应直接修改此文件.\n" +
                " * Copyright (C) 2008 - 鹏业软件公司\n" +
                " */"
        updateClassComment(psiElementFactory, classCommentText, psiClass)
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "${metadata.className}List"
            // 创建类并继承 QueryDataList<T>
            val psiClass = psiElementFactory.createClass(className).apply {
                val extendsRef = psiElementFactory.createReferenceFromText(
                    "pengesoft.db.QueryDataList<${metadata.className}>",
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
        return findClassInModule(module, metadata.pkg, "${metadata.className}List")
            ?: createNewClass(module, metadata)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
        // 生成构造方法
        generateConstructors(psiClass, metadata, psiElementFactory)
    }

    private fun generateConstructors(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 默认构造方法
        val defaultConstructor = """
            /**
             * 默认构造方法
             */
            public ${psiClass.name}() {
                super();
            }
        """.trimIndent()

        // 通过已存在集合构造列表
        val collectionConstructor = """
            /**
             * 通过已存在集合构造列表
             *
             * @param c 已存在的集合
             */
            public ${psiClass.name}(java.util.Collection<${metadata.className}> c) {
                super(c);
            }
        """.trimIndent()

        // 添加默认构造方法
        if (psiClass.constructors.none { it.parameterList.parameters.isEmpty() }) {
            psiClass.add(factory.createMethodFromText(defaultConstructor, psiClass))
        }

        // 添加集合构造方法
        if (psiClass.constructors.none { it.parameterList.parameters.size == 1 && it.parameterList.parameters[0].type.canonicalText == "java.util.Collection<${metadata.className}>" }) {
            psiClass.add(factory.createMethodFromText(collectionConstructor, psiClass))
        }
    }
}