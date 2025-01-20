package com.github.zzg.gen.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MyPluginSettings",
    storages = [Storage("my-plugin-settings.xml")]
)
class MyPluginSettings : PersistentStateComponent<MyPluginSettings> {
    var author: String? = "小赵"
    var outputDirectory: String = ""
    var generateDTO: Boolean = true
    var generateDAO: Boolean = true
    var generateService: Boolean = true

    override fun getState(): MyPluginSettings {
        return this
    }

    override fun loadState(state: MyPluginSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    companion object {
        fun getInstance(project: Project): MyPluginSettings {
            return project.getService(MyPluginSettings::class.java)
        }
    }
}