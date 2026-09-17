// src/org/acme/claims/GenaiAdvisory.groovy
// Asks the central GenAI gateway for an advisory summary. Never
// acts on the response — humans do.

package org.acme.claims

class GenaiAdvisory implements Serializable {
    private static final long serialVersionUID = 1L

    def script
    Map config
    String env
    String digest

    GenaiAdvisory(script, Map config, String env, String digest) {
        this.script = script
        this.config = config
        this.env = env
        this.digest = digest
    }

    void summarize() {
        // The gateway enforces the data-class rules from ADR-011.
        // We only ever pass metadata, never raw logs or build
        // output. The full evidence bundle goes to the audit log,
        // not to the gateway.
        def body = [
            env: env,
            artifact: [name: config.containerImage, digest: digest],
            build: script.env.BUILD_NUMBER,
            testSummary: [
                // numbers only, no source code
                unitPass: script.sh(script: "cat target/surefire-reports/*.txt 2>/dev/null | grep -c 'Tests run' || echo 0", returnStdout: true).trim(),
            ],
        ]
        def resp = script.httpRequest(
            url: "https://genai-01:8443/advisory/release-summary",
            httpMode: 'POST',
            contentType: 'APPLICATION_JSON',
            requestBody: script.writeJSON(returnText: true, json: body),
            authentication: 'genai-01-client',
        )
        // Write the response (an advisory) to the audit log. Never
        // act on it.
        script.echo "GenAI advisory: ${resp.content}"
    }
}
