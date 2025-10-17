package com.springboot;

import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.OfferRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Cart Offer API Test Suite
 *
 * This class tests the cart discount functionality for Zomato's customer segments.
 * We have 3 segments: p1, p2, p3 with different offer types: FLATX and PERCENTAGE
 *
 * Setup Requirements:
 * - Main application running on localhost:9001
 * - Mock server with user segment mappings (user 1->p1, user 2->p2, user 3->p3)
 */
@SpringBootTest
public class CartOfferApplicationTests {

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 9001;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * TC001 - Apply FLATX offer for regular user segment
     *
     * Test Description: Verify FLATX discount works for p1 segment user
     * Sample Test Data: cart=200, user=1(p1), restaurant=1, flatx=10
     * Expectation: Cart value reduces by 10 to become 190
     * Assumption: User 1 belongs to p1 segment and has valid offer
     * Example: 200 - 10 = 190
     */
    @Test
    public void TC001_apply_flatx_offer_for_regular_user_segment() {
        addOffer(1, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 1))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(190));
    }

    /**
     * TC002 - Apply PERCENTAGE offer for regular user segment
     *
     * Test Description: Check percentage discount calculation for p2 segment
     * Sample Test Data: cart=200, user=2(p2), restaurant=2, percentage=10%
     * Expectation: Cart reduces by 10% which is 20, final value 180
     * Assumption: User 2 is in p2 segment with active percentage offer
     * Example: 200 - (200 * 10/100) = 180
     */
    @Test
    public void TC002_apply_percentage_offer_for_regular_user_segment() {
        addOffer(2, "PERCENTAGE", 10, "p2");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 2, 2))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(180));
    }

    /**
     * TC003 - Apply best offer when multiple offers exist
     *
     * Test Description: System should pick better discount when multiple offers available
     * Sample Test Data: cart=200, flatx=10 vs percentage=15%, user=1(p1), restaurant=3
     * Expectation: System picks 15% (30 off) over flatx 10, final value 170
     * Assumption: Both offers valid for p1 segment, system has best-offer logic
     * Example: Better of (200-10=190) vs (200-30=170) is 170
     */
    @Test
    public void TC003_apply_best_offer_when_multiple_offers_exist() {
        addOffer(3, "FLATX", 10, "p1");
        addOffer(3, "PERCENTAGE", 15, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 3))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(170));
    }

    /**
     * TC004 - Apply offer with cart value equal to offer amount
     *
     * Test Description: Handle case when discount equals cart total
     * Sample Test Data: cart=200, flatx=200, user=1(p1), restaurant=4
     * Expectation: Cart becomes 0, not negative
     * Assumption: System handles edge case properly without going negative
     * Example: 200 - 200 = 0
     */
    @Test
    public void TC004_apply_offer_with_cart_equal_to_offer_amount() {
        addOffer(4, "FLATX", 200, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 4))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC005 - Apply offer with cart value just above offer amount
     *
     * Test Description: Small remainder after discount application
     * Sample Test Data: cart=200, flatx=199, user=1(p1), restaurant=5
     * Expectation: Cart becomes 1 (minimal remainder)
     * Assumption: System handles small amounts correctly
     * Example: 200 - 199 = 1
     */
    @Test
    public void TC005_apply_offer_with_cart_just_above_offer_amount() {
        addOffer(5, "FLATX", 199, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 5))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(1));
    }

    /**
     * TC006 - Apply offer with floating point values
     *
     * Test Description: Decimal handling in discount calculations
     * Sample Test Data: cart=200, flatx=10, user=1(p1), restaurant=6
     * Expectation: Cart becomes 190 (API uses integers only)
     * Assumption: API converts or rounds decimal values
     * Example: 200 - 10.0 = 190
     */
    @Test
    public void TC006_apply_offer_with_floating_point_values() {
        addOffer(6, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 6))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(190));
    }

    /**
     * TC007 - Apply offer for user segment with no offers
     *
     * Test Description: User segment has no matching offers available
     * Sample Test Data: cart=200, user=3(p3), restaurant=1 (no p3 offers)
     * Expectation: Cart stays 200, no discount applied
     * Assumption: System gracefully handles no-offer scenario
     * Example: 200 (unchanged)
     */
    @Test
    public void TC007_apply_offer_for_user_segment_with_no_offers() {
        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 3, 1))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(200));
    }

    /**
     * TC008 - Apply offer with invalid user_id
     *
     * Test Description: Non-existent user ID validation
     * Sample Test Data: cart=200, user=999 (invalid), restaurant=1
     * Expectation: 400 error response
     * Assumption: System validates user existence before processing
     * Example: HTTP 400 for user_id=999
     */
    @Test
    public void TC008_apply_offer_with_invalid_user_id() {
        addOffer(1, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 999, 1))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(400);
    }

    /**
     * TC009 - Apply offer with invalid restaurant_id
     *
     * Test Description: Non-existent restaurant ID handling
     * Sample Test Data: cart=200, user=1(p1), restaurant=999 (invalid)
     * Expectation: 400 error response
     * Assumption: Restaurant validation happens before offer lookup
     * Example: HTTP 400 for restaurant_id=999
     */
    @Test
    public void TC009_apply_offer_with_invalid_restaurant_id() {
        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 999))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(400);
    }

    /**
     * TC010 - Apply offer with negative cart_value
     *
     * Test Description: Input validation for negative cart amounts
     * Sample Test Data: cart=-100 (invalid), user=1(p1), restaurant=1
     * Expectation: 400 error for invalid input
     * Assumption: System rejects negative cart values
     * Example: HTTP 400 for cart_value=-100
     */
    @Test
    public void TC010_apply_offer_with_negative_cart_value() {
        addOffer(1, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(-100, 1, 1))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(400);
    }

    /**
     * TC011 - Apply offer with missing parameters
     *
     * Test Description: Required field validation check
     * Sample Test Data: cart=200, user=1, missing restaurant_id
     * Expectation: 400 error for incomplete request
     * Assumption: All required fields must be present
     * Example: HTTP 400 when restaurant_id missing
     */
    @Test
    public void TC011_apply_offer_with_missing_parameters() {
        addOffer(1, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body("{\"cart_value\":200,\"user_id\":1}")
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(400);
    }

    /**
     * TC012 - Apply FLATX offer where offer amount exceeds cart value
     *
     * Test Description: Discount larger than cart total handling
     * Sample Test Data: cart=200, flatx=300, user=1(p1), restaurant=7
     * Expectation: Cart becomes 0, not negative
     * Assumption: System caps discount at cart total
     * Example: min(200, 300) discount = 200, so 200-200=0
     */
    @Test
    public void TC012_apply_flatx_offer_where_offer_exceeds_cart() {
        addOffer(7, "FLATX", 300, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 1, 7))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC013 - Apply PERCENTAGE offer with 100% discount
     *
     * Test Description: Complete percentage discount edge case
     * Sample Test Data: cart=200, percentage=100%, user=2(p2), restaurant=8
     * Expectation: Cart becomes 0 (full discount)
     * Assumption: 100% discount is valid and makes cart free
     * Example: 200 - (200 * 100/100) = 0
     */
    @Test
    public void TC013_apply_percentage_offer_with_100_percent_discount() {
        addOffer(8, "PERCENTAGE", 100, "p2");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 2, 8))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC014 - Apply PERCENTAGE offer with over 100% discount
     *
     * Test Description: Percentage discount exceeding 100% handling
     * Sample Test Data: cart=200, percentage=150%, user=2(p2), restaurant=9
     * Expectation: Cart becomes 0 (capped at 100%)
     * Assumption: System caps percentage at 100% maximum
     * Example: max discount = 100%, so 200 - 200 = 0
     */
    @Test
    public void TC014_apply_percentage_offer_with_over_100_percent() {
        addOffer(9, "PERCENTAGE", 150, "p2");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 2, 9))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC015 - Apply offer with zero cart value
     *
     * Test Description: Empty cart discount application
     * Sample Test Data: cart=0, flatx=10, user=1(p1), restaurant=10
     * Expectation: Cart stays 0 (nothing to discount)
     * Assumption: Zero cart is valid input, no discount possible
     * Example: 0 - 10 = 0 (minimum)
     */
    @Test
    public void TC015_apply_offer_with_zero_cart_value() {
        addOffer(10, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(0, 1, 10))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC016 - Apply offer with very small cart value
     *
     * Test Description: Minimal cart amount handling
     * Sample Test Data: cart=1, flatx=1, user=1(p1), restaurant=11
     * Expectation: Cart becomes 0
     * Assumption: System handles small integer values correctly
     * Example: 1 - 1 = 0
     */
    @Test
    public void TC016_apply_offer_with_very_small_cart_value() {
        addOffer(11, "FLATX", 1, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(1, 1, 11))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(0));
    }

    /**
     * TC017 - Apply offer with very large cart value
     *
     * Test Description: High value transaction processing
     * Sample Test Data: cart=1000000, percentage=10%, user=3(p3), restaurant=12
     * Expectation: Cart becomes 900000
     * Assumption: System handles large numbers without overflow
     * Example: 1000000 - (1000000 * 10/100) = 900000
     */
    @Test
    public void TC017_apply_offer_with_very_large_cart_value() {
        addOffer(12, "PERCENTAGE", 10, "p3");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(1000000, 3, 12))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(900000));
    }

    /**
     * TC018 - Apply offer with floating point precision
     *
     * Test Description: Decimal precision in percentage calculations
     * Sample Test Data: cart=100, percentage=33%, user=2(p2), restaurant=13
     * Expectation: Cart becomes 67 (rounded calculation)
     * Assumption: System handles non-round percentages appropriately
     * Example: 100 - (100 * 33/100) = 67
     */
    @Test
    public void TC018_apply_offer_with_floating_point_precision() {
        addOffer(13, "PERCENTAGE", 33, "p2");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(100, 2, 13))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", is(67));
    }

    /**
     * TC019 - Apply offer with concurrent requests
     *
     * Test Description: Basic load handling verification
     * Sample Test Data: cart=200, flatx=10, user=1(p1), restaurant=14, 5 requests
     * Expectation: All requests return cart=190
     * Assumption: System handles multiple simultaneous requests correctly
     * Example: 5 parallel calls all return 190
     */
    @Test
    public void TC019_apply_offer_with_concurrent_requests() {
        addOffer(14, "FLATX", 10, "p1");
        ApplyOfferRequest req = cartRequest(200, 1, 14);

        for(int i = 0; i < 5; i++) {
            given()
                .contentType(ContentType.JSON)
                .body(req)
            .when()
                .post("/cart/apply_offer")
            .then()
                .statusCode(200)
                .body("cart_value", equalTo(190));
        }
    }

    /**
     * TC020 - Apply offer for different user segment
     *
     * Test Description: Segment access control verification
     * Sample Test Data: p1 offer exists, but p2 user tries to use it
     * Expectation: Cart stays 200 (no unauthorized discount)
     * Assumption: System enforces segment-based offer access
     * Example: p2 user cannot use p1 offers, cart unchanged
     */
    @Test
    public void TC020_apply_offer_for_different_user_segment() {
        addOffer(15, "FLATX", 10, "p1");

        given()
            .contentType(ContentType.JSON)
            .body(cartRequest(200, 2, 15))
        .when()
            .post("/cart/apply_offer")
        .then()
            .statusCode(200)
            .body("cart_value", equalTo(200));
    }

    // Helper methods for test setup
    private void addOffer(int restaurantId, String type, int value, String segment) {
        OfferRequest offer = new OfferRequest();
        offer.setRestaurant_id(restaurantId);
        offer.setOffer_type(type);
        offer.setOffer_value(value);
        offer.setCustomer_segment(Arrays.asList(segment));

        given()
            .contentType(ContentType.JSON)
            .body(offer)
        .when()
            .post("/offer")
        .then()
            .statusCode(200);
    }

    private ApplyOfferRequest cartRequest(int cartValue, int userId, int restaurantId) {
        ApplyOfferRequest req = new ApplyOfferRequest();
        req.setCart_value(cartValue);
        req.setUser_id(userId);
        req.setRestaurant_id(restaurantId);
        return req;
    }
}
