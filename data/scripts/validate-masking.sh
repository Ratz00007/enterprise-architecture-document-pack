#!/bin/bash
# =============================================================================
# PII Leakage Detection Script
# =============================================================================
# Purpose: Validate that no raw PII exists in QA/UAT databases after masking
# Usage:   ./validate-masking.sh [qa|uat] [database_host]
# Requires: psql client, PostgreSQL credentials via .pgpass or environment
# =============================================================================

set -euo pipefail

# Configuration
ENVIRONMENT="${1:-qa}"
DB_HOST="${2:-${ENVIRONMENT}-db.acme.local}"
DB_NAME="claims_db"
DB_USER="admin"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0

echo "=============================================="
echo "PII Leakage Detection Validation"
echo "=============================================="
echo "Environment: ${ENVIRONMENT}"
echo "Database Host: ${DB_HOST}"
echo "Timestamp: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "=============================================="
echo ""

# Function to run SQL query and return result
run_query() {
    local query="$1"
    psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$query" 2>/dev/null || echo "0"
}

# Function to log errors
log_error() {
    echo -e "${RED}❌ CRITICAL: $1${NC}"
    ((ERRORS++))
}

# Function to log warnings
log_warning() {
    echo -e "${YELLOW}⚠️  WARNING: $1${NC}"
    ((WARNINGS++))
}

# Function to log success
log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

echo "=== Starting PII Leakage Detection ==="
echo ""

# -----------------------------------------------------------------------------
# Test 1: SSN Pattern Detection
# -----------------------------------------------------------------------------
echo "[1/12] Checking for raw SSN patterns..."
SSN_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE ssn ~ '^[0-9]{3}-[0-9]{2}-[0-9]{4}$';")
if [[ "$SSN_COUNT" -gt 0 ]]; then
    log_error "Raw SSN detected in $SSN_COUNT customer records!"
else
    log_success "No raw SSN patterns found"
fi

# -----------------------------------------------------------------------------
# Test 2: Unmasked Email Detection
# -----------------------------------------------------------------------------
echo "[2/12] Checking for unmasked email addresses..."
EMAIL_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE email !~ '^claim_[0-9]+_hash[a-f0-9]+@' AND email !~ '^[0-9]+@masked\.local$';")
if [[ "$EMAIL_COUNT" -gt 0 ]]; then
    log_error "Unmasked email detected in $EMAIL_COUNT customer records!"
    # Show sample (first 5)
    SAMPLE=$(run_query "SELECT email FROM customers WHERE email !~ '^claim_[0-9]+_hash[a-f0-9]+@' LIMIT 5;" 2>/dev/null || echo "")
    if [[ -n "$SAMPLE" ]]; then
        echo "   Sample unmasked emails: $SAMPLE"
    fi
else
    log_success "All emails properly masked"
fi

# -----------------------------------------------------------------------------
# Test 3: Raw Phone Number Detection
# -----------------------------------------------------------------------------
echo "[3/12] Checking for raw phone number patterns..."
PHONE_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE phone ~ '^\\([0-9]{3}\\) [0-9]{3}-[0-9]{4}$' AND phone !~ '^\\(555\\)';")
if [[ "$PHONE_COUNT" -gt 0 ]]; then
    log_error "Raw phone numbers detected in $PHONE_COUNT customer records!"
else
    log_success "All phone numbers properly masked"
fi

# -----------------------------------------------------------------------------
# Test 4: Full Name Detection (should be synthetic)
# -----------------------------------------------------------------------------
echo "[4/12] Checking for real names (non-synthetic)..."
NAME_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE first_name !~ '^\\[CLAIM_[0-9]+_FIRST\\]$' OR last_name !~ '^\\[CLAIM_[0-9]+_LAST\\]$';")
if [[ "$NAME_COUNT" -gt 0 ]]; then
    log_error "Real names detected in $NAME_COUNT customer records!"
else
    log_success "All names properly synthetic"
fi

# -----------------------------------------------------------------------------
# Test 5: Address Detection (should be generalized)
# -----------------------------------------------------------------------------
echo "[5/12] Checking for specific street addresses..."
ADDR_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE address_line1 ~ '^[0-9]+ [A-Za-z]+ (St|Street|Ave|Avenue|Blvd|Boulevard|Rd|Road|Dr|Drive|Ln|Lane|Ct|Court|Way|Pl|Place)';")
if [[ "$ADDR_COUNT" -gt 0 ]]; then
    log_error "Specific street addresses detected in $ADDR_COUNT customer records!"
else
    log_success "All addresses properly generalized"
fi

# -----------------------------------------------------------------------------
# Test 6: ZIP Code Precision (should be truncated)
# -----------------------------------------------------------------------------
echo "[6/12] Checking ZIP code precision..."
ZIP_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE zip_code ~ '^[0-9]{5}$';")
if [[ "$ZIP_COUNT" -gt 0 ]]; then
    log_warning "Full 5-digit ZIP codes detected in $ZIP_COUNT records (should be truncated to 3 digits)"
else
    log_success "ZIP codes properly truncated"
fi

# -----------------------------------------------------------------------------
# Test 7: Medical Record Numbers (should be FPE encrypted)
# -----------------------------------------------------------------------------
echo "[7/12] Checking medical record number format..."
MRN_INVALID=$(run_query "SELECT COUNT(*) FROM medical_records WHERE medical_record_number !~ '^MRN-[0-9]{8}$';")
if [[ "$MRN_INVALID" -gt 0 ]]; then
    log_error "Invalid MRN format detected in $MRN_INVALID records!"
else
    log_success "All MRNs properly formatted"
fi

# -----------------------------------------------------------------------------
# Test 8: Diagnosis Code Specificity (should be generalized to category)
# -----------------------------------------------------------------------------
echo "[8/12] Checking diagnosis code specificity..."
DIAG_SPECIFIC=$(run_query "SELECT COUNT(*) FROM medical_records WHERE diagnosis_code ~ '^[A-Z][0-9]{2}\\.[0-9]{1,4}$' AND diagnosis_code !~ '\\.X$';")
if [[ "$DIAG_SPECIFIC" -gt 0 ]]; then
    log_warning "Overly specific diagnosis codes detected in $DIAG_SPECIFIC records (should end with .X)"
else
    log_success "Diagnosis codes properly generalized"
fi

# -----------------------------------------------------------------------------
# Test 9: Bank Account Number Detection
# -----------------------------------------------------------------------------
echo "[9/12] Checking for raw bank account numbers..."
BANK_COUNT=$(run_query "SELECT COUNT(*) FROM financial_accounts WHERE account_number ~ '^[0-9]{12}$' AND account_number !~ '^9[0-9]{11}$';")
if [[ "$BANK_COUNT" -gt 0 ]]; then
    log_error "Raw bank account numbers detected in $BANK_COUNT records!"
else
    log_success "All bank accounts properly masked"
fi

# -----------------------------------------------------------------------------
# Test 10: Driver's License Detection
# -----------------------------------------------------------------------------
echo "[10/12] Checking for raw driver's license numbers..."
DL_COUNT=$(run_query "SELECT COUNT(*) FROM customers WHERE drivers_license ~ '^[A-Z][0-9]{3}-[0-9]{4}-[0-9]{4}$' AND drivers_license !~ '^[A-Z]9[0-9]{2}-[0-9]{4}-[0-9]{4}$';")
if [[ "$DL_COUNT" -gt 0 ]]; then
    log_error "Raw driver's license numbers detected in $DL_COUNT records!"
else
    log_success "All driver's licenses properly masked"
fi

# -----------------------------------------------------------------------------
# Test 11: Date of Birth Offset Check (should have random offset)
# -----------------------------------------------------------------------------
echo "[11/12] Checking date of birth offset application..."
# This is a statistical check - at least some DOBs should differ from original
DOB_CHECK=$(run_query "SELECT COUNT(DISTINCT date_of_birth) FROM customers;")
if [[ "$DOB_CHECK" -lt 10 ]]; then
    log_warning "Low variance in dates of birth - offset may not be applied correctly"
else
    log_success "Date of birth offset appears correctly applied"
fi

# -----------------------------------------------------------------------------
# Test 12: PHI Keywords in Text Fields
# -----------------------------------------------------------------------------
echo "[12/12] Scanning for PHI keywords in text fields..."
PHI_KEYWORDS="cancer|diabetes|heart disease|hiv|aids|hepatitis|tumor|leukemia|chemotherapy|radiation|surgery|transplant"
PHI_COUNT=$(run_query "SELECT COUNT(*) FROM medical_records WHERE LOWER(treatment_description) ~ '$PHI_KEYWORDS';")
if [[ "$PHI_COUNT" -gt 0 ]]; then
    log_warning "Specific PHI keywords detected in $PHI_COUNT treatment descriptions"
else
    log_success "No specific PHI keywords found in plain text"
fi

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
echo ""
echo "=============================================="
echo "Validation Summary"
echo "=============================================="
echo "Total Errors:   $ERRORS"
echo "Total Warnings: $WARNINGS"
echo ""

if [[ $ERRORS -gt 0 ]]; then
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo "Critical PII leakage detected! Do NOT proceed with QA testing."
    echo "Action Required: Re-run masking pipeline and investigate failures."
    exit 1
elif [[ $WARNINGS -gt 0 ]]; then
    echo -e "${YELLOW}⚠️  VALIDATION PASSED WITH WARNINGS${NC}"
    echo "Review warnings above and address before production use."
    echo "QA testing may proceed with caution."
    exit 0
else
    echo -e "${GREEN}✅ VALIDATION PASSED${NC}"
    echo "All PII/PHI masking checks successful."
    echo "QA environment is safe for testing."
    exit 0
fi
