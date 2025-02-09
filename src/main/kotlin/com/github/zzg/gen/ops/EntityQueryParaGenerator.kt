package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.github.zzg.gen.EntityFieldDescMetadata
import com.github.zzg.gen.capitalizeFirstLetter
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile

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
        // 遍历字段并生成 QueryAttr_ 和 OrderAttr_ 常量
        metadata.fields.toList().forEachIndexed { index, field ->
            // 获取前一个字段的 QueryAttr_ 或 OrderAttr_ 常量名称
            var previousConstName: String? = null
            var previousConstName1: String? = null
            if (index > 1) {
                for (i in (index - 1) downTo 1) {
                    if (metadata.fields[i].queryable) {
                        if (previousConstName == null) {
                            previousConstName = metadata.fields[i].field
                        }
                    }
                    if (metadata.fields[i].sortable) {
                        if (previousConstName1 == null) {
                            previousConstName1 = metadata.fields[i].field
                        }
                    }
                }
            }
            // 如果字段是 queryable，则生成 QueryAttr_ 常量
            if (field.queryable) {
                val constName = "QueryAttr_${field.field.capitalizeFirstLetter()}"
                val constText = "/**\n" +
                        "* 常数 查询属性名(${field.desc}).\n" +
                        "*/\n" +
                        "public static final String $constName = \"${field.field}\";"
                addConstantAfterPrevious(psiClass, constText, factory, previousConstName)
            }

            // 如果字段是 sortable，则生成 OrderAttr_ 常量
            if (field.sortable) {
                val constName = "OrderAttr_${field.field.capitalizeFirstLetter()}"
                val constText = "/**\n" +
                        "                     * 常数 排序属性名(${field.desc}).\n" +
                        "                     */\n public static final String $constName = \"${field.field}\";"
                addConstantAfterPrevious(psiClass, constText, factory, previousConstName1)
            }
        }
    }

    // 按顺序插入常量
    private fun addConstantAfterPrevious(
        psiClass: PsiClass,
        constText: String,
        factory: PsiElementFactory,
        previousConstName: String?
    ) {
        val newConst = factory.createFieldFromText(constText, psiClass)

        if (previousConstName == null) {
            // 如果没有前一个常量，则直接添加到类中
            psiClass.add(newConst)
        } else {
            // 找到前一个常量的位置
            val previousConst = psiClass.fields.find { it.name == previousConstName }
            if (previousConst != null) {
                // 在前一个常量后面插入新常量
                previousConst.addAfter(newConst, previousConst.lastChild)
            } else {
                // 如果找不到前一个常量（理论上不应该发生），直接添加到类中
                psiClass.add(newConst)
            }
        }
    }

    private fun generateConstructors(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 默认构造方法
        val defaultConstructor = """/**
     * 默认构造方法
     */
     public ${psiClass.name}() {
                this(null, null, false);
            }
        """.trimIndent()

        // 带参构造方法
        val paramConstructor = """/**
     * 构造函数,指定参数对象及排序字段
     *
     * @param data  查询参数对象
     * @param order 排序字段
     * @param isAse true升序，false降序
     */
     public ${psiClass.name}(${metadata.className} data, String order, boolean isAse) {
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
                field.let {
                    if (it.queryable)
                        appendLine("setParamBy${it.field.capitalizeFirstLetter()}(data.get${it.field.capitalizeFirstLetter()}());")
                }
            }
            appendLine("}")
            appendLine("if (!StringHelper.isNullOrEmpty(order)) addOrderBy(order, isAse);")
        }

        val methodCode = """/**
     * 指定查询参数对象及排序字段
     *
     * @param data  查询参数对象
     * @param order 排序字段
     * @param isAse true升序，false降序
     */
     public void SetQueryPara(${metadata.className} data, String order, boolean isAse) {
                $methodBody
            }
        """.trimIndent()

        psiClass.methods.find { it.name == "SetQueryPara" }?.delete()
        val pa = psiClass.constructors.find { it.parameterList.parameters.size > 1 }!!
        pa.addAfter(factory.createMethodFromText(methodCode, psiClass), pa.lastChild)
    }

    private fun generateParamMethods(
        psiClass: PsiClass,
        metadata: EntityDescMetadata,
        factory: PsiElementFactory
    ) {
        // 需要完善，
        metadata.fields.toList().forEachIndexed { index, field ->
            if (field.queryable) { // 只处理 queryable 为 true 的字段
                when (field.type.canonicalText) {
                    "java.lang.String" -> generateStringParamMethods(metadata, field, psiClass, factory, index)
                    "boolean", "java.lang.Boolean" -> generateBooleanParamMethods(
                        metadata,
                        field,
                        psiClass,
                        factory,
                        index
                    )

                    "int", "java.lang.Integer", "long", "java.lang.Long" ->
                        generateNumericParamMethods(metadata, field, psiClass, factory, index)

                    "java.util.Date" -> generateDateParamMethods(metadata, field, psiClass, factory, index)
                    "java.math.BigDecimal" -> generateBigDecimalParamMethods(metadata, field, psiClass, factory, index)
                    "pengesoft.data.DynDataPacket" -> generateDynDataParamMethod(
                        metadata,
                        field,
                        psiClass,
                        factory,
                        index
                    )

                    else -> generateObjectParamMethods(metadata, field, psiClass, factory, index)
                }
            }
        }
    }

    private fun generateStringParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target like ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(String ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("java.lang.String")
        )

        // InEmpty处理
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(${field.field}为empty时也会加入此条件)，不空时(target like ${field.field})，key:${field.field}，为空时(target is null or target = '')，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}InEmpty(String ${field.field}) {
            put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}",
            listOf("java.lang.String")
        )

        // Enum版本
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加${field.desc}枚举条件(target in (${field.field}s))，key:${field.field}_Enum.
             * 
             * @param ${field.field}s ${field.desc}数组条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Enum(String... ${field.field}s) {
            addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}InEmpty",
            listOf("java.lang.String[]")
        )
    }

    private fun generateBooleanParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target = ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(boolean ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("boolean")
        )
    }

    private fun generateNumericParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val type = field.type.canonicalText.removePrefix("java.lang.")
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target = ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}($type ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf(type)
        )

        // 包含0值
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(包含0值)，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}IncZero($type ${field.field}) {
            put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}",
            listOf(type)
        )

        // 枚举参数
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加${field.desc}枚举条件(target in (${field.field}s))，key:${field.field}_Enum.
             * 
             * @param ${field.field}s ${field.desc}数组条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Enum($type... ${field.field}s) {
            addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}IncZero",
            listOf("$type[]")
        )

        // 范围查询
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}范围匹配条件(target between low and high)，key:${field.field}_Range.
             * 
             * @param low  范围下限
             * @param high 范围上限
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Range($type low, $type high) {
            addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, low, high);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}_Enum",
            listOf(type, type)
        )
    }

    private fun generateDateParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target like ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(java.util.Date ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("java.util.Date")
        )

        // 范围查询
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}范围匹配条件(target between startDate and endDate)，key:${field.field}_Range.
             * 
             * @param startDate 起始日期
             * @param endDate   结束日期
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Range(java.util.Date startDate, java.util.Date endDate) {
            addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, startDate, endDate);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}",
            listOf("java.util.Date", "java.util.Date")
        )
    }

    private fun generateBigDecimalParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target = ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(java.math.BigDecimal ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("java.math.BigDecimal")
        )

        // 包含0值
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(包含0值)，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}IncZero(java.math.BigDecimal ${field.field}) {
            put(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}",
            listOf("java.math.BigDecimal")
        )

        // 枚举参数
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加${field.desc}枚举条件(target in (${field.field}s))，key:${field.field}_Enum.
             * 
             * @param ${field.field}s ${field.desc}数组条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Enum(java.math.BigDecimal... ${field.field}s) {
            addParameterByEnum(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field}s);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}IncZero",
            listOf("java.math.BigDecimal[]")
        )

        // 范围查询
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}范围匹配条件(target between low and high)，key:${field.field}_Range.
             * 
             * @param low  范围下限
             * @param high 范围上限
             */public void setParamBy${field.field.capitalizeFirstLetter()}_Range(java.math.BigDecimal low, java.math.BigDecimal high) {
            addParameterByRange(QueryAttr_${field.field.capitalizeFirstLetter()}, low, high);
        }
        """,
            factory,
            "setParamBy${field.field.capitalizeFirstLetter()}_Enum",
            listOf("java.math.BigDecimal", "java.math.BigDecimal")
        )
    }

    private fun generateDynDataParamMethod(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target = ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(DynDataPacket ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("pengesoft.data.DynDataPacket")
        )
    }

    private fun generateObjectParamMethods(
        metadata: EntityDescMetadata,
        field: EntityFieldDescMetadata,
        psiClass: PsiClass,
        factory: PsiElementFactory,
        index: Int
    ) {
        val previousMethodName = if (index > 0) {
            val previousField = metadata.fields[index - 1]
            "setParamBy${previousField.field.capitalizeFirstLetter()}"
        } else {
            null
        }

        // Basic setter
        addMethodAfterPrevious(
            psiClass,
            """/**
             * 增加用${field.desc}匹配条件(target = ${field.field})，key:${field.field}.
             * 
             * @param ${field.field} ${field.desc}匹配条件参数
             */public void setParamBy${field.field.capitalizeFirstLetter()}(Object ${field.field}) {
            addParameter(QueryAttr_${field.field.capitalizeFirstLetter()}, ${field.field});
        }
        """,
            factory,
            previousMethodName,
            listOf("java.lang.Object")
        )
    }

}