# Settings Menu Alignment Task: iOS to Android

## EVIDENCE-FIRST EXECUTION REQUIREMENTS

Before implementing ANY business requirements below, you MUST complete these phases IN ORDER:

### PHASE 1: COLLECT ANDROID SETTINGS EVIDENCE
**STOP. Do not proceed past this phase until you have:**
1. Read the Android SettingsScreen implementation file(s)
2. Shown me the EXACT code with line numbers
3. Documented what Android settings actually shows (not what you think it shows)

Show me:
- The Android SettingsScreen.kt filename and full implementation
- The EXACT menu structure (with line numbers):
  - Appearance section and its options
  - Security section and its options
  - Backup & Restore section and its options
  - About section and its options
- The EXACT styling used:
  - Section headers style
  - List item style
  - Icons used
  - Colors and spacing
- Screenshots of each Android settings section
- The EXACT text labels Android uses

**Wait for me to say "proceed to phase 2"**

### PHASE 2: COLLECT CURRENT iOS SETTINGS EVIDENCE
**STOP. Do not proceed past this phase until you have:**
1. Read the iOS SettingsScreenIntegrated implementation
2. Shown me the EXACT code with line numbers
3. Documented the specific differences from Android

Show me:
- The iOS SettingsScreenIntegrated implementation (with line numbers)
- The current iOS menu structure
- Screenshots of current iOS settings
- A detailed list of EVERY difference from Android:
  - Missing sections
  - Different labels
  - Different icons
  - Different styling
  - Different functionality

**Wait for me to say "proceed to phase 3"**

### PHASE 3: IMPLEMENTATION PLAN
**STOP. Before making ANY changes:**
1. List the EXACT changes needed to match Android
2. Show the EXACT lines you will modify
3. Confirm you will NOT add any new features

Show me:
- File: SettingsScreenIntegrated, Lines: [X-Y], Change: [specific change to match Android]
- Confirm: "I will only change iOS to match existing Android implementation"
- Confirm: "I will not add any new features or improvements"

**Wait for me to say "proceed with changes"**

### PHASE 4: IMPLEMENTATION
Only NOW may you make changes.
- Copy Android's exact structure
- Use Android's exact labels
- Match Android's exact styling
- Do NOT add anything Android doesn't have

---

## BUSINESS REQUIREMENTS

### Core Requirement
Make iOS SettingsScreenIntegrated look and function EXACTLY like Android's SettingsScreen

### Specific Sections to Align

#### 1. Appearance Section
- Match Android's exact options
- Use same labels
- Use same icons
- Same functionality only

#### 2. Security Section
- Match Android's exact options
- Use same labels
- Use same icons
- Same functionality only

#### 3. Backup & Restore Section
- Match Android's exact options
- Use same labels
- Use same icons
- Same functionality only

#### 4. About Section
- Match Android's exact options
- Use same labels
- Use same icons
- Same functionality only

### NOT ALLOWED
- Do NOT add new features
- Do NOT improve anything
- Do NOT reorganize differently than Android
- Do NOT use different icons or colors
- Do NOT add helpful explanations Android doesn't have

## VERIFICATION
After implementation, you MUST show:
- Side-by-side screenshot: Android settings vs iOS settings
- Confirmation that all sections match exactly
- Confirmation that all labels match exactly
- Confirmation that no new features were added
- Line-by-line comparison of menu items

## DEFINITION OF DONE
- [ ] iOS has exact same Appearance section as Android
- [ ] iOS has exact same Security section as Android
- [ ] iOS has exact same Backup & Restore section as Android
- [ ] iOS has exact same About section as Android
- [ ] All labels match exactly
- [ ] All icons match exactly
- [ ] No additional features added
- [ ] Screenshots provided as evidence