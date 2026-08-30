# Chat Flow Specifications
## Acme Claims Processing Platform
**Version:** 1.0  
**Status:** Approved  
**Date:** October 26, 2023  
**Owner:** UX & Product Team  

---

## Overview
This document defines all conversational flows between users and the Acme Claims Platform, including GenAI-assisted interactions, chatbot dialogs, and adjuster communication templates. All AI interactions route through the Central GenAI Gateway with PII masking.

---

## 1. Policyholder FNOL Chatbot Flow

### 1.1 Entry Point
**Trigger:** User clicks "File a Claim" on portal/app.

### 1.2 Conversation Flow

```
┌─────────────────────────────────────────────────────────────┐
│ BOT: Hello! I'm here to help you file a new claim.          │
│      To get started, can you please provide your            │
│      policy number?                                         │
│                                                             │
│ USER: [Enters: POL-12345678]                                │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Validates policy against core system]              │
│                                                             │
│ IF VALID:                                                   │
│ BOT: Thank you! I found your Auto Insurance policy.         │
│      Are you filing this claim for yourself?                │
│      [Yes] [No - Filing for someone else]                   │
│                                                             │
│ IF INVALID:                                                 │
│ BOT: I couldn't find a policy with that number.             │
│      Please check and try again, or call 1-800-XXX-XXXX.    │
│      [Retry] [Speak to Agent]                               │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ BOT: When did the incident occur?                           │
│      [Date Picker]                                          │
│                                                             │
│ USER: [Selects: October 25, 2023]                           │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ BOT: Please describe what happened in your own words.       │
│      Be as detailed as possible.                            │
│                                                             │
│ USER: "I was stopped at a red light when another car        │
│        hit me from behind. The other driver said they       │
│        were distracted by their phone."                     │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Sends masked description to GenAI Gateway]         │
│ GENAI: [Analyzes sentiment, extracts entities]              │
│                                                             │
│ BOT: I understand. It sounds like a rear-end collision      │
│      where the other driver may be at fault.                │
│                                                             │
│      Was anyone injured in the accident?                    │
│      [Yes] [No]                                             │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ IF YES:                                                     │
│ BOT: I'm sorry to hear that. Is medical attention needed?   │
│      [Emergency services called] [Will see doctor later]    │
│      [Already treated]                                      │
│                                                             │
│ IF NO:                                                      │
│ BOT: Glad to hear no one was injured.                       │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ BOT: Can you upload photos of the damage?                   │
│      You can upload up to 10 images.                        │
│      [Upload Button] [Skip for Now]                         │
│                                                             │
│ USER: [Uploads 3 photos]                                    │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ BOT: Almost done! Please confirm your contact information:  │
│      Email: j***@example.com                                │
│      Phone: (555) ***-1234                                  │
│      [Confirm] [Edit]                                       │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Creates claim, assigns ID]                         │
│ GENAI: [Generates summary for adjuster]                     │
│                                                             │
│ BOT: Your claim has been submitted successfully!            │
│                                                             │
│      Claim Number: CLM-2023-789456                          │
│      Estimated Processing Time: 2-3 business days           │
│                                                             │
│      What would you like to do next?                        │
│      [View Claim Status] [Download Confirmation] [Done]     │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 Edge Cases & Error Handling

| Scenario | Bot Response |
|----------|--------------|
| Policy expired | "Your policy appears to have expired on [date]. Claims for expired policies require special review. Would you like to speak with an agent?" |
| Duplicate claim detected | "We found a similar claim filed on [date] with status [status]. Is this the same incident?" |
| High-risk indicators (fraud) | "Thank you for the information. Your claim requires additional review. A claims specialist will contact you within 24 hours." |
| System unavailable | "We're experiencing technical difficulties. Please try again in a few minutes or call 1-800-XXX-XXXX." |
| User frustration (negative sentiment) | "I understand this is frustrating. Would you prefer to speak with a human agent?" |

---

## 2. Adjuster GenAI Assistant Flow

### 2.1 Entry Point
**Trigger:** Adjuster opens a claim file and clicks "AI Summary".

### 2.2 Conversation Flow

```
┌─────────────────────────────────────────────────────────────┐
│ ADJUSTER: [Clicks "Generate AI Summary"]                    │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Masks PII in claim documents]                      │
│         [Sends to GenAI Gateway with triage prompt]         │
│                                                             │
│ GENAI: [Processes and returns structured response]          │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ AI ASSISTANT:                                               │
│ ═══════════════════════════════════════                     │
│ CLAIM SUMMARY (CLM-2023-789456)                             │
│                                                             │
│ 📋 INCIDENT TYPE: Auto - Rear-end Collision                 │
│ ⚠️  RISK SCORE: 45/100 (Medium)                             │
│ 👤 LIABILITY: Clear - Other party at fault (rear-end)       │
│ 💰 ESTIMATED DAMAGE: $4,200 - $5,800                        │
│                                                             │
│ KEY EVIDENCE:                                               │
│ • Police report confirms other driver cited for distraction │
│ • Photos show moderate rear bumper damage                   │
│ • No prior claims history for this policyholder             │
│ • Witness statement available (attached)                    │
│                                                             │
│ RECOMMENDED ACTION: FAST_TRACK                              │
│ Justification: Clear liability, damage within authority     │
│                limit, cooperative policyholder              │
│                                                             │
│ [Approve Fast Track] [Request More Info] [Manual Review]    │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ ADJUSTER: [Clicks "Request More Info"]                      │
│                                                             │
│ AI ASSISTANT: What additional information do you need?      │
│               [Repair Estimate] [Medical Records]           │
│               [Police Report Full Text] [Custom Request]    │
│                                                             │
│ ADJUSTER: [Types: "Need official repair estimate from       │
│            certified shop"]                                 │
│                                                             │
│ AI ASSISTANT: I'll draft a message to the policyholder      │
│               requesting this. Review before sending:       │
│               ─────────────────────────                     │
│               "Dear [Name], To process your claim, we       │
│               need an official repair estimate from a       │
│               certified auto body shop. Please upload..."   │
│               ─────────────────────────                     │
│               [Send] [Edit] [Cancel]                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Adjudication Draft Flow

```
┌─────────────────────────────────────────────────────────────┐
│ ADJUSTER: [Clicks "Draft Decision"]                         │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ AI ASSISTANT:                                               │
│ ═══════════════════════════════════════                     │
│ DECISION DRAFT                                              │
│                                                             │
│ RECOMMENDATION: APPROVE                                     │
│ PAYOUT AMOUNT: $4,850.00                                    │
│                                                             │
│ REASONING:                                                  │
│ Based on the evidence provided, this claim meets all        │
│ criteria for approval under policy POL-12345678. The        │
│ other party's liability is clearly established through      │
│ police report #12345 and photographic evidence. The         │
│ estimated repair cost of $4,850 falls within the            │
│ comprehensive coverage limit and is consistent with         │
│ market rates for similar damage.                            │
│                                                             │
│ POLICY COVERAGE VERIFICATION:                               │
│ ✓ Collision coverage active                                 │
│ ✓ Deductible: $500 (to be subtracted from payout)           │
│ ✓ No exclusions apply                                       │
│                                                             │
│ REGULATORY COMPLIANCE:                                      │
✓ State notification requirements met                         │
✓ Fraud indicators: None detected                             │
│                                                             │
│ [Approve & Send] [Modify Amount] [Reject] [Escalate]        │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Status Update Notifications (Automated)

### 3.1 SMS/Email Templates

#### Claim Submitted
```
Subject: Claim Submitted - CLM-2023-789456

Dear [FirstName],

Your claim has been successfully submitted.

Claim Number: CLM-2023-789456
Date of Loss: [Date]
Status: Under Review

Next Steps: A claims adjuster will be assigned within 24 hours.
You can track your claim status at: [Portal Link]

Questions? Reply to this email or call 1-800-XXX-XXXX.

Thank you,
Acme Claims Team
```

#### Claim Assigned
```
Subject: Update on Claim CLM-2023-789456

Dear [FirstName],

Good news! Your claim has been assigned to a claims adjuster.

Adjuster Name: [Name]
Contact: [Phone] | [Email]
Expected Contact: Within 1 business day

Your claim status is now: In Progress

Track your claim: [Portal Link]

Acme Claims Team
```

#### Additional Information Requested
```
Subject: Action Required - Claim CLM-2023-789456

Dear [FirstName],

To continue processing your claim, we need the following:

📄 Document Type: [Repair Estimate / Medical Record / etc.]
📅 Due Date: [Date + 7 days]

Upload documents here: [Secure Upload Link]

Without this information, your claim processing may be delayed.

Questions? Contact your adjuster: [Name] at [Phone]

Acme Claims Team
```

#### Claim Approved
```
Subject: Great News - Claim CLM-2023-789456 Approved

Dear [FirstName],

Your claim has been approved!

Approved Amount: $[Amount]
Less Deductible: $[Deductible]
Net Payout: $[NetAmount]

Payment Method: [Direct Deposit / Check]
Expected Payment Date: [Date]

A settlement letter has been sent to your registered email.

Thank you for choosing Acme Insurance.

Acme Claims Team
```

#### Claim Denied
```
Subject: Claim Decision - CLM-2023-789456

Dear [FirstName],

After careful review, we are unable to approve your claim.

Reason: [Specific reason based on policy terms]
Policy Section: [Section Number]

You have the right to appeal this decision within 30 days.
To appeal, please contact us at 1-800-XXX-XXXX or reply to this email.

We understand this may be disappointing. Our team is available
to discuss this decision further.

Acme Claims Team
```

---

## 4. Internal Communication Flows

### 4.1 Escalation to Senior Adjudicator

```
┌─────────────────────────────────────────────────────────────┐
│ JUNIOR ADJUSTER: [Flags claim for escalation]               │
│ Reason: "Payout exceeds my authority limit ($50k)"          │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Notifies Senior Adjudicator via dashboard + email] │
│                                                             │
│ NOTIFICATION:                                               │
│ ═══════════════════════════════════════                     │
│ ESCALATION ALERT                                            │
│                                                             │
│ Claim: CLM-2023-789456                                      │
│ Current Adjuster: [Name]                                    │
│ Reason: Exceeds authority limit                             │
│ Recommended Payout: $78,500                                 │
│                                                             │
│ [Review Now] [Delegate to Another] [Add Notes]              │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SENIOR ADJUDICATOR: [Reviews claim + AI summary]            │
│                                                             │
│ [Types note]: "Reviewed. Damage assessment accurate.        │
│                Approving full amount."                      │
│                                                             │
│ [Approve] [Request Revision] [Call Adjuster]                │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Updates claim status, notifies junior adjuster]    │
│         [Triggers payout workflow]                          │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Fraud Investigation Referral

```
┌─────────────────────────────────────────────────────────────┐
│ SYSTEM: [Detects fraud indicators via GenAI analysis]       │
│ Indicators:                                                 │
│ • Inconsistent statements across documents                  │
│ • Prior similar claims (3 in 12 months)                     │
│ • Damage pattern inconsistent with reported cause           │
└─────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────┐
│ ALERT TO INVESTIGATIONS TEAM:                               │
│ ═══════════════════════════════════════                     │
│ FRAUD REFERRAL - HIGH PRIORITY                              │
│                                                             │
│ Claim: CLM-2023-789456                                      │
│ Policyholder: [Masked Name]                                 │
│ Risk Score: 92/100                                          │
│                                                             │
│ Red Flags:                                                  │
│ 🔴 Multiple recent claims                                   │
│ 🔴 Statement inconsistencies                                │
│ 🔴 Unusual damage pattern                                   │
│                                                             │
│ Recommended Action: SUSPEND processing pending investigation│
│                                                             │
│ [Accept Case] [Request More Info] [Dismiss Flag]            │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. GenAI Gateway Prompt Library

### 5.1 Standard Prompts (Stored in Gateway)

```yaml
prompts:
  fnol_classification:
    version: "1.2"
    template: |
      You are an insurance claims classification expert.
      Analyze the following FNOL description and extract:
      1. Incident category (Auto, Property, Liability, Other)
      2. Sub-category (Collision, Theft, Fire, Water, etc.)
      3. Potential liability (Clear, Disputed, Unknown)
      4. Urgency level (Low, Medium, High, Critical)
      
      Input (masked): {{description}}
      Policy type: {{policyType}}
      
      Output JSON only:
      {
        "category": "...",
        "subCategory": "...",
        "liability": "...",
        "urgency": "...",
        "confidence": 0.XX
      }
    
    max_tokens: 200
    temperature: 0.3
    
  risk_scoring:
    version: "1.1"
    template: |
      You are a claims risk assessment AI.
      Score this claim from 1-100 (higher = more risky).
      Consider:
      - Claim amount relative to policy limits
      - Policyholder claims history
      - Circumstances of loss
      - Potential fraud indicators
      
      Input data (masked): {{claimData}}
      Historical claims count: {{historyCount}}
      
      Output JSON only:
      {
        "riskScore": N,
        "factors": ["factor1", "factor2"],
        "recommendation": "FAST_TRACK|STANDARD|INVESTIGATE"
      }
    
    max_tokens: 250
    temperature: 0.2
    
  settlement_draft:
    version: "1.3"
    template: |
      You are a senior claims adjudicator.
      Draft a settlement recommendation based on:
      - Policy coverage terms
      - Evidence provided
      - Damage assessments
      - Liability determination
      
      Be professional, factual, and cite specific evidence.
      
      Claim details (masked): {{claimDetails}}
      Policy limits: {{limits}}
      Evidence summary: {{evidence}}
      
      Output format:
      RECOMMENDATION: [APPROVE/PARTIAL/DENY]
      AMOUNT: $X,XXX.XX
      REASONING: [2-3 paragraphs]
      CONDITIONS: [If any]
    
    max_tokens: 500
    temperature: 0.4
```

---

## 6. Compliance & Audit Requirements

### 6.1 Conversation Logging
- **All** chatbot conversations logged with timestamps
- PII automatically redacted in logs after 30 days
- GenAI prompts and responses stored for 7 years (audit trail)
- User consent recorded at conversation start

### 6.2 Human Handoff Protocol
- Users can request human agent at ANY point in conversation
- Full conversation transcript transferred to agent
- Maximum wait time for handoff: 2 minutes
- Escalation path: Chatbot → Junior Agent → Senior Agent → Supervisor

### 6.3 Bias & Fairness Monitoring
- Monthly audit of GenAI decisions by demographic factors
- A/B testing of alternative prompt templates
- Human review required for all denials >$10k
- Quarterly third-party fairness assessment

---

## Appendix A: Intent Recognition Matrix

| User Utterance | Detected Intent | Confidence Threshold |
|----------------|-----------------|---------------------|
| "I want to file a claim" | FNOL_START | 0.95 |
| "My car was hit" | FNOL_START + AUTO_CATEGORY | 0.92 |
| "Where is my claim?" | CHECK_STATUS | 0.98 |
| "I need to upload a document" | UPLOAD_REQUEST | 0.94 |
| "This is taking too long" | COMPLAINT + ESCALATE | 0.88 |
| "Speak to a person" | HUMAN_HANDOFF | 0.99 |
| "What's my deductible?" | POLICY_QUERY | 0.96 |

## Appendix B: Multilingual Support

Supported languages (Phase 1):
- English (US)
- Spanish (US)
- French (Canada)

Translation handled by GenAI Gateway with human-reviewed templates for critical messages (denials, legal notices).
