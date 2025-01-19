package com.github.zzg.gen


import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
@Suppress("unused")
object Parser {
    fun parseEntityDescAnnotation(psiClass: PsiClass): EntityDescMetadata? {
        // 获取 @EntityDesc 注解
        val entityDescAnnotation = psiClass.getAnnotation("org.zq.EntityDesc") ?: return null

        // 读取注解属性
        val tbName = entityDescAnnotation.findAttributeValue("tbName")?.text?.removeSurrounding("\"")
        val desc = entityDescAnnotation.findAttributeValue("desc")?.text?.removeSurrounding("\"")
        val logicDel = entityDescAnnotation.findAttributeValue("logicDel")?.text?.toBoolean() ?: true
        val pkg = entityDescAnnotation.findAttributeValue("pkg")?.text?.removeSurrounding("\"")
        val namespace = entityDescAnnotation.findAttributeValue("namespace")?.text?.removeSurrounding("\"")
        val superClass = entityDescAnnotation.findAttributeValue("superClass")?.text?.removeSurrounding("\"")
        val childClass = entityDescAnnotation.findAttributeValue("childClass")?.text?.removeSurrounding("\"")
        val index = entityDescAnnotation.findAttributeValue("index")?.children?.map {
            it.text.removeSurrounding("\"")
        }?.toTypedArray() ?: emptyArray()
        // 返回注解信息
        return EntityDescMetadata(
            tbName = tbName ?: "",
            desc = desc ?: "",
            logicDel = logicDel,
            pkg = pkg ?: "",
            namespace = namespace ?: "",
            superClass = superClass ?: "",
            childClass = childClass ?: "",
            index = index
        )
    }


    fun parseEntityFieldDescAnnotation(field: PsiField): EntityFieldDescMetadata? {
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
            desc = desc ?: "",
            columnName = columnName ?: "",
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
    val tbName: String,
    val desc: String,
    val logicDel: Boolean,
    val pkg: String,
    val namespace: String,
    val superClass: String,
    val childClass: String,
    val index: Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityDescMetadata

        if (tbName != other.tbName) return false
        if (desc != other.desc) return false
        if (logicDel != other.logicDel) return false
        if (pkg != other.pkg) return false
        if (namespace != other.namespace) return false
        if (superClass != other.superClass) return false
        if (childClass != other.childClass) return false
        if (!index.contentEquals(other.index)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tbName.hashCode()
        result = 31 * result + desc.hashCode()
        result = 31 * result + logicDel.hashCode()
        result = 31 * result + pkg.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + superClass.hashCode()
        result = 31 * result + childClass.hashCode()
        result = 31 * result + index.contentHashCode()
        return result
    }
}


data class EntityFieldDescMetadata(
    val desc: String,
    val columnName: String,
    val primary: Boolean,
    val width: Int,
    val pass: Boolean,
    val queryable: Boolean,
    val sortable: Boolean,
    val remark: String
)