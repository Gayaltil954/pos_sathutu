# Setup Guide - POS System

Complete step-by-step setup guide for the POS System.

## Release Version

- Current setup target: 1.3.7
- Includes editable sales, low stock alerts, automatic stock sync, and in-app update notifications

## Prerequisites Check

```bash
# Check Java version (should be 17+)
java -version

# Check Maven version (should be 3.8.0+)
mvn -version
```

## Step 1: MongoDB Setup

### Option A: Windows Local Install

1. Download MongoDB Community Edition from https://www.mongodb.com/try/download/community
2. Run the installer
3. Choose "Install MongoDB as a Service"
4. MongoDB will automatically start as Windows Service

**To verify MongoDB is running:**
```bash
# Open PowerShell
mongosh
# You should see the MongoDB shell prompt
```

### Option B: MongoDB Atlas (Cloud)

1. Go to https://www.mongodb.com/cloud/atlas
2. Create a free account
3. Create a cluster (select AWS, free tier)
4. Network Access: Allow 0.0.0.0/0
5. Create Database User
6. Copy connection string
7. Update in `pos-backend/src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/pos_db?retryWrites=true&w=majority
```

## Step 2: Backend Setup

### 2.1 Navigate to Backend Directory
```bash
cd pos-backend
```

### 2.2 Build the Project
```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile the code
- Run tests
- Package the application

### 2.3 Run the Backend Server
```bash
mvn spring-boot:run
```

**Expected output:**
```
Started PosApplication in 5.123 seconds
Server is running on http://localhost:8080
```

### 2.4 Initialize Categories
Open another terminal and run:
```bash
curl -X POST http://localhost:8080/api/categories/initialize
```

## Step 3: Frontend Setup

### 3.1 Open New Terminal and Navigate to Frontend
```bash
cd pos-frontend
```

### 3.2 Build Frontend
```bash
mvn clean install
```

### 3.3 Run JavaFX Application
```bash
mvn javafx:run
```

**The POS application window will open!**

## Step 4: Create Sample Products

You can populate sample products via API:

```bash
# Create a product
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

Or use the **Products** tab in the app to add products manually.

## Build Windows One-Click Installer (.exe)

Use this when you want client PCs to open POS by double-clicking a desktop icon (no Maven commands on client machine).

### Prerequisites for Installer Build

```bash
java -version
jpackage --version
```

`jpackage` is included with JDK 17+.

### Build the Installer

Optional (for custom app icon):

1. Put your icon file as `assets/app-icon.ico` (recommended), or `app-icon.ico` in project root
2. Run build script

From project root (Windows):

```bash
build-installer.bat
```

This script will:
- Build backend and frontend jars
- Copy frontend runtime dependencies
- Bundle backend jar into installer input
- Use custom icon automatically when `assets/app-icon.ico` (or `app-icon.ico`) exists
- Create Windows `.exe` installer when WiX is installed
- Automatically fall back to portable `app-image` when WiX is not available

Output folder:

```bash
dist/installer
```

### EXE Installer Requirement (WiX)

`jpackage --type exe` on Windows needs WiX tools (`candle.exe` and `light.exe`).

Install (run terminal as Administrator):

```bash
winget install --id WiXToolset.WiXToolset --accept-package-agreements --accept-source-agreements
```

### Client Installation & Run

1. Send the generated `.exe` installer from `dist/installer` to client PC
2. Install normally (Next > Next > Finish)
3. Use desktop shortcut **POS System**
4. Double-click shortcut to open app

The frontend now auto-starts bundled backend jar when backend is not already running.

### Updating Existing Local Installations

If a laptop already has POS System installed locally, the new 1.3.7 installer will upgrade it in place.

Steps:

1. Close POS System on the client laptop
2. Run the new installer generated from `dist/installer`
3. Complete setup (same install location recommended)
4. Open POS System again

Notes:

- Existing local app is updated (not installed as a separate duplicate app)
- Existing MongoDB data remains unchanged
- Desktop/start menu shortcuts continue to work after update

### Database Requirement

MongoDB must still be available:
- Local MongoDB service on client PC, or
- MongoDB Atlas URI configured in backend `application.properties`

## Troubleshooting

### MongoDB Connection Failed

**Problem**: `MongoSocketOpenException: Exception opening socket`

**Solution**:
1. Ensure MongoDB is running: `mongosh`
2. If using Atlas, check:
   - Connection string is correct
   - Network access allows your IP
   - Username and password are correct
3. Check `application.properties` URL

### Port 8080 Already in Use

**Problem**: `Address already in use: 8080`

**Solution**:
1. Change port in `application.properties`:
```properties
server.port=8081
```

2. Kill the process using port 8080:
```bash
# Windows (PowerShell Admin)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :8080
kill -9 <PID>
```

### JavaFX Module Not Found

**Problem**: `module javafx.controls not found`

**Solution**:
1. Ensure JavaFX SDK is installed
2. In IDE, add JavaFX to project configuration:
   - IntelliJ: File > Project Structure > Libraries > Add JavaFX SDK
   - Eclipse: Project > Configure Build Path > Add External JARs

### Frontend Can't Connect to Backend

**Problem**: "Failed to load products" error

**Solution**:
1. Ensure backend is running on port 8080
2. Check `application.properties` in frontend:
```properties
api.base-url=http://localhost:8080/api
```

3. Verify backend is accessible:
```bash
curl http://localhost:8080/api/products
```

## Development Mode

### Terminal Setup (Windows PowerShell)

**Terminal 1 - Start MongoDB:**
```bash
# If installed as service, it auto-starts
# Or manually: mongod

# Verify connection
mongosh
```

**Terminal 2 - Start Backend:**
```bash
cd pos-backend
mvn spring-boot:run
```

**Terminal 3 - Start Frontend:**
```bash
cd pos-frontend
mvn javafx:run
```

## IDE Setup

### IntelliJ IDEA

1. **Open Project**:
   - File > Open > Select `pos-backend` folder
   - Mark `pos-frontend` folder as module

2. **Configure SDK**:
   - File > Project Structure > Project
   - Set Project SDK to Java 17+

3. **Add JavaFX Library**:
   - File > Project Structure > Libraries
   - Click `+` > Download From Maven
   - Type: `javafx:javafx-controls`

4. **Run Configurations**:
   - Backend: Run > Spring Boot App
   - Frontend: Run > Application (PosFxApplication.main())

### Eclipse

1. **Import Projects**:
   - File > Import > Existing Maven Projects
   - Navigate to pos-backend, click Finish
   - Repeat for pos-frontend

2. **Add JavaFX**:
   - Right-click project > Build Path > Configure Build Path
   - Add External JARs from JavaFX SDK lib folder

3. **Run**:
   - Right-click > Run As > Spring Boot App
   - Right-click > Run As > Java Application

## Testing the Application

### Test Endpoints with cURL

```bash
# Get all products
curl http://localhost:8080/api/products

# Get all categories
curl http://localhost:8080/api/categories

# Create a sale
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": "1", "productName": "Mouse", "quantity": 2, "price": 2500, "itemTotal": 5000}
    ],
    "subtotal": 5000,
    "discount": 500,
    "finalTotal": 4500,
    "paymentMethod": "Cash"
  }'
```

### Test UI Features

1. **Add Product to Cart**: Click product buttons on POS Screen
2. **View Cart**: Items appear in right panel
3. **Apply Discount**: Enter value in discount field
4. **Checkout**: Click "Checkout" button
5. **View Reports**: Click "Reports" tab

## Performance Optimization

### For Large Datasets

1. Add Database Indexes:
```bash
mongosh
use pos_db
db.products.createIndex({ "name": 1 })
db.products.createIndex({ "category": 1 })
db.sales.createIndex({ "date": -1 })
```

2. Pagination in API (future feature):
```properties
# Add to application.properties
spring.data.mongodb.auto-index-creation=true
```

## Next Steps

1. ✅ Verify both backend and frontend are running
2. ✅ Create sample products
3. ✅ Test POS functionality
4. ✅ Review generated bills
5. ✅ Check daily/monthly reports

For more details, see [README.md](./README.md)

---

**Setup Complete! Enjoy using POS System!** 🎉
