package com.github.zzg.gen.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JCheckBox

@Suppress("unused")
class MyPluginConfigurable(project: Project) : Configurable {
    private lateinit var panel: JPanel
    private lateinit var outputDirectoryField: JTextField
    private lateinit var generateDTOCheckBox: JCheckBox
    private lateinit var generateDAOCheckBox: JCheckBox
    private lateinit var generateServiceCheckBox: JCheckBox

    private val settings = MyPluginSettings.getInstance(project)

    override fun createComponent(): JComponent {
        panel = JPanel()
        outputDirectoryField = JTextField(settings.outputDirectory, 20)
        generateDTOCheckBox = JCheckBox("Generate DTO", settings.generateDTO)
        generateDAOCheckBox = JCheckBox("Generate DAO", settings.generateDAO)
        generateServiceCheckBox = JCheckBox("Generate Service", settings.generateService)

        panel.add(outputDirectoryField)
        panel.add(generateDTOCheckBox)
        panel.add(generateDAOCheckBox)
        panel.add(generateServiceCheckBox)

        return panel
    }

    override fun isModified(): Boolean {
        return outputDirectoryField.text != settings.outputDirectory ||
                generateDTOCheckBox.isSelected != settings.generateDTO ||
                generateDAOCheckBox.isSelected != settings.generateDAO ||
                generateServiceCheckBox.isSelected != settings.generateService
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        settings.outputDirectory = outputDirectoryField.text
        settings.generateDTO = generateDTOCheckBox.isSelected
        settings.generateDAO = generateDAOCheckBox.isSelected
        settings.generateService = generateServiceCheckBox.isSelected
    }

    override fun reset() {
        outputDirectoryField.text = settings.outputDirectory
        generateDTOCheckBox.isSelected = settings.generateDTO
        generateDAOCheckBox.isSelected = settings.generateDAO
        generateServiceCheckBox.isSelected = settings.generateService
    }

    override fun getDisplayName(): String {
        return "My Plugin Settings"
    }
}