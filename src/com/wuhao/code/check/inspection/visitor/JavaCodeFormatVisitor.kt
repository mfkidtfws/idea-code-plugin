/*
 * ©2009-2018 南京擎盾信息科技有限公司 All rights reserved.
 */

package com.wuhao.code.check.inspection.visitor

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.PsiPrimitiveType.*
import com.intellij.psi.javadoc.PsiDocComment
import com.wuhao.code.check.*
import com.wuhao.code.check.inspection.CodeFormatInspection
import com.wuhao.code.check.inspection.fix.ExtractToVariableFix
import com.wuhao.code.check.inspection.fix.JavaBlockCommentFix
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

/**
 * Created by 吴昊 on 18-4-26.
 */
class JavaCodeFormatVisitor(holder: ProblemsHolder) : BaseCodeFormatVisitor(holder) {

  override fun support(language: Language): Boolean {
    return language == JavaLanguage.INSTANCE
  }

  override fun visitElement(element: PsiElement) {
    when (element) {
      is PsiClass -> {
        if (element.annotations.any { it.qualifiedName == "Entity" || it.qualifiedName == "Table" }) {
          element.fields.filter {
            !it.hasModifier(JvmModifier.STATIC) && it.hasModifier(JvmModifier.PRIVATE)
                && it.firstChild !is PsiDocComment
          }.forEach { fieldElement ->
            holder.registerProblem(fieldElement, Messages.commentRequired, ProblemHighlightType.GENERIC_ERROR,
                JavaBlockCommentFix())
          }
        }
      }
      is PsiIdentifier -> {
        //变量名不能少于2个字符
        if (element.text.length <= 1) {
          if (element.parent.getChildOfType<PsiTypeElement>() == null
              || element.parent.getChildOfType<PsiTypeElement>()!!.text != "Exception") {
//            holder.registerProblem(element, "变量名称不能少于2个字符", ProblemHighlightType.GENERIC_ERROR)
          }
        }
      }
      is PsiLiteralExpression -> {
        // 检查数字参数
        if (element.parent is PsiExpressionList
            && element.text != "0"
            && element.text != "0L" && element.type in PRIMITIVE_TYPES) {
          holder.registerProblem(element, "不允许直接使用数字作为方法参数",
              ProblemHighlightType.GENERIC_ERROR,
              ExtractToVariableFix())
        }
      }
      is PsiMethodCallExpression -> {
        // 使用日志输入代替System.out
        if (element.text.startsWith("System.out") || element.text.startsWith("System.err")) {
          if (element.ancestorOfType<PsiMethod>() == null
              || !element.ancestorsOfType<PsiMethod>().any { func ->
                func.annotations.any { annotation ->
                  annotation.qualifiedName == JUNIT_TEST_ANNOTATION_CLASS_NAME
                }
              }
          ) {
            holder.registerProblem(element, "使用日志向控制台输出", ProblemHighlightType.GENERIC_ERROR, object : LocalQuickFix {

              override fun getFamilyName(): String {
                return "替换为日志输出"
              }

              override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                val el = descriptor.endElement
                val factory = getPsiElementFactory(element)
                if (el.firstChild is PsiReferenceExpression) {
                  if (el.firstChild.text.startsWith("System.out.print")) {
                    el.firstChild.replace(factory.createExpressionFromText("LOG.info", null))
                  } else if (el.firstChild.text.startsWith("System.err.print")) {
                    el.firstChild.replace(factory.createExpressionFromText("LOG.error", null))
                  }
                }
              }
            })
          }
        }
      }
      is PsiMethod -> {
        // 方法长度不能超过指定长度
        if (element.getLineCount() > CodeFormatInspection.MAX_LINES_PER_FUNCTION) {
          holder.registerProblem(element, "方法长度不能超过${CodeFormatInspection.MAX_LINES_PER_FUNCTION}行", ProblemHighlightType.GENERIC_ERROR)
        }
        // 接口方法必须包含注释
        val elClass = element.containingClass
        if (elClass != null && elClass.isInterface && element.firstChild !is PsiDocComment) {
          holder.registerProblem(element, Messages.commentRequired, ProblemHighlightType.GENERIC_ERROR,
              JavaBlockCommentFix())
        }
      }
    }
  }

  companion object {

    val PRIMITIVE_TYPES = setOf(LONG, INT, DOUBLE, FLOAT, BYTE, SHORT)
  }
}
