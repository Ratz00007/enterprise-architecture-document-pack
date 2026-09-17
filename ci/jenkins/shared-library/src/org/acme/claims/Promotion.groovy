// src/org/acme/claims/Promotion.groovy
// Skeleton — full implementation lives in the build-out workstream.

package org.acme.claims

class Promotion implements Serializable {
    private static final long serialVersionUID = 1L

    def script
    Map config
    String env

    Promotion(script, Map config, String env) {
        this.script = script
        this.config = config
        this.env = env
    }

    void run() {
        // 1. Resolve the artifact digest for this build
        def digest = script.sh(
            script: "skopeo inspect --format '{{.Digest}}' docker://${config.containerImage}:${script.env.BUILD_NUMBER}",
            returnStdout: true
        ).trim()

        // 2. Verify the cosign signature
        script.sh "cosign verify --key cosign.pub ${config.containerImage}@${digest}"

        // 3. Render the per-env inventory + run the promote playbook
        def inv = "inventories/${env}.yml"
        script.sh "ansible-playbook -i ${inv} playbooks/promote.yml -e app_version=${script.env.BUILD_NUMBER} -e app_digest=${digest}"

        // 4. Post-deploy smoke + health check
        script.sh "scripts/smoke.sh ${env}"

        // 5. Write to the audit log
        new org.acme.claims.Evidence(script: script, config: config, env: env, digest: digest).write()

        // 6. (optional) Ask the GenAI gateway for an advisory release summary
        if (config.genaiAdvisory) {
            new org.acme.claims.GenaiAdvisory(script: script, config: config, env: env, digest: digest).summarize()
        }
    }
}
