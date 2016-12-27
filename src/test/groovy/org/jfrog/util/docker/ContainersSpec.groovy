package org.jfrog.util.docker

import org.jfrog.util.docker.constants.ContainerStatus
import org.jfrog.util.docker.filters.ContainerFilter
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

/**
 * Created by matank on 4/28/15.
 */
@Stepwise
class ContainersSpec extends Specification {

    @Shared
    DockerClient dockerClient
    @Shared
    DockerImage dockerImage
    @Shared
    DockerContainer dockerContainer

    @Shared
    File output

    def setupSpec() {
        dockerClient = new DockerClient(System.getProperty("docker_url"))
        dockerImage = dockerClient.image().repository("busybox").doCreate()
    }

    def setup() {
        dockerContainer = dockerImage.getNewContainer("busybox-container")
    }

    def "Add command using container object" () {
        when:
        dockerContainer.addCommand("hostname")
        then:
        dockerContainer.createConfig.getCmd()[2] == "hostname;"
    }

    def "Get list of all containers"() {
        when:
        def containers = dockerClient.getContainersFromServer()
        then:
        containers.size() >= 0
    }

    def "Get all running containers"() {
        when:
        def containers = dockerClient.getContainersFromServer(true, 5, null, null, true, new ContainerFilter(null, ContainerStatus.EXITED))
        then:
        containers.size() >= 0
    }

    def "Create container"() {
        setup:
        dockerContainer.createConfig
                .hostname("test-container")
                .addCommand(["/bin/sh", "-c", "echo 'test has passed' > /tmp/test.txt ; ping 8.8.8.8"])
                .addEnv("testEnv", "123")
                .addExposedPort(8081, "tcp")
        dockerContainer.startConfig.publishAllPorts()

        when:
        dockerContainer.doCreate()
        then:
        dockerContainer.inspect()
    }

    def "Start container"() {
        when:
        dockerContainer.doStart(5)
        then:
        dockerContainer.inspect().State.Running == true
    }

    def "Get External Port"() {
        when:
        int externalPort = dockerContainer.getExternalPort(8081, "tcp")
        then:
        externalPort != -1
    }

    def "Check if container exists"() {
        when:
        def exists = dockerContainer.isExists()
        then:
        exists == true
    }

    def "Check if container is running"() {
        when:
        boolean isRunning = dockerContainer.state.isRunning()
        then:
        isRunning == true
    }

    def "Get Container stats"() {
        when:
        Map stats = dockerContainer.stats()
        then:
        stats.containsKey("memory_stats")
//        stats.containsKey("network")
        stats.containsKey("cpu_stats")
    }

    def "Get Container Exit Code When Running"() {
        when:
        int exitCode = dockerContainer.exitCode()
        then:
        exitCode == -1
    }

    def "Get Container variables"() {
        when:
        Map variables = dockerContainer.getEnvs()
        then:
        variables.testEnv == "123"
    }

    def "Stop container"() {
        when:
        dockerContainer.doStop()
        then:
        dockerContainer.inspect().State.Running == false
    }

    def "Check if container is not running"() {
        when:
        boolean isRunning = dockerContainer.state.isRunning()
        then:
        isRunning == false
    }

    def "Get Container Exit Code When Stopped"() {
        when:
        int exitCode = dockerContainer.exitCode()
        then:
        exitCode != -1
    }


    def "Collect logs"() {
        when:
        String logs = dockerContainer.logs()
        then:
        logs.contains("PING 8.8.8.8 (8.8.8.8): 56 data bytes")
    }

    def "Download file from container"() {
        when:
        output = dockerContainer.downloadFile("/tmp/test.txt")
        then:
        output.text == "test has passed\n"
        cleanup:
        if (output != null) output.delete()
    }

    def "Download file from container into custom location"() {
        setup:
        File tempFolder = new File(this.getClass().getResource("").path+File.separator+"tmp")
        tempFolder.mkdir()
        when:
        output = dockerContainer.downloadFile("/tmp/test.txt", this.getClass().getResource("").path+"/tmp")
        then:
        output.text == "test has passed\n"
        cleanup:
        if (output != null) output.delete()
    }

    def cleanupSpec() {
        if (dockerContainer.isExists()) {
            dockerContainer.doDelete(true, true)
        }
        if (dockerImage.isExists()) {
            dockerImage.doDelete(true)
        }
    }
}