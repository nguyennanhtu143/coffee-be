# Huong Dan Tich Hop FE: Dang Ky Bang Email OTP

Tai lieu nay mo ta flow dang ky tai khoan moi bang OTP gui qua email.

## Tong Quan Flow

Dang ky gom 3 API public:

1. `POST /api/v1/user/sign-up`: FE gui form dang ky, backend gui OTP ve email va luu pending registration trong Redis.
2. `POST /api/v1/user/verify-register-otp`: FE gui `email + otp`, backend tao user neu OTP dung.
3. `POST /api/v1/user/resend-register-otp`: FE gui `email`, backend gui lai OTP moi neu registration con trong Redis.

OTP co TTL 5 phut. Backend luu key Redis theo email:

- `register:otp:{email}`
- `register:data:{email}`

FE khong can gui lai full form o buoc verify vi backend da luu pending registration trong Redis.

## 1. Gui Dang Ky Va Nhan OTP

Request:

```http
POST /api/v1/user/sign-up
Content-Type: application/json
```

Body:

```json
{
  "username": "nguyenvana",
  "password": "12345678",
  "fullName": "Nguyen Van A",
  "phoneNumber": "0900000000",
  "email": "user@example.com"
}
```

Response thanh cong:

```text
OTP_SENT
```

FE nen chuyen user sang man nhap OTP va giu lai email dang verify.

Loi thuong gap:

- `400 USERNAME_IS_EXISTS`: username da ton tai.
- `400 EMAIL_IS_EXISTS`: email da ton tai.
- `400 EMAIL_SEND_FAILED`: backend chua cau hinh SMTP dung hoac mail server khong ket noi duoc.
- `400 <validation message>`: thieu field hoac email/password khong hop le.

## 2. Xac Nhan OTP

Request:

```http
POST /api/v1/user/verify-register-otp
Content-Type: application/json
```

Body:

```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

Response thanh cong:

```text
REGISTER_SUCCESS
```

Sau response nay user da duoc tao trong DB. FE co the chuyen sang man login.

Loi thuong gap:

- `400 REGISTER_OTP_INVALID`: OTP sai.
- `400 REGISTER_OTP_EXPIRED`: OTP het han hoac pending registration khong con trong Redis.
- `400 USERNAME_IS_EXISTS` hoac `400 EMAIL_IS_EXISTS`: username/email da bi dang ky boi request khac trong luc user dang verify.

## 3. Gui Lai OTP

Request:

```http
POST /api/v1/user/resend-register-otp
Content-Type: application/json
```

Body:

```json
{
  "email": "user@example.com"
}
```

Response thanh cong:

```text
OTP_SENT
```

Loi thuong gap:

- `400 REGISTER_OTP_EXPIRED`: khong con pending registration trong Redis, FE nen dua user quay lai form dang ky.

## FE Flow Goi Y

1. User submit form dang ky.
2. Goi `POST /api/v1/user/sign-up`.
3. Neu thanh cong, hien man OTP va countdown 5 phut.
4. User nhap OTP, goi `POST /api/v1/user/verify-register-otp`.
5. Neu `REGISTER_SUCCESS`, chuyen sang login.
6. Neu OTP sai, hien loi va cho nhap lai.
7. Neu OTP het han, cho bam resend neu pending data con ton tai; neu resend cung bao expired thi quay lai form dang ky.

Vi du:

```ts
await fetch('/api/v1/user/sign-up', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username,
    password,
    fullName,
    phoneNumber,
    email,
  }),
});
```

```ts
await fetch('/api/v1/user/verify-register-otp', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email,
    otp,
  }),
});
```

```ts
await fetch('/api/v1/user/resend-register-otp', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email }),
});
```

## Backend Config Can Co

Moi truong backend can co Redis va SMTP.

Redis properties vi du:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Mail properties vi du:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<smtp_username>
spring.mail.password=<smtp_app_password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Khong commit username/password SMTP vao Git.

Neu gap loi:

```text
Couldn't connect to host, port: localhost, 25
```

nghia la backend dang chua co `spring.mail.host` hop le nen mail sender fallback ve SMTP mac dinh `localhost:25`. Hay cau hinh SMTP that trong `src/main/resources/application.properties` local.

Vi du Gmail:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_google_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Voi Gmail, `spring.mail.password` phai la App Password, khong phai password dang nhap Google thong thuong.
