package org.cloudfoundry.rivendell.cf

import org.cloudfoundry.client.CloudFoundryClient
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest
import org.cloudfoundry.client.v2.organizations.GetOrganizationResponse
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest
import org.cloudfoundry.client.v2.spaces.GetSpaceResponse
import org.cloudfoundry.doppler.DopplerClient
import org.cloudfoundry.doppler.LogMessage
import org.cloudfoundry.operations.DefaultCloudFoundryOperations
import org.cloudfoundry.operations.applications.ApplicationDetail
import org.cloudfoundry.operations.applications.GetApplicationRequest
import org.cloudfoundry.operations.applications.LogsRequest
import org.cloudfoundry.uaa.UaaClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import javax.annotation.PostConstruct

@Component
open class ApplicationFinder @Autowired constructor(
        val cloudFoundryClient: CloudFoundryClient,
        val dopplerClient: DopplerClient,
        val uaaClient: UaaClient,
        val cfApplicationEnv: CfApplicationEnv
) {

    @PostConstruct
    open fun onInit() {
        logStreamForApplication(cfApplicationEnv.name).subscribe {
            println("You've got mail: $it")
        }
    }

    fun logStreamForApplication(appName: String): Flux<LogMessage> {
        val space = fetchSpaceById(cfApplicationEnv.spaceId)
        val organization = fetchOrganizationById(space.entity.organizationId)
        val application = fetchApplicationByName(appName)!!
        val logsReq = LogsRequest.builder().name(application.name).build()

        return client(organization.entity.name, space.entity.name).applications().logs(logsReq)
    }

    fun fetchApplicationByName(name: String): ApplicationDetail? {
        val space = fetchSpaceById(cfApplicationEnv.spaceId)
        val organization = fetchOrganizationById(space.entity.organizationId)

        return client(organization.entity.name, space.entity.name).applications()
                .get(GetApplicationRequest.builder().name(name).build())
                .block()
    }

    fun fetchSpaceById(spaceId: String): GetSpaceResponse =
            cloudFoundryClient.spaces()
                    .get(GetSpaceRequest.builder().spaceId(spaceId).build())
                    .block()

    private fun fetchOrganizationById(orgId: String): GetOrganizationResponse =
            cloudFoundryClient.organizations()
                    .get(GetOrganizationRequest.builder().organizationId(orgId).build())
                    .block()

    private var _client: DefaultCloudFoundryOperations? = null

    private fun client(orgId: String, spaceId: String): DefaultCloudFoundryOperations {
        if (_client == null) {
            _client = DefaultCloudFoundryOperations.builder()
                    .cloudFoundryClient(cloudFoundryClient)
                    .dopplerClient(dopplerClient)
                    .uaaClient(uaaClient)
                    .organization(orgId)
                    .space(spaceId)
                    .build()
        }
        return _client!!
    }
}
