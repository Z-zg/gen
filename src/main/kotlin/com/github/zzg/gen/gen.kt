@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.intellij.psi.PsiFile
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter


interface Generator {
    val myPluginSettings: MyPluginSettings
    val metadata: EntityDescMetadata
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

class EntityGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        val velocityEngine = VelocityEngine()
        velocityEngine.init()
        val context = VelocityContext()
        // 将 Kotlin 数据类的属性填充到 VelocityContext
        context.put("className", metadata.className);
        context.put("tbName", metadata.tbName);
        context.put("desc", metadata.desc);
        context.put("logicDel", metadata.logicDel);
        context.put("pkg", metadata.pkg);
        context.put("namespace", metadata.namespace);
        context.put("superClass", metadata.superClass);
        context.put("fields", metadata.fields);

        // 3. 加载模板
        val template: Template = velocityEngine.getTemplate("templates/EntityTemplate.vm")
        // 4. 生成代码
        val writer = StringWriter()
        template.merge(context, writer)
        // 5. 输出生成的代码
        println(writer.toString())
        return null
    }
}

class EntityQueryParaGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class EntityQueryExParaGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class DaoGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IDaoGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class BaseSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IBaseSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MybatisXmlGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MybatisXmlExGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class MgeSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IMgeSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class ControllerSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IControllerSvrGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class BusinessGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class IBusinessGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class TsGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}

class TsControllerGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
        return null
    }
}