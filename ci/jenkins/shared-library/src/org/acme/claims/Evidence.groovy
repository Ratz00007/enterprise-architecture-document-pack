// src/org/acme/claims/Evidence.groovy
// Builds the per-stage evidence bundle and writes it to the
// audit log.

package org.acme.claims

class Evidence implements Serializable {
    private static final long serialVersionUID = 1L

    def script
    Map config
    String env
    String digest

    Evidence(script, Map config, String env, String digest) {
        this.script = script
        this.config = config
        this.env = env
        this.digest = digest
    }

    void write() {
        def bundle = [
            artifact: [
                name   : config.containerImage,
                digest : digest,
                build  : script.env.BUILD_NUMBER,
            ],
            env: env,
            promotedAt: script.sh(script: "date -u +%FT%TZ", returnStdout: true).trim(),
            approver: script.currentBuild.upstreamCauses?.toString() ?: 'unknown',
            git: [
                commit : script.sh(script: "git rev-parse HEAD", returnStdout: true).trim(),
                branch : script.env.BRANCH_NAME,
            ],
        ]
        def json = script.writeJSON(returnText: true, json: bundle)
        script.writeFile file: "evidence-${env}-${script.env.BUILD_NUMBER}.json", text: json
        script.archiveArtifacts artifacts: "evidence-${env}-${script.env.BUILD_NUMBER}.json"
        // TODO post-MVP: write to the audit-log service
    }
}
