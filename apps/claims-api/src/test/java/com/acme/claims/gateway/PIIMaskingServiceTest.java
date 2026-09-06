package com.acme.claims.gateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PIIMaskingServiceTest {

    private final PIIMaskingService service = new PIIMaskingService();

    @Test
    void masksSsnPhoneEmailAndCardNumbers() {
        String masked = service.maskPII(
            "SSN 123-45-6789, phone 555-123-4567, mail john.doe@example.com, card 4111 1111 1111 1111");

        assertThat(masked).doesNotContain("123-45-6789").doesNotContain("555-123-4567")
            .doesNotContain("john.doe@example.com").doesNotContain("4111 1111 1111 1111")
            .contains("***-**-****").contains("***-***-****").contains("***@***.***")
            .contains("****-****-****-****");
    }

    @Test
    void leavesCleanTextUntouched() {
        assertThat(service.maskPII("Rear-end collision on I-95, no injuries"))
            .isEqualTo("Rear-end collision on I-95, no injuries");
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(service.maskPII(null)).isNull();
        assertThat(service.maskPII("")).isEmpty();
        assertThat(service.containsPII(null)).isFalse();
    }

    @Test
    void maskFieldHonoursFieldType() {
        assertThat(service.maskField("123-45-6789", "ssn")).isEqualTo("***-**-****");
        assertThat(service.maskField("Jane Doe", "name")).isEqualTo("J**** D****");
        assertThat(service.maskField("anything", "unknown-type")).isEqualTo("anything");
    }

    @Test
    void containsPiiDetectsSensitiveContent() {
        assertThat(service.containsPII("reach me at john.doe@example.com")).isTrue();
        assertThat(service.containsPII("nothing sensitive here")).isFalse();
    }

    @Test
    void maskFieldHandlesNullAndEmptyAndEveryType() {
        assertThat(service.maskField(null, "ssn")).isNull();
        assertThat(service.maskField("", "ssn")).isEmpty();
        assertThat(service.maskField("555-123-4567", "phone")).isEqualTo("***-***-****");
        assertThat(service.maskField("jane@example.com", "e-mail")).isEqualTo("***@***.***");
        assertThat(service.maskField("4111111111111111", "cc")).isEqualTo("****-****-****-****");
        assertThat(service.maskField("123-45-6789", "social_security")).isEqualTo("***-**-****");
        assertThat(service.maskField("4111-1111-1111-1111", "card_number")).isEqualTo("****-****-****-****");
        assertThat(service.maskField("123-45-6789", "SSN")).isEqualTo("***-**-****");
    }

    @Test
    void maskNameKeepsOnlyInitials() {
        assertThat(service.maskName(null)).isNull();
        assertThat(service.maskName("")).isEmpty();
        assertThat(service.maskName("Jane")).isEqualTo("J****");
        assertThat(service.maskName("Jane Maria Doe")).isEqualTo("J**** M**** D****");
    }

    @Test
    void maskAddressRedactsStreetWhenPresent() {
        assertThat(service.maskAddress(null)).isNull();
        assertThat(service.maskAddress("")).isEmpty();
        assertThat(service.maskAddress("12 Oak Street, Springfield, IL"))
            .contains("[REDACTED],").contains("Springfield, IL");
        assertThat(service.maskAddress("Springfield, IL")).isEqualTo("Springfield, IL");
    }

    @Test
    void containsPiiDetectsEachPattern() {
        assertThat(service.containsPII("ssn 987-65-4321 end")).isTrue();
        assertThat(service.containsPII("call 5551234567 now")).isTrue();
        assertThat(service.containsPII("card 4111-1111-1111-1111")).isTrue();
        assertThat(service.containsPII("plain text")).isFalse();
    }

    @Test
    void maskPiiHandlesDottedPhoneAndCompactCard() {
        assertThat(service.maskPII("555.123.4567")).isEqualTo("***-***-****");
        assertThat(service.maskPII("4111111111111111")).isEqualTo("****-****-****-****");
    }
}
