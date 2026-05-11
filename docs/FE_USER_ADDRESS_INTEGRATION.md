# FE Integration: User Address Book

All address APIs require `Authorization: Bearer <token>`.

## Address Object

```json
{
  "addressId": 1,
  "fullName": "Nguyen Van A",
  "phoneNumber": "0900000000",
  "email": "user@example.com",
  "address": "So 1, Duong A",
  "toDistrictId": 1542,
  "toWardCode": "1B1515",
  "isDefault": true
}
```

## APIs

```http
GET /api/v1/user-address/get-addresses
GET /api/v1/user-address/get-default
POST /api/v1/user-address/create
POST /api/v1/user-address/update?addressId=1
DELETE /api/v1/user-address/delete?addressId=1
POST /api/v1/user-address/set-default?addressId=1
```

Create/update body:

```json
{
  "fullName": "Nguyen Van A",
  "phoneNumber": "0900000000",
  "email": "user@example.com",
  "address": "So 1, Duong A",
  "toDistrictId": 1542,
  "toWardCode": "1B1515",
  "isDefault": true
}
```

The first created address is automatically default. Setting one address as default unsets the others.

## Checkout With Saved Address

`POST /api/v1/order/order-products` now accepts optional `addressId`.

When `addressId` is present, FE does not need to send manual address fields:

```json
{
  "addressId": 1,
  "paymentMethod": "cash",
  "productOrderInputs": [],
  "totalPrice": 100000,
  "shippingFee": 15000
}
```

Manual checkout without `addressId` still works, but FE must send:

- `fullName`
- `phoneNumber`
- `email`
- `address`
- `toDistrictId`
- `toWardCode`
