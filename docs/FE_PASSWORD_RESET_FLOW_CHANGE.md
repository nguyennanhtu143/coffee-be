# FE Change Note: Password Reset Flow Split

Password reset is now a 3-step flow instead of verifying OTP and setting the new password in one request.

## New Flow

1. `POST /api/v1/user/forgot-password`
   - Body: `{ "email": "user@example.com" }`
   - Success: `OTP_SENT`

2. `POST /api/v1/user/verify-password-reset-otp`
   - Body: `{ "email": "user@example.com", "otp": "123456" }`
   - Success: `PASSWORD_RESET_OTP_VERIFIED`

3. `POST /api/v1/user/reset-password`
   - Body: `{ "email": "user@example.com", "newPassword": "newPassword123" }`
   - Success: `PASSWORD_RESET_SUCCESS`

## Important FE Update

Do not send `otp` to `/reset-password` anymore.

After OTP verify succeeds, backend stores a temporary Redis flag:

```text
password-reset:verified:{email}
```

If FE calls `/reset-password` before OTP verification, backend returns:

```text
PASSWORD_RESET_NOT_VERIFIED
```
