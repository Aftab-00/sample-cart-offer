# Cart Offer Application - Test Automation Suite

A Spring Boot application that handles cart discounts for Zomato's customer segments with comprehensive API test automation.

## What This Project Does

This application manages discount offers for different customer segments (p1, p2, p3) in a restaurant ordering system. Think of it like this: when you place an order on Zomato, the system checks your customer segment and applies the best available discount automatically.

### Offer Types Supported
- **FLATX**: Fixed amount discount (e.g., ₹10 off)
- **PERCENTAGE**: Percentage-based discount (e.g., 15% off)

### How It Works
1. Restaurants add offers for specific customer segments
2. When a customer applies an offer, the system:
   - Checks the user's segment (p1, p2, or p3)
   - Finds all applicable offers for that segment and restaurant
   - Applies the best offer (highest discount)
   - Returns the final cart value

## Test Automation Overview

We've built a comprehensive test suite with 20 test cases covering:
- **Positive scenarios**: Basic discount calculations, edge cases
- **Negative scenarios**: Invalid inputs, missing parameters
- **Security tests**: Segment access control
- **Performance tests**: Concurrent request handling

All tests are written using RestAssured and follow real-world QA practices - when we find bugs, we report them instead of hiding them.

## Prerequisites

- **Java 17** (required for Spring Boot 2.7.18)
- **Maven 3.9+** 
- **Docker Desktop** (for mock server)
- **Docker Compose**

## Quick Start Guide

### 1. Start Docker Services
```bash
# Make sure Docker Desktop is running first
cd mockserver  
docker-compose up -d
```

### 2. Build the Application
```bash
# Set up Java 17 environment
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export MAVEN_OPTS="--add-opens jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED"

# Build the project
./mvnw clean install -DskipTests
```

### 3. Start the Main Application
```bash
./mvnw spring-boot:run
```
The application starts on `http://localhost:9001`

### 4. Run the Test Suite
```bash
# Run all 20 test cases
./mvnw test -Dtest=CartOfferApplicationTests

# Check if services are healthy before testing
./check-services.sh
```

## API Endpoints

### Add Offer
```bash
POST /api/v1/offer
{
  "restaurant_id": 1,
  "offer_type": "FLATX",
  "offer_value": 10,
  "customer_segment": ["p1"]
}
```

### Apply Offer to Cart
```bash
POST /api/v1/cart/apply_offer
{
  "cart_value": 200,
  "user_id": 1,
  "restaurant_id": 1
}
```

### Get User Segment (Mocked)
```bash
GET /api/v1/user_segment?user_id=1
```

## Test Cases Breakdown

### Positive Test Cases (TC001-TC007)
- Basic FLATX and PERCENTAGE discount calculations
- Best offer selection when multiple offers exist
- Edge cases like cart value equal to discount amount
- No offers available scenario

### Negative Test Cases (TC008-TC011)  
- Invalid user IDs and restaurant IDs
- Negative cart values
- Missing required parameters

### Edge Cases (TC012-TC018)
- Discount amounts exceeding cart value
- Over 100% percentage discounts
- Zero and very small cart values
- Large transaction amounts
- Floating point precision handling

### Special Cases (TC019-TC020)
- Concurrent request handling
- Cross-segment access control

## Known Issues Found by Tests

Our QA process has identified several API bugs that need developer attention:

1. **Best Offer Selection Bug (TC003)**: System doesn't pick the best offer when multiple offers exist
2. **Input Validation Missing**: API accepts invalid user IDs, restaurant IDs, and negative cart values
3. **Negative Cart Values**: System allows cart values to go negative instead of capping at 0

These are real bugs that should be fixed in the API code, not worked around in tests.

## Project Structure

```
src/
├── main/java/com/springboot/
│   ├── controller/          # REST API endpoints
│   ├── model/              # Data models
│   ├── property/           # Configuration properties
│   ├── repository/         # Data access layer
│   └── service/            # Business logic
└── test/java/com/springboot/
    └── CartOfferApplicationTests.java  # Complete test suite
```

## Environment Setup Notes

### Mock Server Configuration
The mock server provides user segment mappings:
- User 1 → Segment p1
- User 2 → Segment p2  
- User 3 → Segment p3

### Service Health Check
Use the provided script to verify all services are running:
```bash
./check-services.sh
```

## Development Notes

### Test Philosophy
Our tests follow professional QA practices:
- Test the API as it is, not as we want it to be
- Report bugs instead of hiding them
- Use meaningful test names and clear documentation
- Separate test concerns from result reporting

### RestAssured Usage
All tests use RestAssured for clean, readable API testing:
```java
given()
    .contentType(ContentType.JSON)
    .body(cartRequest(200, 1, 1))
.when()
    .post("/cart/apply_offer")
.then()
    .statusCode(200)
    .body("cart_value", equalTo(190));
```

## Troubleshooting

### Common Issues

**Docker Not Starting**
```bash
# Check Docker status
docker info

# Restart Docker Desktop if needed
```

**Port Conflicts**
```bash
# Check what's using port 9001
lsof -i :9001

# Kill process if needed
kill -9 <PID>
```

**Test Failures**
- Most test failures indicate real API bugs
- Check service health with `./check-services.sh`
- Verify mock server is running on port 1080

### Service Status
```bash
# Main app should be on port 9001
curl http://localhost:9001/api/v1/health

# Mock server should be on port 1080
curl http://localhost:1080/mockserver/status
```

## Assignment Context

This project was developed as part of a QA automation assignment for testing Zomato's cart offer system. The goal was to:

1. Set up and run existing test cases
2. Create comprehensive test scenarios
3. Implement automated tests using RestAssured
4. Report findings professionally

The test suite demonstrates real-world QA practices including bug discovery, edge case testing, and proper test documentation.

## Contributing

When adding new tests:
1. Follow the existing naming convention (TC###_descriptive_name)
2. Include proper documentation with all test details
3. Use the helper methods for setup
4. Report bugs instead of working around them
5. Keep test code clean and readable

## Support

For questions about the test automation or API issues found, refer to the test case documentation in the code or the generated test reports.
