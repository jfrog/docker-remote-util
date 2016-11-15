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
        dockerImage = dockerClient.image().repository("busybox").doCreate()
        then:
        dockerImage.inspect()

        when:
        dockerContainer = dockerImage.getNewContainer("busybox-exec-keep-alive")
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

        cleanup:
        dockerContainer.doDelete(true, true)
    }

    def "Exec command in running container with Connection close header" () {
        when:
        dockerImage = dockerClient.image().repository("busybox").doCreate()
        then:
        dockerImage.inspect()

        when:
        dockerContainer = dockerImage.getNewContainer("busybox-exec-connection-close")
        dockerContainer.createConfig.addCommand(["ping", "8.8.8.8"]).hostname("busybox-exec")
        dockerContainer.doCreate()
        then:
        dockerContainer.inspect()

        when:
        dockerContainer.dockerClient.setConnectionClose()
        dockerContainer.doStart()
        dockerContainer.dockerClient.setKeepAlive()
        dockerExec = dockerContainer.exec("env").doCreate()

        then:
        dockerExec.doStart().contains("HOSTNAME=busybox-exec")

        cleanup:
        dockerContainer.doDelete(true, true)
    }

    def "Exec command in running container with user" () {
        when:
        dockerImage = dockerClient.image().repository("busybox").doCreate()
        then:
        dockerImage.inspect()

        when:
        dockerContainer = dockerImage.getNewContainer("busybox-exec-user-www-data")
        dockerContainer.createConfig.addCommand(["ping", "8.8.8.8"]).hostname("busybox-exec")
        dockerContainer.doCreate()
        then:
        dockerContainer.inspect()

        when:
        dockerContainer.doStart()
        dockerContainer.exec("touch /var/www/a.txt").asUser("www-data").doCreate().doStart()

        then:
        dockerContainer.exec("ls -al /var/www/a.txt").asUser("www-data").doCreate().doStart().contains("www-data")

        cleanup:
        dockerContainer.doDelete(true, true)
    }

    def cleanupSpec() {
        dockerImage.doDelete(true)
    }
}