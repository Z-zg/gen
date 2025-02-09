@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.github.zzg.gen.ops.Generator
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*

fun String.capitalizeFirstLetter(): String {
    if (isEmpty()) return this
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}

fun String.decapitalizeFirstLetter(): String {
    if (isEmpty()) return this
    return this.replaceFirstChar {
        if (it.isUpperCase()) it.lowercase() else it.toString()
    }
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

fun findModule(project: Project, moduleName: String):
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