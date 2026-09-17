// src/org/acme/claims/Rollback.groovy
// Skeleton — full implementation lives in the build-out workstream.

package org.acme.claims

class Rollback implements Serializable {
    private static final long serialVersionUID = 1L

    def script
    Map config
    String env
    String targetDigest

    Rollback(script, Map config, String env, String targetDigest) {
        this.script = script
        this.config = config
        this.env = env
        this.targetDigest = targetDigest
    }

    void run() {
        // 1. Approval gate (same role as a forward promotion)
        def role = config.approvalRoles[env]
        script.input(
            message: "Rollback ${config.name} in ${env} to ${targetDigest}?",
            submitter: role,
            ok: 'Approve',
        )

        // 2. Verify the cosign signature of the rollback target
        script.sh "cosign verify --key cosign.pub ${config.containerImage}@${targetDigest}"

        // 3. Run the same promote playbook with the old digest
        def inv = "inventories/${env}.yml"
        script.sh "ansible-playbook -i ${inv} playbooks/promote.yml -e app_digest=${targetDigest}"

        // 4. Post-rollback health check
        script.sh "scripts/smoke.sh ${env}"

        // 5. Write to the audit log with `action=rollback`
        new org.acme.claims.Evidence(script: script, config: config, env: env, digest: targetDigest).write()
    }
}
