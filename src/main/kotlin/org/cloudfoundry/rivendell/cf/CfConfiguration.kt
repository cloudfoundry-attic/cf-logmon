package org.cloudfoundry.rivendell.cf

import org.cloudfoundry.client.CloudFoundryClient
import org.cloudfoundry.doppler.DopplerClient
import org.cloudfoundry.reactor.ConnectionContext
import org.cloudfoundry.reactor.DefaultConnectionContext
import org.cloudfoundry.reactor.TokenProvider
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider
import org.cloudfoundry.reactor.uaa.ReactorUaaClient
import org.cloudfoundry.uaa.UaaClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class CfConfiguration {

    @Configuration
    @ConfigurationProperties("cf")
    open class CfLoginInfo {
        lateinit var username: String
        lateinit var password: String
    }

    @Bean
    open fun cfApiHost(cfApplicationEnv: CfApplicationEnv): String = cfApplicationEnv.cfApi.host

    @Bean
    open fun cloudFoundryVcapEnvironmentPostProcessor() = CloudFoundryVcapEnvironmentPostProcessor()

    @Bean
    open fun uaaClient(connectionContext: ConnectionContext, tokenProvider: TokenProvider): UaaClient =
            ReactorUaaClient.builder()
                    .connectionContext(connectionContext)
                    .tokenProvider(tokenProvider)
                    .build()

    @Bean
    open fun connectionContext(cfApiHost: String): DefaultConnectionContext {
        return DefaultConnectionContext.builder()
                .apiHost(cfApiHost)
                .build()
    }

    @Bean
    open fun tokenProvider(cfLoginInfo: CfLoginInfo): TokenProvider {
        return PasswordGrantTokenProvider.builder()
                .username(cfLoginInfo.username)
                .password(cfLoginInfo.password)
                .build()
    }

    @Bean
    open fun cloudFoundryClient(connectionContext: ConnectionContext, tokenProvider: TokenProvider): CloudFoundryClient {
        return ReactorCloudFoundryClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build()
    }

    @Bean
    open fun dopplerClient(connectionContext: ConnectionContext, tokenProvider: TokenProvider): DopplerClient {
        return ReactorDopplerClient.builder()
                .connectionContext(connectionContext)
                .tokenProvider(tokenProvider)
                .build()
    }
}
