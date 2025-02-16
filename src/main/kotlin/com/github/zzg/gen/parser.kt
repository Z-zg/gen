package com.github.zzg.gen


import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl


@Suppress("unused")
object Parser {
    fun parseEntityDescAnnotation(psiClass: PsiClass): EntityDescMetadata? {
        // 获取 @EntityDesc 注解
        val entityDescAnnotation = psiClass.getAnnotation("org.zq.EntityDesc") ?: return null
        val module = entityDescAnnotation.findAttributeValue("module")?.text?.removeSurrounding("\"")
        // 读取注解属性
        val tbName = entityDescAnnotation.findAttributeValue("tbName")?.text?.removeSurrounding("\"")
        val desc = entityDescAnnotation.findAttributeValue("desc")?.text?.removeSurrounding("\"")
        val logicDel = entityDescAnnotation.findAttributeValue("logicDel")?.text?.toBoolean() ?: true
        val pkg = entityDescAnnotation.findAttributeValue("pkg")?.text?.removeSurrounding("\"")
            ?: (psiClass.containingFile as PsiJavaFile).packageName
        val namespace = entityDescAnnotation.findAttributeValue("namespace")?.text?.removeSurrounding("\"")
        val superClass =
            (entityDescAnnotation.findAttributeValue("superClass") as PsiClassObjectAccessExpressionImpl).type
        val childClass = entityDescAnnotation.findAttributeValue("childClass")?.text?.removeSurrounding("\"")
        val index = entityDescAnnotation.findAttributeValue("index")?.children?.map {
            it.text.removeSurrounding("\"")
        }?.toTypedArray() ?: emptyArray()
        // 返回注解信息
        return psiClass.fields.map { parseEntityFieldDescAnnotation(it)!! }.toTypedArray().let {
            var copy = it
            if (logicDel) {
                copy = copy.plus(
                    EntityFieldDescMetadata(
                        "flagDel",
                        PsiElementFactory.getInstance(psiClass.project).createPrimitiveType("boolean")!!,
                        desc = "删除标识",
                        columnName = "flagDel",
                        primary = false,
                        width = 0,
                        pass = false,
                        queryable = true,
                        sortable = true,
                        remark = ""
                    )
                )
            }
            EntityDescMetadata(
                module = module ?: "",
                className = psiClass.name ?: "",
                tbName = tbName ?: "",
                desc = desc ?: "",
                logicDel = logicDel,
                pkg = pkg ?: "",
                namespace = namespace ?: "",
                superClass = superClass,
                childClass = childClass ?: "",
                fields = copy,
                index = index
            )
        }
    }


    private fun parseEntityFieldDescAnnotation(field: PsiField): EntityFieldDescMetadata? {
        val fieldDescAnnotation = field.getAnnotation("org.zq.EntityFieldDesc") ?: return null
        // 读取注解属性
        val desc = fieldDescAnnotation.findAttributeValue("desc")?.text?.removeSurrounding("\"")
        val columnName = fieldDescAnnotation.findAttributeValue("columnName")?.text?.removeSurrounding("\"")
        val primary = fieldDescAnnotation.findAttributeValue("primary")?.text?.toBoolean() ?: false
        val width = fieldDescAnnotation.findAttributeValue("width")?.text?.toInt() ?: 0
        val pass = fieldDescAnnotation.findAttributeValue("pass")?.text?.toBoolean() ?: false
        val queryable = fieldDescAnnotation.findAttributeValue("queryable")?.text?.toBoolean() ?: true
        val sortable = fieldDescAnnotation.findAttributeValue("sortable")?.text?.toBoolean() ?: true
        val remark = fieldDescAnnotation.findAttributeValue("remark")?.text?.removeSurrounding("\"")

        // 返回注解信息
        return EntityFieldDescMetadata(
            field = field.name,
            type = field.type,
            desc = desc!!,
            columnName = if (columnName.isNullOrBlank()) field.name.capitalizeFirstLetter() else columnName,
            primary = primary,
            width = width,
            pass = pass,
            queryable = queryable,
            sortable = sortable,
            remark = remark ?: ""
        )
    }
}


data class EntityDescMetadata(
    val module: String,
    val pkg: String,
    val className: String,
    val tbName: String,
    val desc: String,
    val logicDel: Boolean,
    val namespace: String,
    val superClass: PsiType?,
    val childClass: String,
    val fields: Array<EntityFieldDescMetadata>,
    val index: Array<String>
)


data class EntityFieldDescMetadata(
    val field: String,
    val type: PsiType,
    val desc: String,
    val columnName: String,
    val primary: Boolean,
    val width: Int,
    val pass: Boolean,
    val queryable: Boolean,
    val sortable: Boolean,
    val remark: String
)
