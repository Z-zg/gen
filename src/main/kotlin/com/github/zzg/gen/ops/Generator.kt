package com.github.zzg.gen.ops

import com.github.zzg.gen.Context
import com.github.zzg.gen.EntityDescMetadata
import com.github.zzg.gen.findModule
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope

interface Generator {
    val context: Context
    fun findClassInModule(module: Module, packageName: String, className: String): PsiClass? {
        PsiDocumentManager.getInstance(module.project).commitAllDocuments()
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        return JavaPsiFacade.getInstance(module.project).findClass("$packageName.$className", scope)
    }

    fun updateClassPkg(
        psiElementFactory: PsiElementFactory,
        pkg: String,
        psiClass: PsiClass
    ) {
        // Update package statement
        val packageStatement = psiElementFactory.createPackageStatement(pkg)
        // 检查是否已经存在 package 语句
        val existingPackageStatement = psiClass.containingFile.children.find {
            it is PsiPackageStatement
        } as? PsiPackageStatement

        // 如果不存在 package 语句，则添加
        if (existingPackageStatement == null) {
            psiClass.containingFile.addBefore(packageStatement, psiClass)
        } else {
            // 如果存在，检查是否需要更新
            if (existingPackageStatement.packageName != pkg) {
                // 更新 package 语句
                existingPackageStatement.replace(packageStatement)
            }
        }
    }

    fun findMavenModuleOrCurrent(project: Project, moduleName: String):
            Module? {
        val module = findModule(project, moduleName)
        return module ?: ModuleManager.getInstance(project).modules.first()
    }

    fun getOrCreateDirectory(packageName: String, module: Module): PsiDirectory {
        val moduleRootManager = ModuleRootManager.getInstance(module)
        val contentRoots = moduleRootManager.sourceRoots
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

    fun updateClassComment(
        metadata: EntityDescMetadata,
        psiElementFactory: PsiElementFactory,
        psiClass: PsiClass
    ) {
        // Update class comment
        val classCommentText = "/**\n" +
                " * ${metadata.desc}\n" +
                " *\n" +
                " * 文件由鹏业软件模型工具生成(模板名称：JavaAdv),一般不应直接修改此文件.\n" +
                " * Copyright (C) 2008 - 鹏业软件公司\n" +
                " */"
        val classComment = psiElementFactory.createCommentFromText(
            classCommentText, psiClass
        )
        // 检查是否已经存在类似的类注释
        val existingClassComment = psiClass.firstChild as? PsiComment

        // 如果不存在类注释，或者现有的注释内容与新的注释内容不同，则添加或更新
        if (existingClassComment == null || existingClassComment.text != classCommentText) {
            if (existingClassComment != null) {
                // 如果存在类注释但内容不同，则替换
                existingClassComment.replace(classComment)
            } else {
                // 如果不存在类注释，则添加
                psiClass.addBefore(classComment, psiClass.firstChild)
            }
        }
    }

    /**
     * 修改后的生成方法模板
     */
    fun addMethodAfterPrevious(
        psiClass: PsiClass,
        methodCode: String,
        factory: PsiElementFactory,
        previousMethodName: String?,
        previousMethodParams: List<String> = emptyList()
    ) {
        val newMethod = factory.createMethodFromText(methodCode, psiClass)
        val methods = psiClass.methods

        if (previousMethodName == null) {
            // 如果没有前一个方法，则直接添加到类中
            psiClass.add(newMethod)
        } else {
            // 找到前一个方法的位置
            val previousMethod = methods.find { method ->
                method.name == previousMethodName &&
                        method.parameterList.parameters.size == previousMethodParams.size &&
                        method.parameterList.parameters.zip(previousMethodParams).all { (param, expectedType) ->
                            param.type.canonicalText == expectedType
                        }
            }
            if (previousMethod != null) {
                // 在前一个方法后面插入新方法
                previousMethod.parent.addAfter(newMethod, previousMethod)
            } else {
                // 如果找不到前一个方法（理论上不应该发生），直接添加到类中
                psiClass.add(newMethod)
            }
        }
    }

    fun generate(): PsiFile? {
        val module = findMavenModuleOrCurrent(context.project, context.metadata.module)!!
        val psiClass = findOrCreateClass(module, context.metadata)
        assert(psiClass.containingFile.virtualFile != null)
        WriteCommandAction.runWriteCommandAction(module.project) {
            updateClass(psiClass, context.metadata)
        }
        return findOrCreateClass(module, context.metadata).containingFile
    }

    fun createNewClass(module: Module, metadata: EntityDescMetadata): PsiClass
    fun findOrCreateClass(module: Module, metadata: EntityDescMetadata): PsiClass
    fun updateClass(psiClass: PsiClass, metadata: EntityDescMetadata)
}