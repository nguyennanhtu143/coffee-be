# FE Integration: Password OTP And Change Password

## Forgot Password

```http
POST /api/v1/user/forgot-password
Content-Type: application/json
```

```json
{
  "email": "user@example.com"
}
```

Success:

```text
OTP_SENT
```

Common errors:

- `EMAIL_NOT_FOUND`
- `EMAIL_SEND_FAILED`

## Verify Reset OTP

```http
POST /api/v1/user/verify-password-reset-otp
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

Success:

```text
PASSWORD_RESET_OTP_VERIFIED
```

Common errors:

- `PASSWORD_RESET_OTP_EXPIRED`
- `PASSWORD_RESET_OTP_INVALID`
- `EMAIL_NOT_FOUND`

## Reset Password

Call this only after `PASSWORD_RESET_OTP_VERIFIED`.

```http
POST /api/v1/user/reset-password
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "newPassword": "newPassword123"
}
```

Success:

```text
PASSWORD_RESET_SUCCESS
```

Common errors:

- `PASSWORD_RESET_NOT_VERIFIED`
- `EMAIL_NOT_FOUND`
- validation error if new password is shorter than 6 characters.

## Change Password

```http
POST /api/v1/user/change-password
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

Success:

```text
PASSWORD_CHANGED
```

Common errors:

- `INCORRECT_PASSWORD`
- validation error if new password is shorter than 6 characters.
