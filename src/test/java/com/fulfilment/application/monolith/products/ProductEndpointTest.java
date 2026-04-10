package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testListProducts() {
    given().when().get("/product")
        .then().statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testGetProductById() {
    given().when().get("/product/2")
        .then().statusCode(200)
        .body("name", is("KALLAX"));
  }

  @Test
  public void testGetProductNotFound() {
    given().when().get("/product/9999")
        .then().statusCode(404);
  }

  @Test
  public void testCreateProduct() {
    given().contentType("application/json")
        .body("{\"name\":\"TEST-PRODUCT-" + System.currentTimeMillis() + "\",\"stock\":50}")
        .when().post("/product")
        .then().statusCode(201);
  }

  @Test
  public void testCreateProductWithIdFails() {
    given().contentType("application/json")
        .body("{\"id\":999,\"name\":\"BAD\",\"stock\":5}")
        .when().post("/product")
        .then().statusCode(422);
  }

  @Test
  public void testUpdateProduct() {
    // Create first
    int id = given().contentType("application/json")
        .body("{\"name\":\"UPDATE-TEST-" + System.currentTimeMillis() + "\",\"stock\":10}")
        .when().post("/product")
        .then().statusCode(201)
        .extract().path("id");

    // Update
    given().contentType("application/json")
        .body("{\"name\":\"UPDATED-NAME\",\"description\":\"desc\",\"price\":19.99,\"stock\":20}")
        .when().put("/product/" + id)
        .then().statusCode(200)
        .body("name", is("UPDATED-NAME"))
        .body("stock", is(20));
  }

  @Test
  public void testUpdateProductWithoutNameFails() {
    given().contentType("application/json")
        .body("{\"stock\":5}")
        .when().put("/product/2")
        .then().statusCode(422);
  }

  @Test
  public void testUpdateProductNotFound() {
    given().contentType("application/json")
        .body("{\"name\":\"X\",\"stock\":5}")
        .when().put("/product/9999")
        .then().statusCode(404);
  }

  @Test
  public void testDeleteProduct() {
    // Create then delete
    int id = given().contentType("application/json")
        .body("{\"name\":\"DELETE-TEST-" + System.currentTimeMillis() + "\",\"stock\":10}")
        .when().post("/product")
        .then().statusCode(201)
        .extract().path("id");

    given().when().delete("/product/" + id)
        .then().statusCode(204);
  }

  @Test
  public void testDeleteProductNotFound() {
    given().when().delete("/product/9999")
        .then().statusCode(404);
  }
}
