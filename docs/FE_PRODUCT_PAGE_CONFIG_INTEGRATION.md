# Huong Dan Tich Hop FE: Product Page Size Config

Tai lieu nay mo ta cach FE tich hop tinh nang admin cau hinh so san pham hien thi tren moi page cho danh sach san pham o user site va admin site.

## Contract Moi

Backend khong con dua vao `Authorization` header de nhan dien user/admin site khi goi API list product public.

FE phai truyen site tren URL bang query param:

- `site=USER`: dung config `userPageSize`.
- `site=ADMIN`: dung config `adminPageSize`.
- Neu khong truyen `site`, backend mac dinh `USER`.
- Backend chap nhan ca lowercase nhu `site=user` hoac `site=admin`, nhung FE nen dung uppercase de contract ro rang.

Neu FE truyen query param `size`, backend uu tien `size` do va khong dung config.

## API Cau Hinh Page Size

### Lay config hien tai

```http
GET /api/v1/product/page-config
Authorization: Bearer <admin_token>
```

Chi admin/shop duoc phep goi.

Response:

```json
{
  "userPageSize": 12,
  "adminPageSize": 30
}
```

Neu chua co config trong DB, backend tra default:

```json
{
  "userPageSize": 20,
  "adminPageSize": 20
}
```

### Cap nhat config

```http
POST /api/v1/product/page-config
Authorization: Bearer <admin_token>
Content-Type: application/json
```

Body:

```json
{
  "userPageSize": 12,
  "adminPageSize": 30
}
```

Validation:

- `userPageSize`: bat buoc, tu `1` den `100`.
- `adminPageSize`: bat buoc, tu `1` den `100`.

Response:

```json
{
  "userPageSize": 12,
  "adminPageSize": 30
}
```

Loi thuong gap:

- `403 Forbidden`: token khong phai admin/shop.
- `400 Bad Request`: thieu field hoac page size ngoai khoang `1..100`.

## API List Product

Ap dung cho 3 API public hien co:

```http
GET /api/v1/product/get-products
GET /api/v1/product/get-products-by-category?categoryId=<categoryId>
GET /api/v1/product/get-products-by-search?search=<keyword>
```

Query params:

- `site`: optional, nhan `USER` hoac `ADMIN`, default `USER`.
- `page`: so page, bat dau tu `0`.
- `size`: optional, neu truyen thi override config.
- `sort`: optional theo format Spring Pageable, vi du `sort=name,asc`.

## Cach Goi Cho User Site

User site nen truyen ro `site=USER`.

```http
GET /api/v1/product/get-products?site=USER&page=0
```

Theo category:

```http
GET /api/v1/product/get-products-by-category?site=USER&categoryId=1&page=0
```

Theo search:

```http
GET /api/v1/product/get-products-by-search?site=USER&search=latte&page=0
```

Neu user site can override page size tam thoi:

```http
GET /api/v1/product/get-products?site=USER&page=0&size=16
```

## Cach Goi Cho Admin Site

Admin site phai truyen `site=ADMIN` tren URL de backend dung `adminPageSize`.

Khong can gui token cho API list product neu chi can list public.

```http
GET /api/v1/product/get-products?site=ADMIN&page=0
```

Theo category:

```http
GET /api/v1/product/get-products-by-category?site=ADMIN&categoryId=1&page=0
```

Theo search:

```http
GET /api/v1/product/get-products-by-search?site=ADMIN&search=latte&page=0
```

Neu admin UI co selector page size rieng va muon override config:

```http
GET /api/v1/product/get-products?site=ADMIN&page=0&size=50
```

## Flow Goi Y Cho FE

User site:

1. Load danh sach san pham bang `site=USER&page=<page>`, khong can truyen `size`.
2. Render theo response `Page` cua Spring: `content`, `number`, `size`, `totalElements`, `totalPages`.
3. Khi user doi page, goi lai voi `page=<nextPage>`.

Admin site:

1. Man config goi `GET /api/v1/product/page-config` voi admin token.
2. Hien thi 2 input: `userPageSize` va `adminPageSize`.
3. Khi save, goi `POST /api/v1/product/page-config` voi admin token.
4. Bang product admin goi list voi `site=ADMIN&page=<page>`, khong can truyen `size` neu muon dung config.

Vi du update config:

```ts
await fetch('/api/v1/product/page-config', {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    userPageSize: 12,
    adminPageSize: 30,
  }),
});
```

Vi du list admin products:

```ts
const res = await fetch(`/api/v1/product/get-products?site=ADMIN&page=${page}`);
const data = await res.json();
```

Vi du list user products:

```ts
const res = await fetch(`/api/v1/product/get-products?site=USER&page=${page}`);
const data = await res.json();
```

## Luu Y

- API list product van la public API.
- Admin site khong nen dua vao token de backend nhan dien page size nua; hay truyen `site=ADMIN`.
- Neu FE truyen `size`, backend luon uu tien `size`.
- Sau khi update config thanh cong, admin FE nen reload list tu `page=0`.
- Backend dung page index bat dau tu `0`; neu UI hien thi page tu `1`, FE can convert truoc khi goi API.
