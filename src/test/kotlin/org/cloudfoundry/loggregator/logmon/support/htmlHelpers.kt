package org.cloudfoundry.loggregator.logmon.support

import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.net.URI

fun page(http: TestRestTemplate, baseUrl: String, path: String = "/"): Document {
    val request = RequestEntity.get(URI(baseUrl + path))
        .header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=")
        .accept(MediaType.TEXT_HTML)
        .build()
    return http.exchange(request, String::class.java).body.getHtml()
}

val Document.text: String
    get() = xpath("//body").item(0).text

val Node.text: String
    get() = textContent.trim().replace(Regex("\\s+"), " ")
