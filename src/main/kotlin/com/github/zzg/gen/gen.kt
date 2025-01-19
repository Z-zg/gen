@file:Suppress("unused")

package com.github.zzg.gen

import com.github.zzg.gen.config.MyPluginSettings
import com.intellij.psi.PsiFile


interface Generator {
    val myPluginSettings: MyPluginSettings
    val metadata: EntityDescMetadata
    fun generate(): PsiFile?
}

class EntityGenerator(
    override val myPluginSettings: MyPluginSettings,
    override val metadata: EntityDescMetadata,
) : Generator {
    override fun generate(): PsiFile? {
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