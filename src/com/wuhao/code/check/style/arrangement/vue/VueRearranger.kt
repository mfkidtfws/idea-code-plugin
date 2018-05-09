/*
 * ©2009-2018 南京擎盾信息科技有限公司 All rights reserved.
 */

package com.wuhao.code.check.style.arrangement.vue

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.arrangement.*
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingRule
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementEntryMatcher
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementExtendableSettings
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementSettings
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementTokens
import com.intellij.util.containers.ContainerUtilRt

/**
 * vue代码排序器
 * @author 吴昊
 * @since
 */
class VueRearranger : Rearranger<ArrangementEntry> {

  private val settingsSerializer = DefaultArrangementSettingsSerializer(getDefaultSettings())

  override fun getBlankLines(settings: CodeStyleSettings, parent: ArrangementEntry?, previous: ArrangementEntry?, target: ArrangementEntry): Int {
    return -1
  }

  override fun getSerializer(): ArrangementSettingsSerializer {
    return settingsSerializer
  }

  override fun parse(root: PsiElement, document: Document?,
                     ranges: MutableCollection<TextRange>,
                     settings: ArrangementSettings): List<ArrangementEntry> {
    // Following entries are subject to arrangement: attribute of tag in vue template.
    val parseInfo = VueArrangementParseInfo()
    root.accept(VueArrangementVisitor(parseInfo, document, ranges, settings))
    return parseInfo.entries
  }

  override fun parseWithNew(
      root: PsiElement, document: Document?,
      ranges: MutableCollection<TextRange>, element: PsiElement,
      settings: ArrangementSettings): Pair<ArrangementEntry, List<ArrangementEntry>>? {
    val existingEntriesInfo = VueArrangementParseInfo()
    root.accept(VueArrangementVisitor(existingEntriesInfo, document, ranges, settings))

    val newEntryInfo = VueArrangementParseInfo()
    element.accept(VueArrangementVisitor(newEntryInfo, document, setOf(element.textRange), settings))
    return if (newEntryInfo.entries.size != 1) {
      null
    } else Pair.create<ArrangementEntry, List<ArrangementEntry>>(newEntryInfo
        .entries[0], existingEntriesInfo.entries)
  }

  companion object {
    private fun getDefaultSettings(): StdArrangementSettings {
      val groupingRules = ContainerUtilRt.newArrayList(ArrangementGroupingRule(StdArrangementTokens.Grouping.GETTERS_AND_SETTERS))
      val matchRules = ContainerUtilRt.newArrayList<StdArrangementMatchRule>()
      val aliasTokens = listOf(StdArrangementRuleAliasToken("visibility").apply {
        definitionRules = listOf(StdArrangementTokens.Modifier.PUBLIC, StdArrangementTokens.Modifier.PACKAGE_PRIVATE, StdArrangementTokens.Modifier.PROTECTED, StdArrangementTokens.Modifier.PRIVATE).map {
          StdArrangementMatchRule(
              StdArrangementEntryMatcher(ArrangementAtomMatchCondition(it))
          )
        }
      })
      return StdArrangementExtendableSettings.createByMatchRules(groupingRules, matchRules, aliasTokens)
    }
  }
}

