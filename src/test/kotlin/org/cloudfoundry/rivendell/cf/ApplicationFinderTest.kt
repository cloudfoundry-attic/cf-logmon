package org.cloudfoundry.rivendell.cf

import org.assertj.core.api.Assertions.assertThat
import org.cloudfoundry.client.CloudFoundryClient
import org.cloudfoundry.client.v2.Metadata
import org.cloudfoundry.client.v2.applications.*
import org.cloudfoundry.client.v2.domains.Domain
import org.cloudfoundry.client.v2.organizations.*
import org.cloudfoundry.client.v2.routes.Route
import org.cloudfoundry.client.v2.spaces.*
import org.cloudfoundry.client.v2.stacks.GetStackResponse
import org.cloudfoundry.client.v2.stacks.StackEntity
import org.cloudfoundry.doppler.*
import org.cloudfoundry.uaa.UaaClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentMatcher
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Instant

@SpringBootTest
@RunWith(MockitoJUnitRunner::class)
class ApplicationFinderTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS, name = "Cloud Foundry Client")
    private lateinit var cloudFoundryClient: CloudFoundryClient

    @Mock
    private lateinit var dopplerClient: DopplerClient

    @Mock
    private lateinit var uaaClient: UaaClient

    val cfApplicationEnv = CfApplicationEnv().also {
        it.cfApi = URI("https://foo.bar")
        it.name = "rivendell"
        it.spaceId = "some space id"
    }

    val subject by lazy { ApplicationFinder(cloudFoundryClient, dopplerClient, uaaClient, cfApplicationEnv) }
    @Before
    fun setUp() {
        `when`(cloudFoundryClient.organizations().list(any()))
                .thenReturn(Mono.just(organizationList("org-id1")))
        `when`(cloudFoundryClient.organizations().get(any()))
                .thenReturn(Mono.just(GetOrganizationResponse.builder()
                        .entity(OrganizationEntity.builder().name("org name").build())
                        .metadata(Metadata.builder().id("org-id1").build())
                        .build())
                )
        `when`(cloudFoundryClient.spaces().get(any())).thenReturn(Mono.just(spaceNamed("some space id")))
        `when`(cloudFoundryClient.spaces().list(any())).thenReturn(Mono.just(ListSpacesResponse.builder()
                .resource(SpaceResource.builder()
                        .entity(SpaceEntity.builder().name("space name").organizationId("org-id1").build())
                        .metadata(Metadata.builder().id("some space id").build())
                        .build())
                .build()))

        `when`(cloudFoundryClient.spaces().listApplications(any())).thenReturn(Mono.just(
                ListSpaceApplicationsResponse.builder().resources(listOf(
                        ApplicationResource.builder()
                                .metadata(Metadata.builder().id("some app guid").build())
                                .entity(ApplicationEntity.builder().stackId("39428934").build())
                                .build()
                )).build()
        ))

        `when`(cloudFoundryClient.applicationsV2().statistics(any())).thenReturn(Mono.just(
                ApplicationStatisticsResponse.builder().build()
        ))

        `when`(cloudFoundryClient.applicationsV2().summary(any())).thenReturn(Mono.just(
                SummaryApplicationResponse.builder()
                        .id("some app guid")
                        .diskQuota(39482834)
                        .instances(9)
                        .memory(3923)
                        .name("some app name")
                        .state("running")
                        .runningInstances(39)
                        .stackId("939")
                        .routes(listOf(
                                Route.builder()
                                        .domain(Domain.builder().name("localhost:8080").build())
                                        .host("pez.pivotal.io")
                                        .build()
                        )
                        ).build()
        ))
        `when`(cloudFoundryClient.applicationsV2().instances(any())).thenReturn(Mono.just(
                ApplicationInstancesResponse.builder().instances(mapOf()).build()
        ))
        `when`(cloudFoundryClient.stacks().get(any())).thenReturn(Mono.just(
                GetStackResponse.builder().entity(StackEntity.builder().name("foo").build()).build()
        ))
    }

    @Test
    fun fetchSpaceById_canFetchSpaceById() {
        `when`(cloudFoundryClient.organizations().list(any())).thenReturn(Mono.just(
                organizationList("org-id1", "org-id2")
        ))
        `when`(cloudFoundryClient.spaces().get(any())).thenReturn(Mono.just(spaceNamed("some space id")))

        val space = subject.fetchSpaceById("some space id")

        assertThat(space.metadata.id).isEqualTo("some space id")
    }

    @Test
    fun fetchApplicationByName_canGetAnApplicationInASpaceByName() {
        val application = subject.fetchApplicationByName("some name")!!

        assertThat(application.id).isEqualTo("some app guid")
    }

    @Test
    fun logStreamForApplication_providesSubscriptionOfLogMessages() {
        `when`(dopplerClient.stream(any()))
                .thenReturn(Flux.range(0, 100).map { i ->
                    Envelope.builder()
                            .origin("somewhere like home")
                            .eventType(EventType.LOG_MESSAGE)
                            .logMessage(LogMessage.builder()
                                    .messageType(MessageType.OUT)
                                    .timestamp(Instant.now().toEpochMilli())
                                    .message("message: $i")
                                    .build())
                            .build()
                })
        val stream = subject.logStreamForApplication("app")

        val messages = stream.collectList().block()
        assertThat(messages).hasSize(100)
    }

    private fun spaceNamed(spaceId: String) = GetSpaceResponse.builder()
            .entity(SpaceEntity.builder().name(spaceId).organizationId("some org id").build())
            .metadata(Metadata.builder().id(spaceId).build())
            .build()

    private fun organizationList(vararg ids: String): ListOrganizationsResponse? {
        return ListOrganizationsResponse.builder().resources(
                ids.map { OrganizationResource.builder().metadata(Metadata.builder().id(it).build()).build() }
        ).build()
    }

    private class CfRequestMatcher(val func: (arg: ListOrganizationsRequest) -> Boolean) : ArgumentMatcher<ListOrganizationsRequest>() {
        override fun matches(argument: Any?): Boolean {
            if (argument == null) return false
            return func(argument as ListOrganizationsRequest)
        }
    }
}

