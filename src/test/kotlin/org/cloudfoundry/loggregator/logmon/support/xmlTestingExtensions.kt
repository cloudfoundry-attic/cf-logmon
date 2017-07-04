package org.cloudfoundry.loggregator.logmon.support

import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


fun String.getHtml(): Document {
    val domFactory = DocumentBuilderFactory.newInstance()
    domFactory.isNamespaceAware = true
    val builder = domFactory.newDocumentBuilder()
    return builder.parse(this.byteInputStream())
}

fun Document.xpath(path: String): NodeList {
    val xpath = XPathFactory.newInstance().newXPath()
    val expr = xpath.compile(path)

    val result = expr.evaluate(this, XPathConstants.NODESET)
    return result as NodeList
}
