package org.jfrog.util.docker

import org.jfrog.util.docker.configurations.DockerFileBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
*  Created by matank on 4/27/15.
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
        dockerImages.size() >= 0
    }

    def "Get list of all images"() {
        when:
        def dockerImages = dockerClient.getImagesFromServer(true)

        then:
        dockerImages.size() >= 0
    }

    def "Create new image from public registry"() {
        when:
        DockerImage dockerImage = dockerClient.image().repository("busybox").tag("latest")

        then:
        ! dockerImage.isExists()
        dockerImage.doCreate()
        dockerImage.isExists()

        and:
        dockerImage.doDelete()
    }

    def "Create new image from private registry"() {
        when:
        DockerImage dockerImage = dockerClient.image()
                .registry(new DockerRegistry(dockerPrivateRegistry, dockerPrivateRegistryUsername, dockerPrivateRegistryPassword))
                .repository("busybox")
                .tag("latest")

        then:
        dockerImage.doCreate()
    }

    def "Get history of image"() {
        when:
        DockerImage dockerImage = dockerClient.image()
                .registry(dockerPrivateRegistry)
                .repository("busybox")

        then:
        dockerImage.history().size() > 0

        cleanup:
        dockerImage.doDelete()
    }

    def "Tag new image"() {
        when:
        DockerImage dockerImage = dockerClient.image()
                .repository("busybox")
                .tag("ubuntu-14.04")

        DockerImage targetImage = dockerClient.image()
                .registry(dockerPrivateRegistry)
                .namespace("docker-remote-util-test")
                .repository("busybox")

        then:
        dockerImage.doCreate()
        dockerImage.doTag(targetImage)

        cleanup:
        dockerImage.doDelete()
    }

    def "Push new image to private registry"() {
        when:
        DockerRegistry dockerRegistry = new DockerRegistry(dockerPrivateRegistry, dockerPrivateRegistryUsername, dockerPrivateRegistryPassword)
        DockerImage dockerImage = dockerClient.image()
                .registry(dockerRegistry)
                .namespace("docker-remote-util-test")
                .repository("busybox")

        then:
        dockerImage.doPush()
    }

    def "Search image"() {
        when:
        DockerImage dockerImage = dockerClient.image()
                .repository("busybox")

        and:
        List searchResults = dockerImage.doSearch()

        then:
        searchResults.size() > 0
        searchResults[0].name == "${dockerImage.image}"
    }

    def "Delete image"() {
        when:
        DockerImage dockerImage = dockerClient.image()
                .registry(dockerPrivateRegistry)
                .namespace("docker-remote-util-test")
                .repository("busybox")

        then:

        dockerImage.doDelete()
    }

    def "Build Docker Image"() {
        setup:
        DockerImage dockerImage = dockerClient.image()
                .namespace("artifactory")
                .repository("artifactory-rpm")
                .tag("3.7.0")
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

    def "Build Docker Image without cache"() {
        setup:
        DockerImage dockerImage = dockerClient.image()
                .namespace("artifactory")
                .repository("artifactory-rpm")
                .tag("3.7.0")
        DockerFileBuilder dfb = new DockerFileBuilder(new File(this.getClass().getResource("").path))

        when:
        dfb.from("centos", "6.6").
                run("yum install -y wget && yum install -y rsync").
                copy(this.getClass().getResource("b.txt").path, "/var/opt/jfrog/artifactory/etc/artifactory.system.properties").
                copy(this.getClass().getResource("a/a.txt").path, "/tmp/").
                cmd("service artifactory wait").
                create()

        then:
        dockerClient.build(dfb, dockerImage, true, true) != null

        and:
        println dockerImage.inspect()

        cleanup:
        dfb.close()

    }


}