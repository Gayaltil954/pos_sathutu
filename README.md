# POS System - Point of Sale Desktop Application

Current version: 1.3.7

A modern, production-ready Point of Sale (POS) system built with **Spring Boot** backend and **JavaFX** frontend, integrated with MongoDB database.

## 🎯 Features

### 1. Product Management
- ✅ Add, edit, delete products
- ✅ Category management (21 predefined categories)
- ✅ QR code generation and scanning
- ✅ Product search and filtering
- ✅ Stock management

### 2. Cart & Billing
- ✅ Real-time cart management
- ✅ Add/remove items
- ✅ Auto-calculated totals
- ✅ Discount support (integer values)
- ✅ PDF bill generation
- ✅ Printable receipts

### 3. QR Code Integration
- ✅ QR code generation for products
- ✅ Webcam scanner support
- ✅ Automatic product lookup by QR code
- ✅ One-click cart addition

### 4. Sales Tracking
- ✅ Real-time transaction recording
- ✅ Sales history management
- ✅ Payment method tracking
- ✅ Transaction notes

### 5. Reports & Analytics
- ✅ Daily sales summary (total sales, total discount, net total, transactions)
- ✅ Monthly revenue reports
- ✅ Most sold products
- ✅ Category-wise sales breakdown

### 6. Modern UI
- ✅ Clean, intuitive dashboard
- ✅ Responsive design
- ✅ Dark theme navigation
- ✅ Keyboard shortcuts support
- ✅ Professional styling

## 🏗️ System Architecture

```
┌─────────────────┐
│  JavaFX UI      │
│  (Frontend)     │
└────────┬────────┘
         │ REST APIs
┌────────▼─────────────────┐
│  Spring Boot Backend      │
│  (Controllers/Services)   │
└────────┬─────────────────┘
         │ MongoDB Driver
┌────────▼──────────┐
│  MongoDB Database │
└───────────────────┘
```

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8.0 or higher
- MongoDB 4.4 or higher (local or cloud)
- JavaFX 21.0.1

## 🚀 Quick Start

### Step 1: Setup MongoDB

**Option A: Local MongoDB**
```bash
# Windows users: Download from https://www.mongodb.com/try/download/community
# Start MongoDB service
mongod
```

**Option B: Cloud MongoDB (Atlas)**
1. Create account at https://www.mongodb.com/cloud/atlas
2. Update `application.properties` in backend:
```properties
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority
```

### Step 2: Build Backend

```bash
cd pos-backend
mvn clean install
mvn spring-boot:run
```

The backend will start at `http://localhost:8080`

### Step 3: Initialize Categories

```bash
curl -X POST http://localhost:8080/api/categories/initialize
```

### Step 4: Build & Run Frontend

```bash
cd pos-frontend
mvn clean install
mvn javafx:run
```

## 📁 Project Structure

```
pos-backend/
├── src/main/java/com/posystem/
│   ├── model/              # JPA Entities (Product, Category, Sale)
│   ├── repository/         # MongoDB Repositories
│   ├── service/            # Business Logic
│   ├── controller/         # REST Controllers
│   ├── dto/               # Data Transfer Objects
│   ├── config/            # Configuration
│   └── PosApplication.java # Main Class
├── src/main/resources/
│   └── application.properties
└── pom.xml

pos-frontend/
├── src/main/java/com/posystem/fx/
│   ├── controller/        # JavaFX Controllers
│   ├── service/          # API Service Client
│   ├── dto/              # DTOs
│   └── PosFxApplication.java
├── src/main/resources/
│   ├── fxml/             # FXML UI Files
│   └── css/              # Stylesheets
└── pom.xml
```

## 🔌 API Endpoints

### Products
```
GET    /api/products                      # Get all products
GET    /api/products/{id}                 # Get product by ID
POST   /api/products                      # Add new product
PUT    /api/products/{id}                 # Update product
DELETE /api/products/{id}                 # Delete product
GET    /api/products/category/{category}  # Get by category
GET    /api/products/search?term={term}   # Search products
GET    /api/products/qrcode/{qrCode}      # Get by QR code
```

### Categories
```
GET    /api/categories                    # Get all categories
GET    /api/categories/{id}               # Get by ID
POST   /api/categories                    # Add category
PUT    /api/categories/{id}               # Update category
DELETE /api/categories/{id}               # Delete category
POST   /api/categories/initialize         # Initialize defaults
```

### Sales
```
GET    /api/sales                         # Get all sales
GET    /api/sales/{id}                    # Get sale by ID
POST   /api/sales                         # Record new sale
GET    /api/sales/summary/daily?date=...  # Daily summary
GET    /api/sales/summary/monthly?month=.# Monthly summary
GET    /api/sales/bill/pdf/{id}           # Download bill PDF
GET    /api/sales/bill/text/{id}          # Get bill text
GET    /api/sales/date-range?start=...&end=... # Date range
```

### QR Code
```
POST   /api/qrcode/generate?data=...     # Generate QR code
GET    /api/qrcode/generate?data=...     # Get QR image
```

## 📊 Database Schema

### Products Collection
```json
{
  "_id": "ObjectId",
  "name": "Wireless Mouse",
  "category": "Mouse",
  "price": 2500,
  "qrCode": "PROD123456",
  "stock": 20,
  "description": "Comfortable wireless mouse",
  "createdAt": "2026-03-20T10:00:00",
  "updatedAt": "2026-03-20T10:00:00",
  "active": true
}
```

### Categories Collection
```json
{
  "_id": "ObjectId",
  "name": "Mouse",
  "description": "Computer mouse and peripherals",
  "createdAt": "2026-03-20T10:00:00",
  "active": true
}
```

### Sales Collection
```json
{
  "_id": "ObjectId",
  "items": [
    {
      "productId": "...",
      "productName": "Mouse",
      "category": "Mouse",
      "quantity": 2,
      "price": 2500,
      "itemTotal": 5000
    }
  ],
  "subtotal": 5000,
  "discount": 500,
  "finalTotal": 4500,
  "date": "2026-03-20T10:45:00",
  "paymentMethod": "Cash",
  "notes": ""
}
```

## 🎮 Usage

### POS Screen (Main Selling Screen)
1. **Select Product**: Click on any product button to add to cart
2. **Filter by Category**: Use category dropdown
3. **Search**: Type in search bar to find products
4. **Adjust Quantity**: Select item in cart and click "Update Qty"
5. **Apply Discount**: Enter discount amount (integer only)
6. **Checkout**: Review total and click "Checkout" button

### Product Management
1. Go to **Products** tab
2. Fill product details
3. Click **Add Product**
4. Search or manage existing products

### Billing
- Bill is automatically generated on checkout
- View bill: `/api/sales/bill/pdf/{saleId}`
- Print from browser or use print button

### Reports
1. **Daily Summary**: Select date and view total sales, discounts, transactions
2. **Monthly Summary**: Select month and view revenue breakdown, category-wise sales

## 🔧 Configuration

### Backend Configuration (`application.properties`)
```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/pos_db
spring.data.mongodb.database=pos_db

# Server
server.port=8080
server.servlet.context-path=/

# Logging
logging.level.root=INFO
logging.level.com.posystem=DEBUG
```

### Frontend Configuration
```properties
# API Configuration
api.base-url=http://localhost:8080/api
```

## 🔐 Security Features

- CORS enabled for API protection
- Input validation on all endpoints
- Error handling with meaningful messages
- Database constraints
- Transaction integrity

## 📝 Category List

Default categories:
- Chargers
- Backcovers
- Handsfree
- Tempered Glass
- Battery
- OTG
- Chip Reader
- Phones
- Speakers
- Mouse
- Keyboard
- Powerbank
- Router
- Dongle
- Phone Cable
- Earbuds
- Earbuds Covers
- Charging Dock
- Smartwatch
- Pen
- Chip

## 📱 System Requirements

| Component | Requirement |
|-----------|-------------|
| Java | 17+ |
| JavaFX | 21.0.1+ |
| MongoDB | 4.4+ |
| RAM | 2GB minimum |
| Storage | 500MB minimum |
| OS | Windows/Mac/Linux |

## 🐛 Troubleshooting

### MongoDB Connection Error
```
Error: Unable to connect to MongoDB
```
**Solution**: Ensure MongoDB is running on localhost:27017 or update connection string.

### Port Already in Use
```
Error: Address already in use: 8080
```
**Solution**: Change server.port in application.properties

### JavaFX Not Loading
```
Error: Found an item in CLASSPATH that cannot be represented as a File
```
**Solution**: Ensure JavaFX SDK is properly configured in IDE.

## 🚀 Building for Production

### Backend
```bash
cd pos-backend
mvn clean package -Pprod
# Generate JAR in target/
```

### Frontend
```bash
cd pos-frontend
mvn clean package
```

## 📄 License

This project is provided as-is for educational and commercial use.

## 👨‍💼 Support

For issues or questions:
1. Check the troubleshooting section
2. Review MongoDB connection strings
3. Verify Java and JavaFX versions
4. Check API endpoints with curl

## 🎯 Future Enhancements

- [x] Inventory alerts
- [x] Editable sales records
- [x] Auto stock synchronization on sale create/edit
- [x] App version/update notification with changelog
- [ ] User authentication
- [ ] Multiple payment methods
- [ ] Barcode scanning support
- [ ] Mobile app
- [ ] Cloud synchronization
- [ ] Advanced analytics
- [ ] Customer management

---

**Version**: 1.3.7  
**Last Updated**: April 11, 2026
