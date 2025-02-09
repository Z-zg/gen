package com.github.zzg.gen.ops

import com.github.zzg.gen.*
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

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
            field.let {
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
                        val last = psiClass.fields.find { f -> f.name == metadata.fields[index - 1].field }
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
            genGetterAndSetter(field, index, metadata, psiElementFactory, psiClass)
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
            metadata.fields.none { it.field == field.name }.ifTrue {
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
                val fieldName1 = metadata.fields[index - 1].field
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
        }
        return findOrCreateClass(module, metadata)
    }
}