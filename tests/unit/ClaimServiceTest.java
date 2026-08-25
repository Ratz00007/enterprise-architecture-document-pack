package com.acme.claims.service;

import com.acme.claims.model.Claim;
import com.acme.claims.repository.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClaimService
 */
@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @InjectMocks
    private ClaimService claimService;

    private Claim testClaim;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testClaim = new Claim();
        testClaim.setId(testId);
        testClaim.setClaimNumber("CLM-TEST-001");
        testClaim.setPolicyNumber("POL-123456");
        testClaim.setStatus(Claim.ClaimStatus.FNOL);
        testClaim.setClaimType(Claim.ClaimType.AUTO);
        testClaim.setIncidentDate(LocalDateTime.now());
        testClaim.setReportedDate(LocalDateTime.now());
        testClaim.setDescription("Test claim description");
        testClaim.setEstimatedAmount(new BigDecimal("5000.00"));
    }

    @Test
    void createClaim_shouldSaveAndReturnClaim() {
        // Arrange
        when(claimRepository.save(any(Claim.class))).thenReturn(testClaim);

        // Act
        Claim result = claimService.createClaim(testClaim, "test-user");

        // Assert
        assertNotNull(result);
        assertEquals(Claim.ClaimStatus.FNOL, result.getStatus());
        assertEquals("test-user", result.getCreatedBy());
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    void getClaimById_whenExists_shouldReturnClaim() {
        // Arrange
        when(claimRepository.findById(testId)).thenReturn(Optional.of(testClaim));

        // Act
        Optional<Claim> result = claimService.getClaimById(testId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testId, result.get().getId());
        verify(claimRepository, times(1)).findById(testId);
    }

    @Test
    void getClaimById_whenNotExists_shouldReturnEmpty() {
        // Arrange
        when(claimRepository.findById(testId)).thenReturn(Optional.empty());

        // Act
        Optional<Claim> result = claimService.getClaimById(testId);

        // Assert
        assertFalse(result.isPresent());
        verify(claimRepository, times(1)).findById(testId);
    }

    @Test
    void transitionToTriage_shouldUpdateStatus() {
        // Arrange
        when(claimRepository.findById(testId)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Claim result = claimService.transitionToTriage(testId, 5, 0.85, "test-user");

        // Assert
        assertEquals(Claim.ClaimStatus.TRIAGE, result.getStatus());
        assertEquals(5, result.getTriagePriority());
        assertEquals(0.85, result.getTriageScore());
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    void approveClaim_shouldUpdateStatusAndAmount() {
        // Arrange
        BigDecimal approvedAmount = new BigDecimal("4500.00");
        when(claimRepository.findById(testId)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Claim result = claimService.approveClaim(testId, approvedAmount, "test-user");

        // Assert
        assertEquals(Claim.ClaimStatus.APPROVED, result.getStatus());
        assertEquals(approvedAmount, result.getApprovedAmount());
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    void rejectClaim_shouldUpdateStatusAndNotes() {
        // Arrange
        String rejectionReason = "Policy does not cover this type of incident";
        when(claimRepository.findById(testId)).thenReturn(Optional.of(testClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Claim result = claimService.rejectClaim(testId, rejectionReason, "test-user");

        // Assert
        assertEquals(Claim.ClaimStatus.REJECTED, result.getStatus());
        assertEquals(rejectionReason, result.getAdjudicationNotes());
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    void getAllClaims_shouldReturnPageOfClaims() {
        // Arrange
        List<Claim> claims = Arrays.asList(testClaim);
        Page<Claim> expectedPage = new PageImpl<>(claims, PageRequest.of(0, 10), claims.size());
        when(claimRepository.findAll(PageRequest.of(0, 10))).thenReturn(expectedPage);

        // Act
        Page<Claim> result = claimService.getAllClaims(PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(claimRepository, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getClaimCountByStatus_shouldReturnCount() {
        // Arrange
        when(claimRepository.countByStatus(Claim.ClaimStatus.FNOL)).thenReturn(5L);

        // Act
        long count = claimService.getClaimCountByStatus(Claim.ClaimStatus.FNOL);

        // Assert
        assertEquals(5L, count);
        verify(claimRepository, times(1)).countByStatus(Claim.ClaimStatus.FNOL);
    }

    @Test
    void deleteClaim_whenExists_shouldDelete() {
        // Arrange
        when(claimRepository.existsById(testId)).thenReturn(true);
        doNothing().when(claimRepository).deleteById(testId);

        // Act
        claimService.deleteClaim(testId);

        // Assert
        verify(claimRepository, times(1)).deleteById(testId);
    }

    @Test
    void deleteClaim_whenNotExists_shouldThrowException() {
        // Arrange
        when(claimRepository.existsById(testId)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> claimService.deleteClaim(testId));
    }
}
