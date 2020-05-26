package org.jfrog.util.docker

import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by matank on 5/18/15.
 */
class ClientSpec extends Specification {

    @Shared
    DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClient(System.getProperty("docker_url"))
    }

    def "Get all networks"() {
        when:
        def networks = dockerClient.getAllNetworks()

        then:
        def networkNames = []
        for(DockerNetwork network : networks){
            networkNames.add(network.name)
        }
        networkNames.contains("host")
        networkNames.contains("bridge")
    }
}