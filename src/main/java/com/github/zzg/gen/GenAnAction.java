//package com.github.zzg.gen;
//
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.util.PsiTreeUtil;
//import org.jetbrains.annotations.NotNull;
//
//public class GenAnAction extends AnAction {
//
//    @Override
//    public void actionPerformed(@NotNull AnActionEvent event) {
//        // 获取当前文件
//        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
//        if (psiFile == null) {
//            Messages.showInfoMessage("No file found.", "Error");
//            return;
//        }
//
//        // 获取当前类的 PsiClass
//        PsiClass psiClass = PsiTreeUtil.getParentOfType(psiFile, PsiClass.class);
//        if (psiClass == null) {
//            Messages.showInfoMessage("No class found at the cursor position.", "Info");
//            return;
//        }
//
//        // 检查是否包含目标注解
//        String targetAnnotation = "java.lang.Deprecated"; // 替换为你的目标注解
//        boolean hasAnnotation = hasAnnotation(psiClass, targetAnnotation);
//
//        // 显示结果
//        if (hasAnnotation) {
//            Messages.showInfoMessage("The class contains the annotation: " + targetAnnotation, "Info");
//        } else {
//            Messages.showInfoMessage("The class does not contain the annotation: " + targetAnnotation, "Info");
//        }
//    }
//
//    private boolean hasAnnotation(PsiClass psiClass, String annotationQualifiedName) {
//        PsiAnnotation[] annotations = psiClass.getAnnotations();
//        for (PsiAnnotation annotation : annotations) {
//            if (annotationQualifiedName.equals(annotation.getQualifiedName())) {
//                return true;
//            }
//        }
//        return false;
//    }
//}
