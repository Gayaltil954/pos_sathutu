# API Documentation - POS System

Complete REST API documentation with examples.

## Base URL
```
http://localhost:8080/api
```

## Response Format

All responses follow this format:

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* payload */ },
  "timestamp": "2026-03-20T10:30:45"
}
```

### Error Response (400/500)
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2026-03-20T10:30:45"
}
```

---

## PRODUCT ENDPOINTS

### 1. Get All Products
```http
GET /api/products
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Products fetched successfully",
  "data": [
    {
      "id": "507f1f77bcf86cd799439011",
      "name": "Wireless Mouse",
      "category": "Mouse",
      "price": 2500,
      "qrCode": "MOUSE001",
      "stock": 50,
      "description": "Comfortable wireless mouse",
      "active": true
    }
  ]
}
```

### 2. Get Product by ID
```http
GET /api/products/{id}
```

**Parameters:**
- `id` (path): Product ID

**Example:**
```bash
curl http://localhost:8080/api/products/507f1f77bcf86cd799439011
```

### 3. Get Product by QR Code
```http
GET /api/products/qrcode/{qrCode}
```

**Parameters:**
- `qrCode` (path): QR code value

**Example:**
```bash
curl http://localhost:8080/api/products/qrcode/MOUSE001
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Product found",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "name": "Wireless Mouse",
    "category": "Mouse",
    "price": 2500,
    "qrCode": "MOUSE001",
    "stock": 50,
    "description": "Comfortable wireless mouse",
    "active": true
  }
}
```

### 4. Get Products by Category
```http
GET /api/products/category/{category}
```

**Parameters:**
- `category` (path): Category name

**Example:**
```bash
curl http://localhost:8080/api/products/category/Mouse
```

### 5. Search Products
```http
GET /api/products/search?term={searchTerm}
```

**Parameters:**
- `term` (query): Search keyword

**Example:**
```bash
curl http://localhost:8080/api/products/search?term=Mouse
```

### 6. Advanced Search
```http
GET /api/products/search/advanced?name={name}&category={category}
```

**Parameters:**
- `name` (query, optional): Product name
- `category` (query, optional): Category name

**Example:**
```bash
curl "http://localhost:8080/api/products/search/advanced?name=Mouse&category=Mouse"
```

### 7. Add New Product
```http
POST /api/products
Content-Type: application/json

{
  "name": "Wireless Mouse",
  "category": "Mouse",
  "price": 2500,
  "qrCode": "MOUSE001",
  "stock": 50,
  "description": "Comfortable wireless mouse"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "category": "Mouse",
    "price": 2500,
    "qrCode": "MOUSE001",
    "stock": 50,
    "description": "Comfortable wireless mouse"
  }'
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Product added successfully",
  "data": {
    "id": "507f1f77bcf86cd799439011",
    "name": "Wireless Mouse",
    "category": "Mouse",
    "price": 2500,
    "qrCode": "MOUSE001",
    "stock": 50,
    "description": "Comfortable wireless mouse",
    "active": true,
    "createdAt": "2026-03-20T10:30:45",
    "updatedAt": "2026-03-20T10:30:45"
  }
}
```

### 8. Update Product
```http
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "price": 3000
}
```

**Example:**
```bash
curl -X PUT http://localhost:8080/api/products/507f1f77bcf86cd799439011 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 3000,
    "stock": 45
  }'
```

### 9. Delete Product
```http
DELETE /api/products/{id}
```

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/products/507f1f77bcf86cd799439011
```

---

## CATEGORY ENDPOINTS

### 1. Get All Categories
```http
GET /api/categories
```

### 2. Initialize Default Categories
```http
POST /api/categories/initialize
```

**Important!** Run this once to populate default categories:
```bash
curl -X POST http://localhost:8080/api/categories/initialize
```

### 3. Get Category by ID
```http
GET /api/categories/{id}
```

### 4. Get Category by Name
```http
GET /api/categories/name/{name}
```

**Example:**
```bash
curl http://localhost:8080/api/categories/name/Mouse
```

### 5. Add New Category
```http
POST /api/categories
Content-Type: application/json

{
  "name": "New Category",
  "description": "Category description"
}
```

### 6. Update Category
```http
PUT /api/categories/{id}
Content-Type: application/json

{
  "description": "Updated description"
}
```

### 7. Delete Category
```http
DELETE /api/categories/{id}
```

---

## SALE ENDPOINTS

### 1. Get All Sales
```http
GET /api/sales
```

### 2. Get Sale by ID
```http
GET /api/sales/{id}
```

### 3. Record New Sale
```http
POST /api/sales
Content-Type: application/json

{
  "items": [
    {
      "productId": "507f1f77bcf86cd799439011",
      "productName": "Wireless Mouse",
      "category": "Mouse",
      "quantity": 2,
      "price": 2500,
      "itemTotal": 5000
    }
  ],
  "subtotal": 5000,
  "discount": 500,
  "finalTotal": 4500,
  "paymentMethod": "Cash",
  "notes": "Regular customer"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "507f1f77bcf86cd799439011",
        "productName": "Wireless Mouse",
        "category": "Mouse",
        "quantity": 2,
        "price": 2500,
        "itemTotal": 5000
      }
    ],
    "subtotal": 5000,
    "discount": 500,
    "finalTotal": 4500,
    "paymentMethod": "Cash"
  }'
```

### 4. Get Daily Summary
```http
GET /api/sales/summary/daily?date={YYYY-MM-DD}
```

**Example:**
```bash
curl "http://localhost:8080/api/sales/summary/daily?date=2026-03-20"
```

**Response:**
```json
{
  "success": true,
  "message": "Daily summary fetched",
  "data": {
    "totalSales": 25000,
    "totalDiscount": 2500,
    "numberOfTransactions": 10,
    "date": "2026-03-20"
  }
}
```

### 5. Get Monthly Summary
```http
GET /api/sales/summary/monthly?month={YYYY-MM}
```

**Example:**
```bash
curl "http://localhost:8080/api/sales/summary/monthly?month=2026-03"
```

**Response:**
```json
{
  "success": true,
  "message": "Monthly summary fetched",
  "data": {
    "totalRevenue": 500000,
    "totalTransactions": 200,
    "totalDiscount": 25000,
    "mostSoldProducts": {
      "Wireless Mouse": 150,
      "USB Cable": 200
    },
    "categoryWiseSales": {
      "Mouse": 150000,
      "Cables": 100000
    },
    "month": "2026-03"
  }
}
```

### 6. Get Sales by Date Range
```http
GET /api/sales/date-range?startDate={YYYY-MM-DD}&endDate={YYYY-MM-DD}
```

**Example:**
```bash
curl "http://localhost:8080/api/sales/date-range?startDate=2026-03-01&endDate=2026-03-31"
```

### 7. Get Bill as PDF
```http
GET /api/sales/bill/pdf/{id}
```

**Example:**
```bash
curl http://localhost:8080/api/sales/bill/pdf/507f1f77bcf86cd799439011 \
  -o bill.pdf
```

### 8. Get Bill as Text
```http
GET /api/sales/bill/text/{id}
```

**Response:**
```json
{
  "success": true,
  "message": "Bill generated",
  "data": "----------------------------\n   SATHUTU MOBILE SHOP\n----------------------------\nDate: 2026-03-20\nTime: 10:45 AM\n\nItems:\nMouse x2      5000\n\nSubtotal:     5000\nDiscount:     500\n----------------------------\nTotal:        4500\n----------------------------\nThank You!\n"
}
```

---

## QR CODE ENDPOINTS

### 1. Generate QR Code
```http
GET /api/qrcode/generate?data={data}
```

**Parameters:**
- `data` (query): Data to encode in QR code

**Example:**
```bash
curl http://localhost:8080/api/qrcode/generate?data=PRODUCT123 \
  -o product_qr.png
```

Returns: PNG image of QR code

---

## Error Codes

| Code | Message | Solution |
|------|---------|----------|
| 200 | Success | Operation completed |
| 400 | Bad Request | Invalid parameters |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Database or server issue |

---

## Common Use Cases

### 1. Complete Sales Flow

```bash
# 1. Get all products
curl http://localhost:8080/api/products

# 2. Get product by QR code (from scanner)
curl http://localhost:8080/api/products/qrcode/MOUSE001

# 3. Record sale
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{"items": [...], "subtotal": 5000, "discount": 500, "finalTotal": 4500}'

# 4. Get bill PDF
curl http://localhost:8080/api/sales/bill/pdf/{saleId} -o bill.pdf
```

### 2. Daily Closing

```bash
# Get daily summary
curl http://localhost:8080/api/sales/summary/daily?date=2026-03-20

# Get all sales for the day
curl "http://localhost:8080/api/sales/date-range?startDate=2026-03-20&endDate=2026-03-20"
```

### 3. Monthly Report

```bash
curl http://localhost:8080/api/sales/summary/monthly?month=2026-03
```

---

**API Documentation v1.0**  
Last Updated: March 20, 2026
