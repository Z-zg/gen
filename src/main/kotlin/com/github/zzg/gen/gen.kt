@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.search.GlobalSearchScope


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

private fun findModule(project: Project, moduleName: String):
        Module? {
    return ModuleManager.getInstance(project).modules.find { it.name == moduleName }
}

private fun findMavenModuleOrCurrent(project: Project, moduleName: String):
        Module? {
    val module = findModule(project, moduleName)
    return module ?: ModuleManager.getInstance(project).modules.first()
}

private fun findClassInModule(module: Module, packageName: String, className: String): PsiClass? {
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    return JavaPsiFacade.getInstance(module.project).findClass("$packageName.$className", scope)
}

private fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
    val psiClass = findClassInModule(module, metadata.pkg, metadata.className)
    return psiClass ?: createNewClass(module, metadata)
}

private fun getOrCreateDirectory(packageName: String, module: Module): PsiDirectory {
    val moduleRootManager = ModuleRootManager.getInstance(module)
    val contentRoots = moduleRootManager.contentRoots
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

private fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
    val psiElementFactory = PsiElementFactory.getInstance(module.project)
    val psiClass = psiElementFactory.createClass(metadata.className)
    val psiFile = PsiFileFactory.getInstance(module.project).createFileFromText(
        "${metadata.className}.java",
        JavaFileType.INSTANCE,
        psiElementFactory.createPackageStatement(metadata.pkg).text + psiClass.text
    ) as PsiJavaFile

    val directory = getOrCreateDirectory(metadata.pkg, module)
    directory.add(psiFile)
    return psiFile.classes.first()
}

private fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
    val psiElementFactory = PsiElementFactory.getInstance(psiClass.project)

    // Update package statement
    val packageStatement = psiElementFactory.createPackageStatement(metadata.pkg)
    psiClass.containingFile.addBefore(packageStatement, psiClass)

    // Update class comment
    val classComment = psiElementFactory.createCommentFromText(
        "/**\n" +
                " * ${metadata.desc}\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaAdv),一般不应直接修改此文件.\n" +
                " * Copyright (C) 2008 - 鹏业软件公司\n" +
                " */", psiClass
    )
    psiClass.addBefore(classComment, psiClass.firstChild)

    // Add fields
    metadata.fields.forEach { field ->
        field?.let {
            val existingField = psiClass.fields.find { f -> f.name == it.columnName }
            if (existingField == null) {
                val newField = psiElementFactory.createField(
                    it.columnName,
                    psiElementFactory.createTypeFromText("String", psiClass)
                )
                newField.addBefore(
                    psiElementFactory.createCommentFromText("// ${it.desc}", psiClass),
                    newField.firstChild
                )
                psiClass.add(newField)
            }
        }
    }

    // Add getters and setters
    // Add getters and setters
    metadata.fields.forEach { field ->
        field?.let {
            val fieldName = it.columnName
            val capitalizedFieldName = fieldName.capitalize()

            // Generate getter method
            val getterComment = psiElementFactory.createCommentFromText(
                "/**\n" +
                        " * 获取 ${it.desc}\n" +
                        " *\n" +
                        " * @return ${it.desc}\n" +
                        " */", psiClass
            )
            val getter = psiElementFactory.createMethodFromText(
                "public String get$capitalizedFieldName() {\n" +
                        "    return this.$fieldName;\n" +
                        "}",
                psiClass
            )
            if (psiClass.methods.none { it.name == getter.name }) {
                psiClass.add(getterComment)
                psiClass.add(getter)
            }

            // Generate setter method
            val setterComment = psiElementFactory.createCommentFromText(
                "/**\n" +
                        " * 设置 ${it.desc}\n" +
                        " *\n" +
                        " * @param $fieldName ${it.desc}\n" +
                        " */", psiClass
            )
            val setter = psiElementFactory.createMethodFromText(
                "public void set$capitalizedFieldName(String $fieldName) {\n" +
                        "    this.$fieldName = $fieldName;\n" +
                        "}",
                psiClass
            )
            if (psiClass.methods.none { it.name == setter.name }) {
                psiClass.add(setterComment)
                psiClass.add(setter)
            }
        }
    }

    // Add clear method
    val clearMethod = psiElementFactory.createMethodFromText(
        """
            @Override
            public void clear() {
                super.clear();
                ${metadata.fields.joinToString("\n") { "this.${it?.columnName} = null;" }}
            }
            """.trimIndent(),
        psiClass
    )
    if (psiClass.methods.none { it.name == clearMethod.name }) {
        psiClass.add(clearMethod)
    }

    // Add assignFrom method
    val assignFromMethod = psiElementFactory.createMethodFromText(
        """
            @Override
            public void assignFrom(${metadata.className} sou) {
                super.assignFrom(sou);
                if (!(sou instanceof ${metadata.className})) {
                    return;
                }
                ${metadata.className} s = (${metadata.className}) sou;
                ${metadata.fields.joinToString("\n") { "this.${it?.columnName} = s.${it?.columnName};" }}
            }
            """.trimIndent(),
        psiClass
    )
    if (psiClass.methods.none { it.name == assignFromMethod.name }) {
        psiClass.add(assignFromMethod)
    }

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
    if (psiClass.methods.none { it.name == toStringMethod.name }) {
        psiClass.add(toStringMethod)
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
        findMavenModuleOrCurrent(project, context.metadata.module) ?: run {
            Messages.showErrorDialog(project, "Module not found", "Error")
            return
        }
        val psiClass = findOrCreateClass(module, context.metadata)
        updateClass(psiClass, context.metadata)
        return null
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