package com.github.zzg.gen.ops

import com.github.zzg.gen.*
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.xml.XmlFile


class MybatisXmlGenerator(override val context: Context) : Generator {

    override fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass {
        TODO("Not yet implemented")
    }

    override fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata) {
        TODO("Not yet implemented")
    }

    override fun generate(): PsiFile {
        val module = findMavenModuleOrCurrent(context.project, context.metadata.module)!!
        val xmlFile = findOrCreateXml(module, context.metadata)
        updateXml(xmlFile, context.metadata)
        return xmlFile
    }

    private fun buildResultMap(): String {
        val metadata = context.metadata
        val resultElements = metadata.fields.map { field ->
            when {
                field.primary -> "<id property=\"${field.field}\" column=\"${field.columnName}\"/>"
                isJsonField(
                    context.project,
                    field
                ) -> "<result property=\"${field.field}.jsonText\" column=\"${field.columnName}\"/>"

                field.pass -> ""
                else -> "<result property=\"${field.field}\" column=\"${field.columnName}\"/>"
            }
        }
        return """<resultMap type="${metadata.pkg}.${metadata.className}" id="${metadata.className.decapitalizeFirstLetter()}ResultMap">
            ${resultElements.joinToString("\n            ")}
        </resultMap>
        """.trimIndent()
    }

    private fun isJsonField(project: Project, field: EntityFieldDescMetadata): Boolean {
        val psiFacade = JavaPsiFacade.getInstance(project)

        // 查找目标类 pengesoft.data.DataPacket
        val dataPacketClass = psiFacade.findClass("pengesoft.data.DataPacket", GlobalSearchScope.allScope(project))
            ?: return false // 如果找不到目标类，直接返回 false

        // 将 PsiType 转换为 PsiClass
        val typeClass = PsiTypesUtil.getPsiClass(field.type) ?: return false

        // 检查 typeClass 是否是 DataPacket 的子类
        return dataPacketClass.isInheritor(typeClass, true) || typeClass.isInheritor(dataPacketClass, true)
    }

    fun xmlContent(): String {
        val metadata = context.metadata
        return """<?xml version="1.0" encoding="UTF-8" ?>
        <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
        <mapper namespace="${metadata.pkg}.dao.${metadata.className}Dao">
            ${buildResultMap()}
            ${buildSqlColumns("BaseCol")}
            ${buildSqlColumns("AllCol")}
            ${buildJsonPathAndWhere()}
            ${buildWhereSql()} 
            ${buildInsertAndUpdatePartStatement()}
            ${buildCRUDStatement()}
        </mapper>
        """.trimIndent()
    }
    private fun buildCRUDStatement(): String {
        val key = context.metadata.fields.find { it.primary }!!
        return """<insert id="insert${context.metadata.className}" parameterType="${context.metadata.pkg}.${context.metadata.className}Detail">
        insert into ${context.metadata.tbName}(
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}InsertCol"/>
        )values(
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}InsertValues"/>
        )
    </insert>

    <update id="update${context.metadata.className}" parameterType="${context.metadata.pkg}.${context.metadata.className}Detail">
        update ${context.metadata.tbName} set
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}UpdateValues"/>
        where ${key.columnName}=#{${key.field}},jdbcType=${getJdbcType(key)}}
    </update>

    <delete id="delete${context.metadata.className}" parameterType="${context.metadata.pkg}.${context.metadata.className}Detail">
        delete from ${context.metadata.tbName} where ${key.columnName}=#{${key.field}},jdbcType=${getJdbcType(key)}}
    </delete>

    <select id="getBase${context.metadata.className}" parameterType="${context.metadata.pkg}.${context.metadata.className}Detail" resultMap="${context.metadata.className.decapitalizeFirstLetter()}ResultMap">
        select
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}BaseCol" />
        from ${context.metadata.tbName} t where ${key.columnName}=#{${key.field}},jdbcType=${getJdbcType(key)}}
    </select>

    <select id="get${context.metadata.className}" parameterType="${context.metadata.pkg}.${context.metadata.className}Detail" resultMap="${context.metadata.className.decapitalizeFirstLetter()}ResultMap">
        select t.* from ${context.metadata.tbName} t where ${key.columnName}=#{${key.field}},jdbcType=${getJdbcType(key)}}
    </select>

    <select id="query${context.metadata.className}Count" parameterType="java.util.Map" resultType="int">
        select count(*) from ${context.metadata.tbName} t
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}WhereSql" />
    </select>

    <select id="queryBase${context.metadata.className}List" parameterType="java.util.Map" resultMap="${context.metadata.className.decapitalizeFirstLetter()}ResultMap">
        select
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}BaseCol" />
        from ${context.metadata.tbName} t
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}WhereSql" />
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}OrderSql" />
    </select>

    <select id="query${context.metadata.className}List" parameterType="java.util.Map" resultMap="${context.metadata.className.decapitalizeFirstLetter()}ResultMap">
        select t.* from ${context.metadata.tbName} t
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}WhereSql" />
        <include refid="${context.metadata.pkg}.dao.${context.metadata.className.decapitalizeFirstLetter()}DetailDao.${context.metadata.className.decapitalizeFirstLetter()}OrderSql" />
    </select>"""
    }

    private fun getJdbcType(field: EntityFieldDescMetadata): String {
        return when (field.type.canonicalText) {
            "java.lang.String" -> "VARCHAR"
            "java.math.BigDecimal" -> "DECIMAL"
            "java.util.Date" -> "TIMESTAMP"
            "int","java.lang.Integer" -> "INTEGER"
            "java.lang.Boolean" -> "BOOLEAN"
            else -> "VARCHAR"
        }
    }

    private fun buildInsertAndUpdatePartStatement(): String {
        val res = context.metadata.fields.filter { !it.pass }.map { field ->
            field.columnName
        }
        val res1 = context.metadata.fields.filter { !it.pass }.map { field ->
            when {
                isJsonField(context.project, field) -> """#{${field.field}.jsonText,jdbcType=VARCHAR}"""
                else -> """#{${field.field},jdbcType=${getJdbcType(field)}}"""
            }
        }
        val res2 = context.metadata.fields.filter { !it.pass && !it.primary }.map { field ->
            when {
                isJsonField(context.project, field) -> """${field.field.capitalizeFirstLetter()} = #{${field.field}.jsonText,jdbcType=VARCHAR}"""
                else -> """${field.columnName} = #{${field.field},jdbcType=${getJdbcType(field)}}"""
            }
        }
        return """<sql id="${context.metadata.className.decapitalizeFirstLetter()}InsertCol">
            ${res.joinToString(",\n")}
            </sql>
            <sql id="${context.metadata.className.decapitalizeFirstLetter()}InsertValues">
            ${res1.joinToString(",\n")}
            </sql>
            <sql id="${context.metadata.className.decapitalizeFirstLetter()}UpdateValues">
            ${res2.joinToString(",\n")}
            </sql>
        """.trimIndent()
    }

    private fun buildJsonPathAndWhere(): String {
        return """<sql id="jsonQueryWhereSqlInner">
            <if test="${'$'}{JsonQueryFieldName}_JsonProp != null">
                <foreach collection="${'$'}{JsonQueryFieldName}_JsonProp" item="item">
                    <if test="item.propType == null or item.propType == '' or item.propType == 'string'">
                        and JSON_VALUE(t.${'$'}{JsonColName}, '${'$'}{item.propPath}') like #{item.propVal}
                    </if>
                    <if test="item.propType == 'date' or item.propType == 'number'">
                        and JSON_VALUE(t.${'$'}{JsonColName}, '${'$'}{item.propPath}') &gt; #{item.queryRangeStart} 
                        and JSON_VALUE(t.${'$'}{JsonColName}, '${'$'}{item.propPath}') &lt; #{item.queryRangeEnd}
                    </if>
                    <if test="item.propType == 'enum'">
                        and JSON_VALUE(t.${'$'}{JsonColName}, '${'$'}{item.propPath}') in
                        <foreach collection="item.propVal" item="it" open="(" separator="," close=")">
                            #{it}
                        </foreach>
                    </if>
                </foreach>
            </if>
        </sql>
        <sql id="${context.metadata.className.decapitalizeFirstLetter()}WhereSql">
        <trim prefix="WHERE" prefixOverrides="and or ">
            <include refid="${context.metadata.pkg}.dao.${context.metadata.className}Dao.${context.metadata.className}WhereSqlInner"/>
            <if test="_default_mulattr != null">
                and (
                <foreach collection="_default_mulattr" item="item" separator=" or ">
                    <include refid="${context.metadata.pkg}.dao.${context.metadata.className}Dao.${context.metadata.className}WhereSqlInnerOr"/>
                </foreach>
                )
            </if>
        </trim>
    </sql>
    """.trimIndent()
    }

    private fun buildWhereSql(): String {
        val res1 = context.metadata.fields.filter { it.queryable }.map { field ->
            when (field.type.canonicalText) {
                "java.lang.String" -> """<if test="${field.field} != null">
                                            <if test="${field.field} == ''">
                                               and (t.${field.columnName} is null or t.${field.columnName} = '')
                                            </if>
                                            <if test="${field.field} != ''">
                                                and t.${field.columnName} like #{${field.field}}
                                            </if>
                                            </if>
                                            <if test="${field.field}_Enum != null">
                                                and t.${field.columnName} in
                                                <foreach collection="${field.field}_Enum" item="item" open="(" separator="," close=")">#{item}</foreach>
                                            </if>
                                            <if test="${field.field}_EnumLike != null">
                                                and <foreach collection="${field.field}_EnumLike" item="item" open="(" separator=" or " close=")">t.${field.columnName} like #{item}</foreach>
                                            </if>"""

                "java.util.Date" -> """<if test="${field.field} != null">and t.${field.columnName} = #{${field.field}}</if>
        <if test="${field.field}_S != null"><![CDATA[ and t.${field.columnName} > #{${field.field}_S} and t.${field.columnName} < #{${field.field}_E} ]]></if>"""

                "int", "long", "short", "java.lang.Integer", "java.java.Long", "java.math.BigDecimal" -> """<if test="${field.field} != null">and t.${field.columnName} = #{${field.field}}</if>
        <if test="${field.field}_Enum != null">
            and t.${field.columnName} in
            <foreach collection="${field.field}_Enum" item="item" open="(" separator="," close=")">#{item}</foreach>
        </if>
        <if test="${field.field}_L != null"><![CDATA[ and t.${field.columnName} >= #{${field.field}_L} and t.${field.columnName} <= #{${field.field}_H} ]]></if>"""

                "java.lang.Boolean", "boolean" -> """<if test="${field.field} != null">and t.${field.columnName} = #{${field.field}}</if>"""
                else -> ""
            }
        }
        val res2 = context.metadata.fields.filter { it.queryable }.map { field ->
            when (field.type.canonicalText) {
                "java.lang.String", "String" -> """<if test="item.name == '${field.field}'">t.${field.columnName} like #{item.value}</if>"""
                else -> """<if test="item.name == '${field.field}'">t.${field.columnName} = #{item.value}</if>"""
            }
        }
        val res3 = context.metadata.fields.filter { it.sortable }.map { field ->
            """<if test="item == '${field.field}'">t.${field.columnName}</if>
                <if test="item == '${field.field}_D'">t.${field.columnName} desc</if>"""
        }
        return """<sql id="${context.metadata.className.decapitalizeFirstLetter()}WhereSqlInner">
            ${res1.joinToString("\n")}
            </sql>
            <sql id="${context.metadata.className.decapitalizeFirstLetter()}WhereSqlInnerOr">
            ${res2.joinToString("\n")}
            </sql>
            
            <sql id="${context.metadata.className.decapitalizeFirstLetter()}OrderSql">
                <trim prefix="ORDER BY" suffixOverrides=",">
                    <if test="_orderBys != null">
                        <foreach collection="_orderBys" item="item" open="" separator="," close="">
                            <include refid="${context.metadata.pkg}.dao.${context.metadata.className}Dao.${context.metadata.className}OrderSqlInner"/>
                        </foreach>
                    </if>
                </trim>
            </sql>
            
            <sql id="eaAreaContractOrderSqlInner">
               ${res3.joinToString("\n")}
            </sql>
        """.trimMargin()
    }

    private fun buildSqlColumns(type: String): String {
        val elements = context.metadata.fields.map { field ->
            when {
                "BaseCol" == type && (isJsonField(
                    context.project,
                    field
                ) || field.pass) -> ""

                field.pass -> ""
                else -> "t.${field.field.capitalizeFirstLetter()}"
            }
        }
        return """<sql id = "${context.metadata.className.decapitalizeFirstLetter()}${type}">
           ${elements.joinToString(",")}
        </sql>""".trimMargin();

    }

    private fun updateXml(xmlFile: XmlFile, metadata: EntityDescMetadata) {

    }

    private fun findOrCreateXml(module: Module, metadata: EntityDescMetadata): XmlFile {
        PsiDocumentManager.getInstance(module.project).commitAllDocuments()
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val xmls = FilenameIndex.getVirtualFilesByName("mybatis-${metadata.className}.xml", scope)
        if (xmls.isEmpty()) {
            // 创建
            WriteCommandAction.runWriteCommandAction<XmlFile>(module.project) {
                // 使用 PsiFileFactory 创建文件
                val factory = PsiFileFactory.getInstance(module.project)
                // 假设文件名为 "example.xml"，并提供初始内容
                val psiFile: PsiFile =
                    factory.createFileFromText("mybatis-${metadata.className}.xml", XmlFileType.INSTANCE, xmlContent())
                getOrCreateDirectory("${context.metadata.pkg}.dao", module).add(psiFile)
                psiFile as XmlFile
            }
            return findOrCreateXml(module, metadata)
        }
        return PsiManager.getInstance(module.project).findFile(ArrayList(xmls)[0]) as XmlFile
    }
}