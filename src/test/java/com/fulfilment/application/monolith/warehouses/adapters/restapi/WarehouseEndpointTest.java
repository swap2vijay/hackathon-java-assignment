package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for all warehouse REST endpoints.
 * Covers positive, negative, and error conditions through HTTP.
 */
@QuarkusTest
public class WarehouseEndpointTest {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode NOT IN ('MWH.001', 'MWH.012', 'MWH.023')").executeUpdate();
    var warehouses = em.createQuery("SELECT w FROM DbWarehouse w WHERE w.archivedAt IS NOT NULL",
        com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse.class).getResultList();
    for (var w : warehouses) {
      w.archivedAt = null;
      em.merge(w);
    }
    em.flush();
  }

  // --- List ---

  @Test
  public void testListAllWarehouses() {
    given().when().get("/warehouse")
        .then().statusCode(200)
        .body("$", hasSize(3));
  }

  // --- Get by ID ---

  @Test
  public void testGetWarehouseByCode() {
    given().when().get("/warehouse/MWH.001")
        .then().statusCode(200)
        .body("businessUnitCode", is("MWH.001"))
        .body("location", is("ZWOLLE-001"));
  }

  @Test
  public void testGetWarehouseNotFound() {
    given().when().get("/warehouse/NONEXISTENT")
        .then().statusCode(404);
  }

  // --- Create ---

  @Test
  public void testCreateWarehouseSuccess() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-CREATE\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse")
        .then().statusCode(200)
        .body("businessUnitCode", is("TEST-CREATE"))
        .body("location", is("AMSTERDAM-001"));
  }

  @Test
  public void testCreateWarehouseDuplicateCode() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.001\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse")
        .then().statusCode(400)
        .body("error", containsString("already exists"));
  }

  @Test
  public void testCreateWarehouseInvalidLocation() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-BAD-LOC\",\"location\":\"INVALID\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse")
        .then().statusCode(400)
        .body("error", containsString("not valid"));
  }

  @Test
  public void testCreateWarehouseCapacityExceedsMax() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-OVER-CAP\",\"location\":\"ZWOLLE-001\",\"capacity\":999,\"stock\":10}")
        .when().post("/warehouse")
        .then().statusCode(400)
        .body("error", containsString("exceeds location max capacity"));
  }

  @Test
  public void testCreateWarehouseStockExceedsCapacity() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-OVER-STK\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":60}")
        .when().post("/warehouse")
        .then().statusCode(400)
        .body("error", containsString("exceeds warehouse capacity"));
  }

  // --- Archive ---

  @Test
  public void testArchiveWarehouseSuccess() {
    // Create then archive
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-ARCHIVE\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse").then().statusCode(200);

    given().when().delete("/warehouse/TEST-ARCHIVE")
        .then().statusCode(204);
  }

  @Test
  public void testArchiveWarehouseNotFound() {
    given().when().delete("/warehouse/NONEXISTENT")
        .then().statusCode(404);
  }

  @Test
  public void testArchiveAlreadyArchivedWarehouse() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-DOUBLE-ARC\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse").then().statusCode(200);

    given().when().delete("/warehouse/TEST-DOUBLE-ARC").then().statusCode(204);
    given().when().delete("/warehouse/TEST-DOUBLE-ARC").then().statusCode(400);
  }

  // --- Replace ---

  @Test
  public void testReplaceWarehouseSuccess() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-REPLACE\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse").then().statusCode(200);

    given().contentType("application/json")
        .body("{\"location\":\"ZWOLLE-001\",\"capacity\":30,\"stock\":15}")
        .when().post("/warehouse/TEST-REPLACE/replacement")
        .then().statusCode(200)
        .body("location", is("ZWOLLE-001"))
        .body("capacity", is(30));
  }

  @Test
  public void testReplaceWarehouseNotFound() {
    given().contentType("application/json")
        .body("{\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse/NONEXISTENT/replacement")
        .then().statusCode(400);
  }

  @Test
  public void testReplaceArchivedWarehouse() {
    given().contentType("application/json")
        .body("{\"businessUnitCode\":\"TEST-ARC-REP\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":10}")
        .when().post("/warehouse").then().statusCode(200);

    given().when().delete("/warehouse/TEST-ARC-REP").then().statusCode(204);

    given().contentType("application/json")
        .body("{\"location\":\"ZWOLLE-001\",\"capacity\":30,\"stock\":15}")
        .when().post("/warehouse/TEST-ARC-REP/replacement")
        .then().statusCode(400)
        .body("error", containsString("archived"));
  }

  // --- Health Check ---

  @Test
  public void testHealthEndpoint() {
    given().when().get("/q/health")
        .then().statusCode(200)
        .body("status", is("UP"));
  }
}
