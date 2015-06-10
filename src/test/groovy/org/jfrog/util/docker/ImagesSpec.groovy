package org.jfrog.util.docker

import org.jfrog.util.docker.configurations.DockerFileBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by matank on 4/27/15.
 */
class ImagesSpec extends Specification {

    @Shared
    DockerClient dockerClient
    @Shared
    String dockerPrivateRegistry
    @Shared
    String dockerPrivateRegistryUsername
    @Shared
    String dockerPrivateRegistryPassword

    def setupSpec() {
        dockerClient = new DockerClient(System.getProperty("docker_url"))
        dockerPrivateRegistry = System.getProperty("docker_private_registry")
        dockerPrivateRegistryUsername = System.getProperty("docker_private_registry_username")
        dockerPrivateRegistryPassword = System.getProperty("docker_private_registry_password")
    }

    def "Get list of images created by current process"() {
        when:
        def dockerImages = dockerClient.getImagesFromServer()

        then:
        dockerImages.size() > 0
    }

    def "Get list of all images"() {
        when:
        def dockerImages = dockerClient.getImagesFromServer(true)

        then:
        dockerImages.size() > 0
    }

    def "Create new image from public registry"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox").withTag("ubuntu-14.04")

        then:
        dockerImage.doCreate()

        and:
        dockerImage.doDelete()
    }

    def "Create new image from private registry"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry)
                .fromRepo("templates")
                .withTag("latest")

        then:
        dockerImage.doCreate()
    }

    def "Get history of image"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry)
                .fromRepo("templates")
                .withTag("latest")

        then:
        dockerImage.history().size() > 0

        cleanup:
        dockerImage.doDelete()
    }

    def "Tag new image"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .withTag("ubuntu-14.04")

        DockerImage targetImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry)
                .fromRepo("dockerclient")

        then:
        dockerImage.doCreate()
        dockerImage.doTag(targetImage)

        cleanup:
        dockerImage.doDelete()
    }

    def "Push new image to private registry"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry, dockerPrivateRegistryUsername, dockerPrivateRegistryPassword)
                .fromRepo("dockerclient")

        then:
        dockerImage.doPush()
    }

    def "Search image"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry)
                .fromRepo("dockerclient")

        and:
        List searchResults = dockerImage.doSearch()

        then:
        searchResults.size() == 1
        searchResults[0].name == "${dockerImage.repo}/${dockerImage.image}"
    }

    def "Delete image"() {
        when:
        DockerImage dockerImage = dockerClient.getImage("busybox")
                .fromRegistry(dockerPrivateRegistry)
                .fromRepo("dockerclient")
                .withTag("latest")

        then:

        dockerImage.doDelete()
    }

    def "Build Docker Image"() {
        setup:
        DockerImage dockerImage = dockerClient.getImage("artifactory-rpm").withTag("3.7.0").fromRepo("artifactory")
        DockerFileBuilder dfb = new DockerFileBuilder(new File(this.getClass().getResource("").path))

        when:
        dfb.from("centos", "6.6").
                run("yum install -y wget && yum install -y rsync").
                copy(this.getClass().getResource("b.txt").path, "/var/opt/jfrog/artifactory/etc/artifactory.system.properties").
                copy(this.getClass().getResource("a/a.txt").path, "/tmp/").
                cmd("service artifactory wait").
                create()

        then:
        dockerClient.build(dfb, dockerImage) != null

        and:
        println dockerImage.inspect()

        cleanup:
        dfb.close()

    }

}