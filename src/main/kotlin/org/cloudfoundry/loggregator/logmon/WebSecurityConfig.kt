package org.cloudfoundry.loggregator.logmon

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


@Configuration
@EnableWebSecurity
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests().anyRequest()
            .fullyAuthenticated()
            .and()
            .httpBasic()
            .and()
            .csrf().disable()
    }

    @Value("\${logmon.auth.username}")
    private lateinit var basicAuthUsername: String
    @Value("\${logmon.auth.password}")
    private lateinit var basicAuthPassword: String

    @Autowired
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth
            .inMemoryAuthentication()
            .withUser(basicAuthUsername).password(basicAuthPassword).roles("USER")
    }
}
