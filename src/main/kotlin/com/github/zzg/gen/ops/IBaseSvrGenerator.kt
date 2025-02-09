package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.*

class IBaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        val module = findMavenModuleOrCurrent(context.project, context.metadata.module)!!
        val psiClass = findOrCreateClass(module, context.metadata)
        assert(psiClass.containingFile.virtualFile != null)
        WriteCommandAction.runWriteCommandAction(module.project) {
            updateClass(psiClass, context.metadata)
        }
        return psiClass.containingFile
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "I${metadata.className}BaseSvr"
            // 创建接口
            val psiClass = psiElementFactory.createInterface(className).apply {
                // 添加 @SuppressWarnings 注解
                modifierList?.addAnnotation("SuppressWarnings(\"unused\")")
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
        return findClassInModule(module, metadata.pkg, "I${metadata.className}BaseSvr")
            ?: createNewClass(module, metadata)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
        // 生成方法
        generateMethods(psiClass, metadata, psiElementFactory)
    }

    private fun generateMethods(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 清空现有方法
        psiClass.methods.forEach { it.delete() }

        // 生成新增方法
        generateAddMethod(psiClass, metadata, factory)
        generateAddListMethod(psiClass, metadata, factory)

        // 生成删除方法
        generateRemoveMethod(psiClass, metadata, factory)
        generateRemoveListMethod(psiClass, metadata, factory)

        // 生成修改方法
        generateUpdateMethod(psiClass, metadata, factory)
        generateUpdateListMethod(psiClass, metadata, factory)

        // 生成查询方法
        generateGetDetailMethod(psiClass, metadata, factory)
        generateQueryListMethod(psiClass, metadata, factory)
        generateQueryCountMethod(psiClass, metadata, factory)
    }

    private fun generateAddMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "add${metadata.className}"
        val methodCode = """
            /**
             * 新增${metadata.desc}
             *
             * @param uid   用户id
             * @param item  ${metadata.desc}
             * @return rows
             */
            int $methodName(String uid, ${metadata.className} item);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateAddListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "add${metadata.className}List"
        val methodCode = """
            /**
             * 新增${metadata.desc}列表
             *
             * @param uid   用户id
             * @param list  ${metadata.desc}列表
             * @return rows
             */
            int $methodName(String uid, List<${metadata.className}> list);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateRemoveMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "remove${metadata.className}"
        val methodCode = """
            /**
             * 删除${metadata.desc}
             *
             * @param uid   用户id
             * @param item  ${metadata.desc}
             * @return rows
             */
            int $methodName(String uid, ${metadata.className} item);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateRemoveListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "remove${metadata.className}List"
        val methodCode = """
            /**
             * 删除${metadata.desc}列表
             *
             * @param uid   用户id
             * @param list  ${metadata.desc}列表
             */
            void $methodName(String uid, List<${metadata.className}> list);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateUpdateMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "update${metadata.className}"
        val methodCode = """
            /**
             * 修改${metadata.desc}
             *
             * @param uid   用户id
             * @param item  ${metadata.desc}
             * @return rows
             */
            int $methodName(String uid, ${metadata.className} item);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateUpdateListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "update${metadata.className}List"
        val methodCode = """
            /**
             * 修改${metadata.desc}列表
             *
             * @param uid   用户id
             * @param list  ${metadata.desc}列表
             */
            void $methodName(String uid, List<${metadata.className}> list);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateGetDetailMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "get${metadata.className}Detail"
        val keyField = metadata.fields.find { it.primary }?.field ?: "id"
        val methodCode = """
            /**
             * 查询${metadata.desc}详情
             *
             * @param uid    用户id
             * @param keyId  标识符
             * @return ${metadata.desc}详情
             */
            ${metadata.className} $methodName(String uid, String keyId);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateQueryListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "query${metadata.className}List"
        val queryParaType = "${metadata.className}QueryPara"
        val methodCode = """
            /**
             * 查询${metadata.desc}详情列表
             *
             * @param uid         用户id
             * @param para        查询参数
             * @param startIndex  起始索引
             * @param maxSize     最大返回行数
             * @param retTotal    是否返回总数
             * @return ${metadata.desc}列表
             */
            ${metadata.className}List $methodName(String uid, $queryParaType para, int startIndex, int maxSize, boolean retTotal);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    private fun generateQueryCountMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "query${metadata.className}Count"
        val queryParaType = "${metadata.className}QueryPara"
        val methodCode = """
            /**
             * 查询${metadata.desc}总数
             *
             * @param uid   用户id
             * @param para  查询参数
             * @return ${metadata.desc}总数
             */
            int $methodName(String uid, $queryParaType para);
        """.trimIndent()
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }
}
