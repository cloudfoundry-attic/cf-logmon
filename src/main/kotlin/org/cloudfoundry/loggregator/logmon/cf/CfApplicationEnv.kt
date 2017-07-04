package org.cloudfoundry.loggregator.logmon.cf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI

@Component
@ConfigurationProperties("vcap.application")
open class CfApplicationEnv {
    lateinit var name: String
    lateinit var cfApi: URI
    lateinit var spaceId: String
}
