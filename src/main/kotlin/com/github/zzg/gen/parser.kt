package com.github.zzg.gen


import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiClassObjectAccessExpressionImpl
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.idea.base.util.module


@Suppress("unused")
object Parser {
    fun parseEntityDescByPsiClass(psiClass: PsiClass): EntityDescMetadata? {
        // 找到类 再找到对应的QueryPara
        val dir = psiClass.containingFile.containingDirectory
        // 找到Para
        PsiDocumentManager.getInstance(psiClass.project).commitAllDocuments()
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(psiClass.module!!)
        val psiPackage = JavaDirectoryService.getInstance().getPackage(dir)?.qualifiedName
        val queryParaClass = JavaPsiFacade.getInstance(psiClass.project)
            .findClass(psiPackage + "." + psiClass.name + "QueryPara", scope)!!
        val qset = HashSet<String>()
        val oset = HashSet<String>()
        queryParaClass.fields.forEach {
            if (it.name.startsWith("OrderAttr_")) {
                oset.add(it.name.removePrefix("OrderAttr_").decapitalizeFirstLetter())
            }
            if (it.name.startsWith("QueryAttr_")) {
                qset.add(it.name.removePrefix("QueryAttr_").decapitalizeFirstLetter())
            }
        }
        // 找到 xml
        val xmlArr = FilenameIndex.getVirtualFilesByName("mybatis-${psiClass.name}.xml", scope)
        val xmlFile = PsiManager.getInstance(psiClass.project).findFile(ArrayList(xmlArr)[0]) as XmlFile
        // 解析xmlFile
        val insertTag = xmlFile.rootTag?.subTags?.find { it.name == "insert" }!!
        val regex = Regex("insert\\s+into\\s+(\\w+)", RegexOption.IGNORE_CASE)

        // 使用正则表达式匹配 SQL 语句
        val tableName = regex.find(insertTag.text)?.groupValues?.get(1)!!

        val fields = psiClass.fields.map {
            EntityFieldDescMetadata(
                field = it.name,
                type = it.type,
                desc = it.docComment?.text ?: it.name,
                columnName = it.name.capitalizeFirstLetter(),
                primary = false,
                width = 0,
                pass = it.docComment?.text?.contains("跳过") ?: false,
                queryable = qset.contains(it.name),
                sortable = oset.contains(it.name),
                remark = ""
            )
        }.toTypedArray()
        return EntityDescMetadata(
            module = psiClass.module?.name ?: "",
            pkg = psiPackage!!,
            className = psiClass.name!!,
            tbName = tableName,
            desc = "",
            logicDel = true,
            namespace = psiPackage,
            superClass = psiClass.superClassType as PsiType,
            childClass = "",
            fields = fields,
            index = emptyArray()
            )
    }

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
                        columnName = "FlagDel",
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
