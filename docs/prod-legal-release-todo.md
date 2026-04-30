# Production legal release TODO

Status: blocking item before public paid production release.

The application should treat trainer Pankova as:
- provider of fitness/training services,
- seller of credit packages,
- controller of client personal data.

The software author/technical operator should only be described as a technical processor/admin if they have access to production infrastructure or personal data. They should not be presented as the seller of training services unless payments and client contracts are actually under their name/company.

## Values to collect from trainer Pankova

- Full legal name / business name.
- ICO.
- DIC, if applicable.
- Registered address or public contact address.
- Support e-mail for clients.
- GDPR/privacy e-mail, can be same as support e-mail.
- Phone number, if it should be public.
- VAT status / whether invoices or tax documents are issued outside the app.
- Exact cancellation policy:
  - full credit refund threshold,
  - partial credit refund threshold, if any,
  - no-refund threshold,
  - no-show rule,
  - illness/emergency exceptions, if any,
  - rule when trainer cancels a session.
- Complaint handling:
  - contact e-mail,
  - expected handling time,
  - evidence required for payment/credit issues.
- Final position on withdrawal from contract for online credit purchases and dated training services.

## App files prepared as drafts

- `frontend/src/pages/TermsOfService.tsx`
- `frontend/src/pages/PrivacyPolicy.tsx`

Both pages currently contain visible `[DOPLNIT ...]` placeholders and must not be treated as final legal text until those placeholders are replaced.

## Recommended legal validation

Before paid public launch, have the final Czech wording checked by a Czech lawyer/accountant familiar with:
- consumer services,
- online payments,
- fitness/personal training,
- GDPR and health-related training notes,
- tax/accounting document requirements.
