// vars/acmePipeline.groovy
// Global step. Calls the underlying AcmePipeline class.

def call(Map config) {
    def pipeline = new org.acme.claims.AcmePipeline(script: this, config: config)
    pipeline.run()
}
