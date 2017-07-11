package org.jfrog.util.docker

import groovyx.net.http.HttpResponseException
import spock.lang.Shared
import spock.lang.Specification

/**
 *  Created by matank on 4/27/15.
 */
class VolumesSpec extends Specification {

    @Shared
    DockerClient dockerClient

    def setupSpec() {
        dockerClient = new DockerClient(System.getProperty("docker_url"))
    }

    def "Create volume"() {
        when:
        dockerClient.volume().name("test").doCreate()
        then:
        dockerClient.volume().name("test").inspect()
        when:
        dockerClient.volume().name("test").doDelete()
        then:
        try {
            dockerClient.volume().name("test").inspect()
        } catch (HttpResponseException hre) {
            true
        }
    }

    def "Create container with volume"() {
        setup:
        DockerVolume volume = dockerClient.volume().name("container-volume").doCreate()
        DockerImage image = dockerClient.image().repository("busybox").doCreate()
        DockerContainer container = image.getNewContainer("test-container")
        when:
        container.startConfig.addBinds("container-volume", "/tmp")
        container.doCreate().doStart()
        then:
        container.inspect().HostConfig.Binds[0].startsWith(volume.name)
        cleanup:
        container.doDelete(true)
        volume.doDelete()
    }
}