package org.jfrog.util.artifactory

import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import spock.lang.Requires
import spock.lang.Specification

//@Requires({
//    System.getProperty("artifactory_contextUrl") &&
//            System.getProperty("artifactory_user") &&
//            System.getProperty("artifactory_password") &&
//            System.getProperty("docker_repository") &&
//            System.getProperty("docker_tag")
//})
class ArtifactoryUtilSpec extends Specification {

    def "Get latest tag without tag layout"() {
        setup:
        Artifactory artifactory = ArtifactoryClientBuilder
                .create()
                .setUrl(System.getenv("artifactory_contextUrl"))
                .setUsername(System.getenv("artifactory_user"))
                .setPassword(System.getenv("artifactory_password"))
                .build()
        when:
        String latestTag = ArtifactoryUtil.getLatestTag(artifactory, System.getenv("docker_repository"), "jfrog/artifactory-*")
        then:
        latestTag
    }

    def "Get latest tag with tag layout"() {
        setup:
        Artifactory artifactory = ArtifactoryClientBuilder
                .create()
                .setUrl(System.getenv("artifactory_contextUrl"))
                .setUsername(System.getenv("artifactory_user"))
                .setPassword(System.getenv("artifactory_password"))
                .build()
        when:
        String latestTag = ArtifactoryUtil.getLatestTag(artifactory, System.getenv("docker_repository"), "jfrog/artifactory-*", "*"+System.getenv("docker_tag") + "*")
        then:
        latestTag.contains(System.getenv("docker_tag"))
    }
}
