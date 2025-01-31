@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue


interface Generator {
    val context: Context
    fun generate(): PsiFile?
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

public fun findMavenModuleOrCurrent(project: Project, moduleName: String):
        Module? {
    val module = findModule(project, moduleName)
    return module ?: ModuleManager.getInstance(project).modules.first()
}

public fun findClassInModule(module: Module, packageName: String, className: String): PsiClass? {
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    return JavaPsiFacade.getInstance(module.project).findClass("$packageName.$className", scope)
}

public fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
    val psiClass = findClassInModule(module, metadata.pkg, metadata.className)
    return psiClass ?: createNewClass(module, metadata)
}

public fun getOrCreateDirectory(packageName: String, module: Module): PsiDirectory {
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

public fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
    WriteCommandAction.runWriteCommandAction(module.project) {
        val psiElementFactory = PsiElementFactory.getInstance(module.project)
        val psiClass = psiElementFactory.createClass(metadata.className)
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

fun genCleanItem(field: EntityFieldDescMetadata?): String {
    return field?.let {
        val defaultValue = getDefaultValueForType(field.type)
        return "this.${field.field} = $defaultValue;"
    } ?: ""
}

fun genAssignFromItem(field: EntityFieldDescMetadata?): String {
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

private fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
    val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)
    updateClassPkg(psiElementFactory, metadata, psiClass)
    updateClassComment(metadata, psiElementFactory, psiClass)
    updateEntityFields(metadata, psiClass, psiElementFactory)
    PsiDocumentManager.getInstance(psiClass.project).commitAllDocuments()
    psiClass.containingFile.virtualFile?.refresh(false, false)

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
        """
            @Override
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
    val capitalizedFieldName = fieldName.capitalize()
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
        if (index == 0){
            psiClass.add(getter)
        }else{
            val fieldName1 = metadata.fields[index - 1]?.field ?: ""
            val setter1 = psiClass.methods.find { it.name == "set$fieldName1" }
            if (setter1 == null){
                psiClass.add(getter)
            }else{
                setter1.addAfter(getter, setter1.lastChild)
            }
        }
    }
}

private fun updateClassPkg(
    psiElementFactory: PsiElementFactory,
    metadata: EntityDescMetadata,
    psiClass: PsiClass
) {
    // Update package statement
    val packageStatement = psiElementFactory.createPackageStatement(metadata.pkg)
    // 检查是否已经存在 package 语句
    val existingPackageStatement = psiClass.containingFile.children.find {
        it is PsiPackageStatement
    } as? PsiPackageStatement

    // 如果不存在 package 语句，则添加
    if (existingPackageStatement == null) {
        psiClass.containingFile.addBefore(packageStatement, psiClass)
    } else {
        // 如果存在，检查是否需要更新
        if (existingPackageStatement.packageName != metadata.pkg) {
            // 更新 package 语句
            existingPackageStatement.replace(packageStatement)
        }
    }
}

private fun updateClassComment(
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


data class Context(
    val myPluginSettings: MyPluginSettings,
    val metadata: EntityDescMetadata,
    val psiClass: PsiClass,
    val project: Project
)

class EntityGenerator(
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
}

class EntityQueryParaGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class EntityQueryExParaGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class DaoGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IDaoGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class BaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IBaseSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MybatisXmlGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MybatisXmlExGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MgeSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IMgeSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class ControllerSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IControllerSvrGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class BusinessGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IBusinessGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class TsGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class TsControllerGenerator(
    override val context: Context
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}