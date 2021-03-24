package org.jfrog.util.artifactory

import groovy.json.JsonOutput
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.ArtifactoryRequest
import org.jfrog.artifactory.client.ArtifactoryResponse
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl
import org.jfrog.util.docker.exceptions.TagNotFoundException

class ArtifactoryUtil {

    static String getLatestTag(String repository, String image, String tag = null) {
        return this.getLatestTag(
                ArtifactoryClientBuilder.create()
                        .setUrl(getArtifactoryContextUrl())
                        .setUsername(getArtifactoryUser())
                        .setPassword(getArtifactoryPassword())
                        .build(),
                repository,
                image,
                tag
        )
    }

    /**
     * Get latest pushed tag for a specific repository using AQL.
     *
     * @param artifactory - artifactory to search in.
     * @param repository - repository to search in.
     * @param image - image to get latest tag for.
     * @param tag - if null, it would return the latest tag for the entire docker repository.<br>
     *     If not null, will return the latest tag pushed matches to the layout inserted.<br>
     *         Wildcard is supported.
     *         example: 17.*
     * @return the latest tag based on the inserted parameters.
     */
    static String getLatestTag(Artifactory artifactory, String repository, String image, String tag = null) {
        Map json = [
                "repo": repository,
                "type": "folder",
                "path": ["\$match": "$image"]
        ]

        if (tag) {
            json.put("name", ["\$match": "$tag"])
        }

        String aqlQuery = "items.find(${JsonOutput.toJson(json)}).sort({\"\$desc\" : [\"created\"]}).limit(1)"

        ArtifactoryRequest aqlRequest = new ArtifactoryRequestImpl()
                .method(ArtifactoryRequest.Method.POST)
                .apiUrl("api/search/aql")
                .requestType(ArtifactoryRequest.ContentType.TEXT)
                .requestBody(aqlQuery)
                .responseType(ArtifactoryRequest.ContentType.JSON)

        ArtifactoryResponse artifactoryResponse = artifactory.restCall(aqlRequest)

        assertOnFailure(artifactoryResponse)


        def result = Optional.ofNullable(artifactoryResponse)
                .map({response -> response.parseBody(Map.class).results[0]})
                .orElseThrow({-> new TagNotFoundException("Docker tag was not found")})
        return result.name
    }

    static String getArtifactoryContextUrl() {
        return getProp("artifactory_contextUrl")
    }

    static String getArtifactoryUser() {
        return getProp("artifactory_user")
    }

    static String getArtifactoryPassword() {
        return getProp("artifactory_password")
    }

    static String getProp(String property) {
        return System.getProperty(property) ?: System.getenv(property)
    }

    static private void assertOnFailure(ArtifactoryResponse artifactoryResponse) {
        if (! artifactoryResponse.isSuccessResponse()) {
            throw new RuntimeException("Artifactory responded with: '${artifactoryResponse.getStatusLine()}'")
        }
    }

}
