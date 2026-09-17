// src/org/acme/claims/AcmePipeline.groovy
// Skeleton — full implementation lives in the build-out workstream
// (multi-agent-plan.md workstream #7).

package org.acme.claims

class AcmePipeline implements Serializable {
    private static final long serialVersionUID = 1L

    def script
    Map config

    AcmePipeline(script, Map config) {
        this.script = script
        this.config = config
    }

    void run() {
        script.stage('Checkout') { checkout scm }

        script.stage('Build & Unit Test') {
            script.sh "./mvnw -B -q clean verify"
        }

        script.stage('Static Analysis') {
            script.sh "./mvnw -B -q spotbugs:check checkstyle:check pmd:check"
        }

        script.stage('Dependency CVE Scan') {
            script.sh "./mvnw -B -q org.owasp:dependency-check-maven:check"
        }

        script.stage('Container Build & Sign') {
            script.sh "buildah build -t ${config.containerImage}:${script.env.BUILD_NUMBER} ."
            script.sh "skopeo copy --dest-creds \\$REGISTRY_USER:\\$REGISTRY_PASS containers-storage:${config.containerImage}:${script.env.BUILD_NUMBER} docker://${config.containerImage}:${script.env.BUILD_NUMBER}"
            script.sh "cosign sign --yes ${config.containerImage}:${script.env.BUILD_NUMBER}"
        }

        script.stage('Integration Tests (Testcontainers)') {
            script.sh "./mvnw -B -q failsafe:integration-test failsafe:verify -Pintegration"
        }

        script.stage('Contract Tests (Pact provider)') {
            script.sh "./mvnw -B -q pact:verify"
        }

        // Promotion chain
        def chain = config.promotionChain ?: []
        for (env in chain) {
            script.stage("Promote to ${env}") {
                def role = config.approvalRoles[env]
                script.input(
                    message: "Promote ${config.name} build ${script.env.BUILD_NUMBER} to ${env}?",
                    submitter: role,
                    ok: 'Approve',
                    parameters: [
                        script.text(name: 'COMMENT', defaultValue: '', description: 'Approval comment')
                    ]
                )
                new org.acme.claims.Promotion(script: script, config: config, env: env).run()
            }
        }
    }
}
