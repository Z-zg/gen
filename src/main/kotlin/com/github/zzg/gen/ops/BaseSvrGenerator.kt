package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.github.zzg.gen.capitalizeFirstLetter
import com.github.zzg.gen.decapitalizeFirstLetter
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile

class BaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "${metadata.className}BaseSvr"
            // 创建类并实现接口
            val psiClass = psiElementFactory.createClass(className).apply {
                val implementsRef = psiElementFactory.createReferenceFromText(
                    "I${metadata.className}BaseSvr",
                    this
                )
                implementsList?.add(implementsRef)
            }
            // 添加 @Service 注解
            psiClass.modifierList?.addAnnotation("org.springframework.stereotype.Service")
            // 创建文件内容
            val psiFile = PsiFileFactory.getInstance(module.project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, psiClass.text) as PsiJavaFile
            // 添加到目标目录
            getOrCreateDirectory(metadata.pkg, module).add(psiFile)
        }
        return findOrCreateClass(module, context.metadata)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        return findClassInModule(module, metadata.pkg, "${metadata.className}BaseSvr")
            ?: createNewClass(module, metadata)
    }

    override fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        val classCommentText = """/*
            * ${metadata.desc}基础服务
            *    Copyright (C) 2008 - 鹏业软件公司
            */"""
        updateClassComment(psiElementFactory, classCommentText, psiClass)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        // 更新包声明
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        // 更新类注释
        updateClassComment(metadata, psiElementFactory, psiClass)
        // 添加 DAO 属性
        addDaoField(psiClass, metadata, psiElementFactory)
        // 生成方法
        generateMethods(psiClass, metadata, psiElementFactory)
    }

    private fun addDaoField(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val daoFieldName = metadata.className.decapitalizeFirstLetter() + "Dao"
        val daoFieldType = "I${metadata.className}Dao"
        val fieldCode = """@org.springframework.beans.factory.annotation.Autowired
    protected $daoFieldType $daoFieldName;"""
        if (psiClass.fields.none { it.name == daoFieldName }) {
            psiClass.add(factory.createFieldFromText(fieldCode, psiClass))
        }
    }

    private fun generateMethods(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
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

    /**
     * 生成新增方法。
     */
    private fun generateAddMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "add${metadata.className}"
        val methodCode = """
            /**
             * 新增 ${metadata.desc}
             *
             * @param userId 用户id
             * @param item   ${metadata.desc} 对象
             * @return 受影响的行数
             * @throws IllegalArgumentException 如果 {@code item} 为 null
             */
            @Override
            public int $methodName(String userId, ${metadata.className} item) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
                return ${metadata.className.decapitalizeFirstLetter()}Dao.insert(item);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成新增列表方法。
     */
    private fun generateAddListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "add${metadata.className}List"
        val methodCode = """
            /**
             * 新增 ${metadata.desc} 列表
             *
             * @param userId 用户id
             * @param list   ${metadata.desc} 列表
             * @return 受影响的行数
             * @throws IllegalArgumentException 如果 {@code list} 为 null 或为空
             */
            @Override
            public int $methodName(String userId, java.util.List<${metadata.className}> list) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
                return ${metadata.className.decapitalizeFirstLetter()}Dao.insertList(list);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成删除方法。
     */
    private fun generateRemoveMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "remove${metadata.className}"
        val keyField = metadata.fields.find { it.primary }?.field ?: "id"
        val flagDelField = metadata.fields.find { it.field == "flagDel" }
        val deleteLogic = if (flagDelField != null) {
            """
                detail.setFlagDel(true);
                return ${metadata.className.decapitalizeFirstLetter()}Dao.update(detail);
            """.trimIndent()
        } else {
            """
                return ${metadata.className.decapitalizeFirstLetter()}Dao.delete(item);
            """.trimIndent()
        }

        val methodCode = """/**
             * 删除 ${metadata.desc}
             *
             * 如果存在逻辑删除字段，则设置删除标志；否则直接删除记录。
             *
             * @param userId 用户id
             * @param item   ${metadata.desc} 对象
             * @return 受影响的行数
             * @throws IllegalArgumentException 如果 {@code item} 或其主键为 null
             */
            @Override
            public int $methodName(String userId, ${metadata.className} item) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
                pengesoft.utils.AssertUtils.ThrowArgNullException(item.get${keyField.capitalizeFirstLetter()}(), "${metadata.desc}Id");
                ${metadata.className} detail = get${metadata.className}Detail(userId, item.get${keyField.capitalizeFirstLetter()}());
                $deleteLogic
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成删除列表方法。
     */
    private fun generateRemoveListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "remove${metadata.className}List"
        val flagDelField = metadata.fields.find { it.field == "flagDel" }
        val deleteLogic = if (flagDelField != null) {
            """
                list.forEach(v -> v.setFlagDel(true));
                ${metadata.className.decapitalizeFirstLetter()}Dao.updateList(list);
            """.trimIndent()
        } else {
            """
                ${metadata.className.decapitalizeFirstLetter()}Dao.deleteList(list);
            """.trimIndent()
        }

        val methodCode = """/**
             * 删除 ${metadata.desc} 列表
             *
             * 如果存在逻辑删除字段，则批量设置删除标志；否则直接删除记录。
             *
             * @param userId 用户id
             * @param list   ${metadata.desc} 列表
             * @throws IllegalArgumentException 如果 {@code list} 为 null 或为空
             */
            @Override
            public void $methodName(String userId, java.util.List<${metadata.className}> list) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
                $deleteLogic
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成修改方法。
     */
    private fun generateUpdateMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "update${metadata.className}"
        val keyField = metadata.fields.find { it.primary }?.field ?: "id"
        val methodCode = """/**
             * 修改 ${metadata.desc}
             *
             * @param userId 用户id
             * @param item   ${metadata.desc} 对象
             * @return 受影响的行数
             * @throws IllegalArgumentException 如果 {@code item} 或其主键为 null
             */
            @Override
            public int $methodName(String userId, ${metadata.className} item) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
                pengesoft.utils.AssertUtils.ThrowArgNullException(item.get${keyField.capitalizeFirstLetter()}(), "${metadata.desc}Id");
                return ${metadata.className.decapitalizeFirstLetter()}Dao.update(item);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成修改列表方法。
     */
    private fun generateUpdateListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "update${metadata.className}List"
        val methodCode = """/**
             * 修改 ${metadata.desc} 列表
             *
             * @param userId 用户id
             * @param list   ${metadata.desc} 列表
             * @throws IllegalArgumentException 如果 {@code list} 为 null 或为空
             */
            @Override
            public void $methodName(String userId, java.util.List<${metadata.className}> list) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
                ${metadata.className.decapitalizeFirstLetter()}Dao.updateList(list);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成查询详情方法。
     */
    private fun generateGetDetailMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "get${metadata.className}Detail"
        val keyField = metadata.fields.find { it.primary }?.field ?: "id"
        val methodCode = """/**
             * 查询 ${metadata.desc} 详情
             *
             * @param userId 用户id
             * @param keyId  主键标识
             * @return ${metadata.desc} 详情
             * @throws IllegalArgumentException 如果 {@code keyId} 为 null 或为空
             */
            @Override
            public ${metadata.className} $methodName(String userId, String keyId) {
                pengesoft.utils.AssertUtils.ThrowArgNullException(keyId, "${metadata.desc}详情标识", true);
                ${metadata.className} detail = new ${metadata.className}();
                detail.set${keyField.capitalizeFirstLetter()}(keyId);
                return ${metadata.className.decapitalizeFirstLetter()}Dao.getDetail(detail);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成查询列表方法。
     */
    private fun generateQueryListMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "query${metadata.className}List"
        val queryParaType = "${metadata.className}QueryPara"
        val methodCode = """/**
             * 查询 ${metadata.desc} 列表
             *
             * @param userId      用户id
             * @param para        查询参数
             * @param startIndex  起始索引
             * @param maxSize     最大返回行数
             * @param retTotal    是否返回总数
             * @return ${metadata.desc} 列表
             */
            @Override
            public ${metadata.className}List $methodName(String userId, $queryParaType para, int startIndex, int maxSize, boolean retTotal) {
                if (para == null) {
                    para = new $queryParaType();
                }
                ${metadata.className}List list = new ${metadata.className}List(${metadata.className.decapitalizeFirstLetter()}Dao.queryList(para, startIndex, maxSize));
                if (retTotal) {
                    list.setTotalCount(${metadata.className.decapitalizeFirstLetter()}Dao.queryCount(para));
                }
                return list;
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }

    /**
     * 生成查询总数方法。
     */
    private fun generateQueryCountMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "query${metadata.className}Count"
        val queryParaType = "${metadata.className}QueryPara"
        val methodCode = """/**
             * 查询 ${metadata.desc} 总数
             *
             * @param userId 用户id
             * @param para   查询参数
             * @return ${metadata.desc} 总数
             */
            @Override
            public int $methodName(String userId, $queryParaType para) {
                if (para == null) {
                    para = new $queryParaType();
                }
                return ${metadata.className.decapitalizeFirstLetter()}Dao.queryCount(para);
            }
        """.trimIndent()

        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }
}