package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Store REST endpoints.
 * Covers CRUD operations and error conditions.
 */
@QuarkusTest
public class StoreEndpointTest {

  @Test
  public void testListStores() {
    given().when().get("/store")
        .then().statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(3)));
  }

  @Test
  public void testGetStoreById() {
    given().when().get("/store/1")
        .then().statusCode(200)
        .body("name", is("TONSTAD"));
  }

  @Test
  public void testGetStoreNotFound() {
    given().when().get("/store/9999")
        .then().statusCode(404);
  }

  @Test
  public void testCreateAndUpdateStore() {
    // Create
    String name = "STORE-TEST-" + System.currentTimeMillis();
    int id = given().contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"quantityProductsInStock\":5}")
        .when().post("/store")
        .then().statusCode(201)
        .body("name", is(name))
        .extract().path("id");

    // Update
    given().contentType("application/json")
        .body("{\"name\":\"" + name + "-UPDATED\",\"quantityProductsInStock\":99}")
        .when().put("/store/" + id)
        .then().statusCode(200)
        .body("name", is(name + "-UPDATED"))
        .body("quantityProductsInStock", is(99));

    // Patch
    given().contentType("application/json")
        .body("{\"name\":\"" + name + "-PATCHED\",\"quantityProductsInStock\":77}")
        .when().patch("/store/" + id)
        .then().statusCode(200);

    // Delete
    given().when().delete("/store/" + id)
        .then().statusCode(204);
  }

  @Test
  public void testCreateStoreWithIdFails() {
    given().contentType("application/json")
        .body("{\"id\":999,\"name\":\"BAD\",\"quantityProductsInStock\":5}")
        .when().post("/store")
        .then().statusCode(422);
  }

  @Test
  public void testUpdateStoreWithoutNameFails() {
    given().contentType("application/json")
        .body("{\"quantityProductsInStock\":5}")
        .when().put("/store/1")
        .then().statusCode(422);
  }

  @Test
  public void testUpdateStoreNotFound() {
    given().contentType("application/json")
        .body("{\"name\":\"X\",\"quantityProductsInStock\":5}")
        .when().put("/store/9999")
        .then().statusCode(404);
  }

  @Test
  public void testDeleteStoreNotFound() {
    given().when().delete("/store/9999")
        .then().statusCode(404);
  }
}
