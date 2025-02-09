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
        val fieldCode = """@Autowired
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

    private fun generateAddMethod(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodName = "add${metadata.className}"
        val methodCode = """@Override
    public int $methodName(String uid, ${metadata.className} item) {
        AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
        return ${metadata.className.decapitalizeFirstLetter()}Dao.insert(item);
    }"""
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
        val methodCode = """@Override
    public int $methodName(String uid, List<${metadata.className}> list) {
        AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
        return ${metadata.className.decapitalizeFirstLetter()}Dao.insertList(list);
    }"""
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
        val methodCode = """@Override
    public int $methodName(String uid, ${metadata.className} item) {
        AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
        AssertUtils.ThrowArgNullException(item.get${keyField.capitalizeFirstLetter()}(), "${metadata.desc}Id");
        ${metadata.className} detail = get${metadata.className}Detail(uid, item.get${keyField.capitalizeFirstLetter()}());
        $deleteLogic
    }"""
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
        val methodCode = """@Override
    public void $methodName(String uid, List<${metadata.className}> list) {
        AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
        $deleteLogic
    }"""
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
        val keyField = metadata.fields.find { it.primary }?.field ?: "id"
        val methodCode = """@Override
    public int $methodName(String uid, ${metadata.className} item) {
        AssertUtils.ThrowArgNullException(item, "${metadata.desc}");
        AssertUtils.ThrowArgNullException(item.get${keyField.capitalizeFirstLetter()}(), "${metadata.desc}Id");
        return ${metadata.className.decapitalizeFirstLetter()}Dao.update(item);
    }"""
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
        val methodCode = """@Override
    public void $methodName(String uid, List<${metadata.className}> list) {
        AssertUtils.ThrowArgNullException(list, "${metadata.desc}", true);
        ${metadata.className.decapitalizeFirstLetter()}Dao.updateList(list);
    }"""
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
        val methodCode = """@Override
    public ${metadata.className} $methodName(String uid, String keyId) {
        AssertUtils.ThrowArgNullException(keyId, "${metadata.desc}详情标识", true);
        ${metadata.className} detail = new ${metadata.className}();
        detail.set${keyField.capitalizeFirstLetter()}(keyId);
        return ${metadata.className.decapitalizeFirstLetter()}Dao.getDetail(detail);
    }"""
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
        val methodCode = """@Override
    public ${metadata.className}List $methodName(String uid, $queryParaType para, int startIndex, int maxSize, boolean retTotal) {
        if (para == null) {
            para = new $queryParaType();
        }
        ${metadata.className}List list = new ${metadata.className}List(${metadata.className.decapitalizeFirstLetter()}Dao.queryList(para, startIndex, maxSize));
        if (retTotal) {
            list.setTotalCount(${metadata.className.decapitalizeFirstLetter()}Dao.queryCount(para));
        }
        return list;
    }"""
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
        val methodCode = """@Override
    public int $methodName(String uid, $queryParaType para) {
        if (para == null) {
            para = new $queryParaType();
        }
        return ${metadata.className.decapitalizeFirstLetter()}Dao.queryCount(para);
    }"""
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }
}