package org.jfrog.util.docker

import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by matank on 5/31/15.
 */
class ExecSpec extends Specification {

    @Shared
    DockerClient dockerClient

    @Shared
    DockerImage dockerImage

    @Shared
    DockerContainer dockerContainer

    @Shared
    DockerExec dockerExec

    def setupSpec() {
        dockerClient = new DockerClient(System.getProperty("docker_url"))
    }

    def "Exec command in running container" () {
        when:
        dockerImage = dockerClient.getImage("busybox")
        dockerImage.doCreate()
        then:
        dockerImage.inspect()

        when:
        dockerContainer = dockerImage.getNewContainer("busybox-exec")
        dockerContainer.createConfig.addCommand(["ping", "8.8.8.8"]).hostname("busybox-exec")
        dockerContainer.doCreate()
        then:
        dockerContainer.inspect()

        and:
        dockerContainer.doStart()

        when:
        dockerExec = dockerContainer.exec("env").doCreate()
        then:
        dockerExec.doStart().contains("HOSTNAME=busybox-exec")
    }

    def cleanupSpec() {
        dockerContainer.doDelete(true, true)
        dockerImage.doDelete(true)
    }
}