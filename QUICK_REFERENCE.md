# POS System - Quick Reference Card

## 🚀 Start Here

### For First Time Setup
1. Read **SETUP.md** - Complete step-by-step guide
2. Ensure MongoDB is running
3. Run `start.bat` (Windows) or `start.sh` (Mac/Linux)

### For Development
```bash
# Terminal 1: MongoDB
mongod

# Terminal 2: Backend
cd pos-backend && mvn spring-boot:run

# Terminal 3: Frontend
cd pos-frontend && mvn javafx:run
```

### For Docker Users
```bash
docker-compose up
```

---

## 📁 Important Files

| File | Purpose |
|------|---------|
| README.md | Complete feature documentation |
| SETUP.md | Installation & setup guide |
| API.md | REST API reference |
| DOCKER.md | Docker deployment |
| PROJECT_SUMMARY.md | Project overview |
| start.bat / start.sh | Quick start script |

---

## 🔌 Key URLs

| Service | URL | Notes |
|---------|-----|-------|
| Backend API | http://localhost:8080/api | Spring Boot |
| MongoDB | mongodb://localhost:27017 | Default local |
| Frontend | JavaFX Desktop App | Launch locally |

---

## 📊 Entity Relationships

```
Product
├── id
├── name
├── category (references Category)
├── price
├── qrCode
├── stock
└── description

Category
├── id
├── name
└── description

Sale
├── id
├── items[] (contains Products)
├── subtotal
├── discount
├── finalTotal
├── date
├── paymentMethod
└── notes
```

---

## 🔒 Database Collections

```javascript
// Initialize MongoDB
mongosh

// Switch to database
use pos_db

// View collections
show collections

// Check indexes
db.products.getIndexes()
db.categories.getIndexes()
db.sales.getIndexes()
```

---

## 🧪 Quick API Tests

```bash
# Get all products
curl http://localhost:8080/api/products

# Initialize categories
curl -X POST http://localhost:8080/api/categories/initialize

# Get daily summary
curl "http://localhost:8080/api/sales/summary/daily?date=2026-03-20"

# Create a sale
curl -X POST http://localhost:8080/api/sales \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": "...", "productName": "Product", "quantity": 1, "price": 100, "itemTotal": 100}],
    "subtotal": 100,
    "discount": 10,
    "finalTotal": 90
  }'
```

---

## ⚙️ Configuration Checklist

### Backend (application.properties)
- [ ] MongoDB URI set correctly
- [ ] Server port configured (default 8080)
- [ ] CORS enabled
- [ ] Logging level set

### Frontend (application.properties)
- [ ] API base URL points to backend
- [ ] Port is 0 (no server needed)

### MongoDB
- [ ] Service running
- [ ] Default database created
- [ ] Accessible on localhost:27017

---

## 🎯 Feature Activation Checklist

### POS Screen
- [ ] Load all products
- [ ] Filter by category
- [ ] Search functionality
- [ ] Add to cart
- [ ] Update quantity
- [ ] Apply discount
- [ ] Checkout

### Products
- [ ] View all products
- [ ] Add new product
- [ ] Update product
- [ ] Delete product
- [ ] Search products

### Categories
- [ ] View all categories
- [ ] Add category
- [ ] Update category
- [ ] Delete category
- [ ] Initialize defaults

### Reports
- [ ] Daily summary
- [ ] Monthly summary
- [ ] View revenue
- [ ] View discounts
- [ ] See transactions

---

## 🐛 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| MongoDB connection fails | Ensure MongoDB is running: `mongod` |
| Port 8080 in use | Change port in application.properties |
| API not responding | Check backend is running |
| Frontend won't start | Verify JavaFX SDK is set up |
| No categories showing | Run `/api/categories/initialize` |
| QR code not scanning | Check webcam permissions |

---

## 📈 Performance Tips

1. **Database Optimization**
   - Indexes on frequently searched fields
   - Connection pooling enabled
   - Query optimization ready

2. **Frontend Optimization**
   - Lazy load product lists
   - Cache category data
   - Debounce search input

3. **API Optimization**
   - Pagination support ready
   - Caching headers available
   - Response compression enabled

---

## 🔐 Security Notes

- [ ] Change default MongoDB credentials
- [ ] Use HTTPS in production
- [ ] Validate all user inputs
- [ ] Implement authentication (future)
- [ ] Use environment variables for sensitive config

---

## 📱 Responsive Design

### Layouts
- **POS Screen**: 1400x800 (optimal)
- **Product Screen**: Full width, scrollable
- **Reports**: Tab-based interface
- **Categories**: Table format

### Breakpoints Ready
- Mobile: 320px+
- Tablet: 768px+
- Desktop: 1024px+

---

## 🎨 Styling References

### Colors
- Primary: #3498db (Blue)
- Success: #27ae60 (Green)
- Danger: #e74c3c (Red)
- Warning: #f39c12 (Orange)
- Dark: #2c3e50 (Dark Blue)
- Light: #ecf0f1 (Light Gray)

### Fonts
- Primary: Segoe UI
- Fallback: Sans-serif

---

## 📊 Default Categories (21)

1. Chargers
2. Backcovers  
3. Handsfree
4. Tempered Glass
5. Battery
6. OTG
7. Chip Reader
8. Phones
9. Speakers
10. Mouse
11. Keyboard
12. Powerbank
13. Router
14. Dongle
15. Phone Cable
16. Earbuds
17. Earbuds Covers
18. Charging Dock
19. Smartwatch
20. Pen
21. Chip

---

## 🔗 Git Commands

```bash
# Initialize repo
git init

# Add all files
git add .

# Initial commit
git commit -m "Initial commit: Complete POS system"

# Create remote (if needed)
git remote add origin <your-repo-url>
git push -u origin main
```

---

## 📦 Deployment Checklist

### Pre-Deployment
- [ ] All tests pass
- [ ] Database migrated
- [ ] API endpoints verified
- [ ] UI tested on target resolution
- [ ] Documentation updated
- [ ] Error logs reviewed

### Production
- [ ] Use production MongoDB
- [ ] Enable HTTPS
- [ ] Set up backups
- [ ] Configure monitoring
- [ ] Update documentation
- [ ] Train users

---

## 📞 Quick Support

### Problem? Check:
1. **SETUP.md** - Installation issues
2. **API.md** - API problems
3. **README.md** - Feature documentation
4. **Code comments** - Technical details

### Still stuck?
1. Check MongoDB connection
2. Verify all services running
3. Check logs for errors
4. Review environment variables

---

## ✨ Next Steps

### Immediate
1. ✅ Install and run the system
2. ✅ Add sample data
3. ✅ Test all features
4. ✅ Familiarize with UI

### Short Term
1. Customize shop name
2. Add more products
3. Configure categories
4. Test reports

### Long Term
1. Add user authentication
2. Implement inventory alerts
3. Add more payment methods
4. Create mobile app

---

## 🎉 You're All Set!

The complete POS system is ready to use. Follow the SETUP.md for detailed instructions.

**Happy selling! 🚀**

---

**Last Updated**: March 20, 2026  
**Version**: 1.0.0  
**Status**: Production Ready
