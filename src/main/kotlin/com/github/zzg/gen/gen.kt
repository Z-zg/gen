@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

fun String.capitalizeFirstLetter(): String {
    if (isEmpty()) return this
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}
interface Generator {
    val context: Context
    fun findClassInModule(module: Module, packageName: String, className: String): PsiClass? {
        PsiDocumentManager.getInstance(module.project).commitAllDocuments()
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return JavaPsiFacade.getInstance(module.project).findClass("$packageName.$className", scope)
    }

    fun updateClassPkg(
        psiElementFactory: PsiElementFactory,
        pkg: String,
        psiClass: PsiClass
    ) {
        // Update package statement
        val packageStatement = psiElementFactory.createPackageStatement(pkg)
        // 检查是否已经存在 package 语句
        val existingPackageStatement = psiClass.containingFile.children.find {
            it is PsiPackageStatement
        } as? PsiPackageStatement

        // 如果不存在 package 语句，则添加
        if (existingPackageStatement == null) {
            psiClass.containingFile.addBefore(packageStatement, psiClass)
        } else {
            // 如果存在，检查是否需要更新
            if (existingPackageStatement.packageName != pkg) {
                // 更新 package 语句
                existingPackageStatement.replace(packageStatement)
            }
        }
    }

    fun findMavenModuleOrCurrent(project: Project, moduleName: String):
            Module? {
        val module = findModule(project, moduleName)
        return module ?: ModuleManager.getInstance(project).modules.first()
    }

    fun getOrCreateDirectory(packageName: String, module: Module): PsiDirectory {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val contentRoots = moduleRootManager.sourceRoots
        if (contentRoots.isEmpty()) {
            throw IllegalStateException("Module root directory not found")
        }

        val moduleRoot = contentRoots[0]
        val psiManager = PsiManager.getInstance(module.project)
        val moduleRootDir =
            psiManager.findDirectory(moduleRoot) ?: throw IllegalStateException("Module root directory not found")

        val packagePath = packageName.replace('.', '/')
        var currentDir = moduleRootDir
        for (pathSegment in packagePath.split('/')) {
            val subDir = currentDir.findSubdirectory(pathSegment)
            if (subDir == null) {
                currentDir = currentDir.createSubdirectory(pathSegment)
            } else {
                currentDir = subDir
            }
        }
        return currentDir
    }

    fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        // Update class comment
        val classCommentText = "/**\n" +
                " * ${metadata.desc}\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaAdv),一般不应直接修改此文件.\n" +
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

    fun generate(): PsiFile? {
        val module = findMavenModuleOrCurrent(context.project, context.metadata.module)!!
        val psiClass = findOrCreateClass(module, context.metadata)
        assert(psiClass.containingFile.virtualFile != null)
        WriteCommandAction.runWriteCommandAction(module.project) {
            updateClass(psiClass, context.metadata)
        }
        return findOrCreateClass(module, context.metadata).containingFile
    }

    fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass
    fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass
    fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata)
}

fun getDefaultValue(type: String): String {
    return when (type) {
        "int" -> "0"
        "long" -> "0L"
        "double" -> "0.0"
        "boolean" -> "false"
        "String" -> "null"
        else -> "null"
    }
}

fun isCustomType(fieldType: String): Boolean {
    val on = setOf(
        "int", "long", "double", "boolean", "String",
        "BigDecimal"
    )
    return !on.contains(fieldType)
}

public fun findModule(project: Project, moduleName: String):
        Module? {
    return ModuleManager.getInstance(project).modules.find { it.name == moduleName }
}


fun getDefaultValueForType(psiType: PsiType): String {
    return when (psiType.canonicalText) {
        "byte" -> "0"
        "short" -> "0"
        "int" -> "0"
        "long" -> "0L"
        "float" -> "0.0f"
        "double" -> "0.0"
        "char" -> "''"
        "boolean" -> "false"
        "java.math.BigDecimal" -> "java.math.BigDecimal.ZERO"
        else -> "null"
    }
}

fun isPrimitiveType(psiType: PsiType): Boolean {
    return when (psiType.canonicalText) {
        "byte", "short", "int", "long", "float", "double", "char", "boolean" -> true
        else -> false
    }
}


data class Context(
    val myPluginSettings: MyPluginSettings,
    val metadata: EntityDescMetadata,
    val psiClass: PsiClass,
    val project: Project
)

class EntityGenerator(
    override val context: Context
) : Generator {
    private fun genCleanItem(field: EntityFieldDescMetadata?): String {
        return field?.let {
            val defaultValue = getDefaultValueForType(field.type)
            return "this.${field.field} = $defaultValue;"
        } ?: ""
    }

    private fun genAssignFromItem(field: EntityFieldDescMetadata?): String {
        return field?.let {
            if (isPrimitiveType(field.type) || "java.lang.String" == field.type.canonicalText) {
                "this.${field.field} = s.${field.field};"
            } else if ("java.math.BigDecimal" == field.type.canonicalText) {
                """if (s.${field.field} != null)
                  this.${field.field} = s.${field.field}.add(BigDecimal.ZERO);
            """.trimIndent()
            } else if ("java.util.Date" == field.type.canonicalText) {
                """if (s.${field.field} != null)
                  this.${field.field} = new Date(s.${field.field}.getTime());
            """.trimIndent()
            } else {
                """if (s.${field.field} != null) {
                  this.${field.field} = new ${field.type.canonicalText}();
                  this.${field.field}.assignFrom(s.${field.field});
            }""".trimIndent()
            }
        } ?: ""
    }

    private fun updateEntityFields(
        metadata: EntityDescMetadata,
        psiClass: PsiClass,
        psiElementFactory: PsiElementFactory
    ) {
        // Add fields
        metadata.fields.forEachIndexed { index, field ->
            field?.let {
                val existingField = psiClass.fields.find { f -> f.name == it.field }
                if (existingField == null) {
                    val newField = psiElementFactory.createField(
                        it.field,
                        it.type
                    )
                    newField.addBefore(
                        psiElementFactory.createCommentFromText("// ${it.desc}", psiClass),
                        newField.firstChild
                    )
                    if (index == 0) {
                        psiClass.add(newField)
                    } else {
                        val last = psiClass.fields.find { f -> f.name == metadata.fields[index - 1]?.field }
                        if (last == null) {
                            psiClass.add(newField)
                        } else {
                            val psiParserFacade = PsiParserFacade.getInstance(psiClass.project)
                            val emptyLine = psiParserFacade.createWhiteSpaceFromText("\n")
                            last.addAfter(emptyLine, last.lastChild)
                            last.addAfter(newField, last.lastChild)
                        }
                    }
                }
            }
        }

        // Add getters and setters
        metadata.fields.forEachIndexed { index, field ->
            if (field != null) {
                genGetterAndSetter(field, index, metadata, psiElementFactory, psiClass)
            }
        }

        // Add clear method
        val clearMethod = psiElementFactory.createMethodFromText(
            """@Override
            public void clear() {
                super.clear();
                ${metadata.fields.joinToString("\n") { genCleanItem(it) }}
            }
            """.trimIndent(),
            psiClass
        )
        psiClass.methods.find { it.name == clearMethod.name }?.delete()
        psiClass.add(clearMethod)

        // Add assignFrom method
        val assignFromMethod = psiElementFactory.createMethodFromText(
            """@Override
            public void assignFrom(pengesoft.data.DataPacket sou) {
                super.assignFrom(sou);
                if (!(sou instanceof ${metadata.className})) {
                    return;
                }
                ${metadata.className} s = (${metadata.className}) sou;
                ${metadata.fields.joinToString("\n") { genAssignFromItem(it) }}
            }
            """.trimIndent(),
            psiClass
        )
        psiClass.methods.find { it.name == assignFromMethod.name }?.delete()
        psiClass.add(assignFromMethod)
        // Add toString method
        val toStringMethod = psiElementFactory.createMethodFromText(
            """@Override
            public String toString() {
                return this.getJsonText();
            }
            """.trimIndent(),
            psiClass
        )
        psiClass.methods.find { it.name == toStringMethod.name }?.delete()
        psiClass.add(toStringMethod)
        // delete unclear field and method
        psiClass.fields.forEach { field ->
            metadata.fields.none { (it?.field ?: "") == field.name }.ifTrue {
                field.delete()
                val prefix = if (field.type.canonicalText == "boolean") "is" else "get"
                psiClass.methods.filter { it.name == prefix + field.name }.forEach { it.delete() }
            }
        }
    }

    private fun genGetterAndSetter(
        it: EntityFieldDescMetadata,
        index: Int,
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        val fieldName = it.field
        val capitalizedFieldName = fieldName.capitalizeFirstLetter()
        val prefix = if (it.type.canonicalText == "boolean") "is" else "get"
        val getter = psiElementFactory.createMethodFromText(
            "public ${it.type.canonicalText} ${prefix}$capitalizedFieldName() {\n" +
                    "    return this.$fieldName;\n" +
                    "}",
            psiClass
        )
        // Generate getter method
        val getterComment = psiElementFactory.createCommentFromText(
            "/**\n" +
                    " * 获取 ${it.desc}\n" +
                    " *\n" +
                    " * @return ${it.desc}\n" +
                    " */", getter
        )
        getter.addBefore(getterComment, getter.firstChild)


        // Generate setter method
        val setterComment = psiElementFactory.createCommentFromText(
            "/**\n" +
                    " * 设置 ${it.desc}\n" +
                    " *\n" +
                    " * @param $fieldName ${it.desc}\n" +
                    " */", psiClass
        )
        val setter = psiElementFactory.createMethodFromText(
            "public void set$capitalizedFieldName(${it.type.canonicalText} $fieldName) {\n" +
                    "    this.$fieldName = $fieldName;\n" +
                    "}",
            psiClass
        )
        setter.addBefore(setterComment, setter.firstChild)
        if (psiClass.methods.none { it.name == setter.name }) {
            getter.addAfter(setter, getter.lastChild)
        }

        if (psiClass.methods.none { it.name == getter.name }) {
            if (index == 0) {
                psiClass.add(getter)
            } else {
                val fieldName1 = metadata.fields[index - 1]?.field ?: ""
                val setter1 = psiClass.methods.find { it.name == "set$fieldName1" }
                if (setter1 == null) {
                    psiClass.add(getter)
                } else {
                    setter1.addAfter(getter, setter1.lastChild)
                }
            }
        }
    }


    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
        updateClassPkg(psiElementFactory, metadata.pkg, psiClass)
        updateClassComment(metadata, psiElementFactory, psiClass)
        updateEntityFields(metadata, psiClass, psiElementFactory)
        PsiDocumentManager.getInstance(psiClass.project).commitAllDocuments()
        psiClass.containingFile.virtualFile?.refresh(false, false)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        val psiClass = findClassInModule(module, metadata.pkg, metadata.className)
        return psiClass ?: createNewClass(module, metadata)
    }


    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val psiClass = psiElementFactory.createClass(metadata.className)
            // 创建一个包含完全限定名的引用元素
            val referenceElement: PsiJavaCodeReferenceElement =
                psiElementFactory.createReferenceFromText("pengesoft.data.DataPacket", psiClass)
            psiClass.extendsList?.add(referenceElement)
            val psiFile = PsiFileFactory.getInstance(module.project).createFileFromText(
                "${metadata.className}.java",
                JavaFileType.INSTANCE,
                psiElementFactory.createPackageStatement(metadata.pkg).text + psiClass.text
            ) as PsiJavaFile
            val directory = getOrCreateDirectory(metadata.pkg, module)
            directory.add(psiFile)
        };
        return findOrCreateClass(module, metadata)
    }
}

class EntityQueryParaGenerator(
    override val context: Context
) : Generator {

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        WriteCommandAction.runWriteCommandAction<PsiClass>(module.project) {
            val psiElementFactory = PsiElementFactory.getInstance(module.project)
            val className = "${metadata.className}QueryPara"

            // 创建类并继承 QueryParameter
            val psiClass = psiElementFactory.createClass(className).apply {
                val extendsRef = psiElementFactory.createReferenceFromText(
                    "pengesoft.db.QueryParameter",
                    this
                )
                extendsList?.add(extendsRef)
            }
            val psiFile = PsiFileFactory.getInstance(module.project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, psiClass.text) as PsiJavaFile
            // 添加到目标目录
            getOrCreateDirectory(metadata.pkg, module).add(psiFile)
            psiFile.classes[0]
        }
        return findOrCreateClass(module, metadata)
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        return findClassInModule(module, metadata.pkg, "${metadata.className}QueryPara")
            ?: createNewClass(module, metadata)
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)

        // 1. 生成静态常量
        generateQueryAttrs(psiClass, metadata, psiElementFactory)

        // 2. 生成构造函数
        generateConstructors(psiClass, metadata, psiElementFactory)

        // 3. 生成SetQueryPara方法
        generateSetQueryPara(psiClass, metadata, psiElementFactory)

        // 4. 生成字段处理方法
        generateParamMethods(psiClass, metadata, psiElementFactory)

    }

    private fun generateQueryAttrs(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 生成 QueryAttr_ 常量
        var x = 0
        var y = 0
        metadata.fields.forEach { field ->
            field?.let {
                if (field.queryable) {
                    val constName = "QueryAttr_${it.field.capitalizeFirstLetter()}"
                    val constText = "public static final String $constName = \"${it.field}\";"
                    if (psiClass.fields.none { f -> f.name == constName }) {
                        psiClass.add(factory.createFieldFromText(constText, psiClass))
                    }

                }
                x++
                if (field.sortable) {
                    val constName = "OrderAttr_${field.field.capitalizeFirstLetter()}"
                    val constText = "public static final String $constName = \"${field.field}\";"
                    if (psiClass.fields.none { f -> f.name == constName }) {
                        psiClass.add(factory.createFieldFromText(constText, psiClass))
                    }
                }
                y++
            }
        }

    }

    private fun generateConstructors(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 默认构造方法
        val defaultConstructor = """public ${psiClass.name}() {
                this(null, null, false);
            }
        """.trimIndent()

        // 带参构造方法
        val paramConstructor = """public ${psiClass.name}(${metadata.className} data, String order, boolean isAse) {
                SetQueryPara(data, order, isAse);
            }
        """.trimIndent()

        listOf(defaultConstructor, paramConstructor).forEach { code ->
            if (psiClass.methods.none { it.text == code }) {
                psiClass.add(factory.createMethodFromText(code, psiClass))
            }
        }
    }

    private fun generateSetQueryPara(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        val methodBody = buildString {
            appendLine("if (data != null) {")
            metadata.fields.forEach { field ->
                field?.let {
                    if (it.queryable)
                        appendLine("setParamBy${it.field.capitalizeFirstLetter()}(data.get${it.field.capitalizeFirstLetter()}());")
                }
            }
            appendLine("}")
            appendLine("if (!StringHelper.isNullOrEmpty(order)) addOrderBy(order, isAse);")
        }

        val methodCode = """public void SetQueryPara(${metadata.className} data, String order, boolean isAse) {
                $methodBody
            }
        """.trimIndent()

        psiClass.methods.find { it.name == "SetQueryPara" }?.delete()
        psiClass.add(factory.createMethodFromText(methodCode, psiClass))
    }

    private fun generateParamMethods(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        metadata.fields.filterNotNull().forEach { field ->
            when (field.type.canonicalText) {
                "java.lang.String" -> generateStringParamMethods(field, psiClass, factory)
                "boolean", "java.lang.Boolean" -> generateBooleanParamMethods(field, psiClass, factory)
                "int", "java.lang.Integer", "long", "java.lang.Long" -> generateNumericParamMethods(
                    field,
                    psiClass,
                    factory
                )

                "java.util.Date" -> generateDateParamMethods(field, psiClass, factory)
                "java.math.BigDecimal" -> generateBigDecimalParamMethods(field, psiClass, factory)
                "pengesoft.data.DynDataPacket" -> generateDynDataParamMethod(field, psiClass, factory)
                else -> generateObjectParamMethods(field, psiClass, factory)
            }
        }
    }

    private fun generateStringParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        // Basic setter
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(String ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        // InEmpty处理
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}InEmpty(String ${field.field}) {
                put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        // Enum版本
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}_Enum(String... ${field.field}s) {
                addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
            }
        """, factory
        )
    }

    private fun generateBooleanParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(boolean ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )
    }

    private fun generateNumericParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        val type = field.type.canonicalText.removePrefix("java.lang.")

        // 基本方法
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}($type ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        // 包含0值
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}IncZero($type ${field.field}) {
                put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        // 枚举参数
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}_Enum($type... ${field.field}s) {
                addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
            }
        """, factory
        )

        // 范围查询
        addMethodIfNotExists(
            psiClass,
            """public void setParamBy${field.field.capitalizeFirstLetter()}_Range($type low, $type high) {
                addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, low, high);
            }
        """, factory
        )
    }

    private fun generateDateParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(java.util.Date ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}_Range(java.util.Date startDate, java.util.Date endDate) {
                addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, startDate, endDate);
            }
        """, factory
        )
    }

    private fun generateBigDecimalParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(java.math.BigDecimal ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}IncZero(java.math.BigDecimal ${field.field}) {
                put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )

        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}_Enum(java.math.BigDecimal... ${field.field}s) {
                addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
            }
        """, factory
        )

        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}_Range(java.math.BigDecimal low, java.math.BigDecimal high) {
                addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, low, high);
            }
        """, factory
        )
    }

    private fun generateDynDataParamMethod(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(DynDataPacket ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )
    }

    private fun generateObjectParamMethods(
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory
    ) {
        addMethodIfNotExists(
            psiClass, """public void setParamBy${field.field.capitalizeFirstLetter()}(Object ${field.field}) {
                addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
            }
        """, factory
        )
    }

    private fun addMethodIfNotExists(
        psiClass: PsiClass,
        methodCode: String,
        factory: PsiElementFactory
    ) {
        val methodName = methodCode.substringAfter("public void ").substringBefore("(")
        if (psiClass.methods.none { it.name == methodName }) {
            psiClass.add(factory.createMethodFromText(methodCode, psiClass))
        }
    }
}

class EntityQueryExParaGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class DaoGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class IDaoGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class BaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class IBaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class MybatisXmlGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class MybatisXmlExGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class MgeSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class IMgeSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class ControllerSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class IControllerSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class BusinessGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class IBusinessGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class TsGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}

class TsControllerGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }
}