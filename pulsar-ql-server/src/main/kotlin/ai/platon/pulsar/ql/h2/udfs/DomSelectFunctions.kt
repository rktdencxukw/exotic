package ai.platon.pulsar.ql.h2.udfs

import ai.platon.pulsar.common.RegexExtractor
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.dom.nodes.A_LABELS
import ai.platon.pulsar.dom.select.appendSelectorIfMissing
import ai.platon.pulsar.dom.select.first
import ai.platon.pulsar.dom.select.select2
import ai.platon.pulsar.ql.annotation.UDFGroup
import ai.platon.pulsar.ql.annotation.UDFunction
import ai.platon.pulsar.ql.h2.Queries
import ai.platon.pulsar.ql.types.ValueDom
import org.h2.value.ValueArray
import org.h2.value.ValueString
import org.jsoup.nodes.Element

/**
 * Created by vincent on 17-11-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
@Suppress("unused")
@UDFGroup(namespace = "DOM")
object DomSelectFunctions {

    @UDFunction(description = "Select the first element from a DOM by the given css query and return a DOM")
    @JvmStatic
    fun selectFirst(dom: ValueDom, cssQuery: String): ValueDom {
        if (dom.isNil) {
            return dom
        }

        val element = dom.element.first(cssQuery)
        return ValueDom.getOrNil(element)
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query and return a DOM")
    @JvmStatic
    fun selectNth(dom: ValueDom, cssQuery: String, n: Int): ValueDom {
        return if (dom.isNil) {
            dom
        } else ValueDom.getOrNil(nthElement(dom.element, cssQuery, n))
    }

    @UDFunction(description = "Select all elements from a DOM by the given css query and return the the element texts")
    @JvmStatic
    fun allTexts(dom: ValueDom, cssQuery: String): ValueArray {
        return Queries.select(dom, cssQuery) { it.text() }
    }

    @UDFunction(description = "Select the first element from a DOM by the given css query and return the element text")
    @JvmStatic
    fun firstText(dom: ValueDom, cssQuery: String): String {
        return Queries.selectFirst(dom, cssQuery) { it.text() } ?: ""
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query and return the element text")
    @JvmStatic
    fun nthText(dom: ValueDom, cssQuery: String, n: Int): String {
        return Queries.selectNth(dom, cssQuery, n) { it.text() } ?: ""
    }

    @UDFunction(description = "Select the first element from a DOM by the given css query " +
            "and try to extract an integer from the element text")
    @JvmStatic
    fun firstInteger(dom: ValueDom, cssQuery: String, defaultValue: Int = 0): Int {
        val s = firstText(dom, cssQuery)
        return Strings.getFirstInteger(s, defaultValue)
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query " +
            "and try to extract an integer from the element text")
    @JvmStatic
    @JvmOverloads
    fun nthInteger(dom: ValueDom, cssQuery: String, n: Int, defaultValue: Int = 0): Int {
        val s = nthText(dom, cssQuery, n)
        return Strings.getFirstInteger(s, defaultValue)
    }

    @UDFunction(description = "Select the first element from a DOM by the given css query " +
            "and try to extract an number from the element text")
    @JvmStatic
    fun firstNumber(dom: ValueDom, cssQuery: String, defaultValue: Float = 0.0f): Float {
        val s = firstText(dom, cssQuery)
        return Strings.getFirstFloatNumber(s, defaultValue)
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query " +
            "and try to extract an number from the element text")
    @JvmStatic
    @JvmOverloads
    fun nthNumber(dom: ValueDom, cssQuery: String, n: Int, defaultValue: Float = 0.0f): Float {
        val s = nthText(dom, cssQuery, n)
        return Strings.getFirstFloatNumber(s, defaultValue)
    }

    @UDFunction(description = "Select all the element from a DOM by the given css query " +
            "and return the attribute value associated by the attribute name")
    @JvmStatic
    @JvmOverloads
    fun allAttrs(dom: ValueDom, cssQuery: String = ":root", attrName: String): ValueArray {
        return Queries.select(dom, cssQuery) { it.attr(attrName) }
    }

    @UDFunction(description = "Select the first element from a DOM by the given css query " +
            "and return the attribute value associated by the attribute name")
    @JvmStatic
    @JvmOverloads
    fun firstAttr(dom: ValueDom, cssQuery: String = ":root", attrName: String): String {
        return Queries.selectFirst(dom, cssQuery) { it.attr(attrName) } ?: ""
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query " +
            "and return the attribute value associated by the attribute name")
    @JvmStatic
    fun nthAttr(dom: ValueDom, cssQuery: String, n: Int, attrName: String): String {
        return Queries.selectNth(dom, cssQuery, n) { it.attr(attrName) } ?: ""
    }

    @UDFunction(description = "Select all the image elements from a DOM by the given css query " +
            "and return the src of the image element")
    @JvmStatic
    @JvmOverloads
    fun allImgs(dom: ValueDom, cssQuery: String = ":root"): ValueArray {
        val q = appendSelectorIfMissing(cssQuery, "img")
        return Queries.select(dom, q) { it.attr("abs:src") }
    }

    @UDFunction(description = "Select the first image element from a DOM by the given css query " +
            "and return the src of it")
    @JvmStatic
    @JvmOverloads
    fun firstImg(dom: ValueDom, cssQuery: String = ":root"): String {
        val q = appendSelectorIfMissing(cssQuery, "img")
        return Queries.selectFirst(dom, q) { it.attr("abs:src") } ?: ""
    }

    @UDFunction(description = "Select the nth image element from a DOM by the given css query " +
            "and return the src of it")
    @JvmStatic
    fun nthImg(dom: ValueDom, cssQuery: String, n: Int): String {
        val q = appendSelectorIfMissing(cssQuery, "img")
        return Queries.selectNth(dom, q, n) { it.attr("abs:src") } ?: ""
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun allHrefs(dom: ValueDom, cssQuery: String = ":root"): ValueArray {
        val q = appendSelectorIfMissing(cssQuery, "a")
        return Queries.select(dom, q) { ele -> ele.attr("abs:href") }
    }

    @UDFunction(description = "Select the first anchor element from a DOM by the given css query " +
            "and return the href of it")
    @JvmStatic
    @JvmOverloads
    fun firstHref(dom: ValueDom, cssQuery: String = ":root"): String {
        val q = appendSelectorIfMissing(cssQuery, "a")
        return Queries.selectFirst(dom, q) { it.attr("abs:href") } ?: ""
    }

    @UDFunction(description = "Select the nth anchor element from a DOM by the given css query " +
            "and return the href of it")
    @JvmStatic
    fun nthHref(dom: ValueDom, cssQuery: String, n: Int): String {
        val q = appendSelectorIfMissing(cssQuery, "a")
        return Queries.selectNth(dom, q, n) { it.attr("abs:href") } ?: ""
    }

    @UDFunction(description = "Select the first element from a DOM by the given css query " +
            "and return the label of it")
    @JvmStatic
    @JvmOverloads
    fun allNodesLabels(dom: ValueDom, cssQuery: String = ":root"): ValueArray {
        return allAttrs(dom, cssQuery, A_LABELS)
    }

    @UDFunction
    @JvmStatic
    @JvmOverloads
    fun firstNodeLabels(dom: ValueDom, cssQuery: String = ":root"): String {
        return firstAttr(dom, cssQuery, A_LABELS)
    }

    @UDFunction(description = "Select the nth element from a DOM by the given css query " +
            "and return the label of it")
    @JvmStatic
    fun nthNodeLabels(dom: ValueDom, cssQuery: String, n: Int): String {
        return nthAttr(dom, cssQuery, n, A_LABELS)
    }

    @UDFunction
    @JvmStatic
    fun allRe1(dom: ValueDom, regex: String): ValueArray {
        return allRe1(dom, ":root", regex)
    }

    @UDFunction
    @JvmStatic
    fun allRe1(dom: ValueDom, cssQuery: String, regex: String): ValueArray {
        val extractor = RegexExtractor()
        return Queries.select(dom, cssQuery) { extractor.re1(it.text(), regex) }
    }

    @UDFunction(description = "Select the first element from a DOM whose text matches the regex " +
            "and return the element text")
    @JvmStatic
    fun firstRe1(dom: ValueDom, regex: String): String {
        return firstRe1(dom, ":root", regex)
    }

    @UDFunction(description = "Select the first element from a DOM whose text matches the regex " +
            "and return the element text")
    @JvmStatic
    fun firstRe1(dom: ValueDom, cssQuery: String, regex: String): String {
        val text = text(selectFirst(dom, cssQuery))
        return RegexExtractor().re1(text, regex)
    }

    @UDFunction(description = "Select the first element from a DOM whose text matches the regex " +
            "and return the element text")
    @JvmStatic
    fun firstRe1(dom: ValueDom, cssQuery: String, regex: String, group: Int): String {
        val text = text(selectFirst(dom, cssQuery))
        return RegexExtractor().re1(text, regex, group)
    }

    @UDFunction
    @JvmStatic
    fun allRe2(dom: ValueDom, regex: String): ValueArray {
        return allRe2(dom, ":root", regex)
    }

    @UDFunction
    @JvmStatic
    fun allRe2(dom: ValueDom, cssQuery: String, regex: String): ValueArray {
        val extractor = RegexExtractor()
        return Queries.select(dom, cssQuery) { extractor.re2(it.text(), regex).toString() }
    }

    @UDFunction
    @JvmStatic
    fun firstRe2(dom: ValueDom, cssQuery: String, regex: String): ValueArray {
        val text = text(selectFirst(dom, cssQuery))
        val result = RegexExtractor().re2(text, regex)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun firstRe2(dom: ValueDom, cssQuery: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val text = text(selectFirst(dom, cssQuery))
        val result = RegexExtractor().re2(text, regex, keyGroup, valueGroup)
        val array = arrayOf(ValueString.get(result.key), ValueString.get(result.value))
        return ValueArray.get(array)
    }

    @UDFunction
    @JvmStatic
    fun allRe2(dom: ValueDom, cssQuery: String, regex: String, keyGroup: Int, valueGroup: Int): ValueArray {
        val extractor = RegexExtractor()
        return Queries.select(dom, cssQuery) { extractor.re2(it.text(), regex, keyGroup, valueGroup).toString() }
    }

    private fun text(dom: ValueDom): String {
        return dom.element.text()
    }

    private fun nthElement(root: Element, cssQuery: String, n: Int): Element? {
        val elements = root.select2(cssQuery)
        return if (elements.size > n) {
            elements[n]
        } else null
    }
}
