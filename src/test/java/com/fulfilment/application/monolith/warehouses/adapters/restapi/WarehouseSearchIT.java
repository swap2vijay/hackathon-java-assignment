package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the warehouse search and filter endpoint.
 *
 * <p>Uses the seed data from import.sql:
 * <ul>
 *   <li>MWH.001 - ZWOLLE-001, capacity=100, stock=10</li>
 *   <li>MWH.012 - AMSTERDAM-001, capacity=50, stock=5</li>
 *   <li>MWH.023 - TILBURG-001, capacity=30, stock=27</li>
 * </ul>
 */
@QuarkusTest
public class WarehouseSearchIT {

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    // Reset to seed data only — remove any test-created warehouses
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode NOT IN ('MWH.001', 'MWH.012', 'MWH.023')")
        .executeUpdate();
    // Ensure seed data is not archived (use entity-based update to respect @Version)
    var warehouses = em.createQuery("SELECT w FROM DbWarehouse w WHERE w.archivedAt IS NOT NULL", 
        com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse.class).getResultList();
    for (var w : warehouses) {
      w.archivedAt = null;
      em.merge(w);
    }
    em.flush();
  }

  @Test
  public void testSearchReturnsAllActiveWarehouses() {
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("totalElements", is(3))
        .body("page", is(0))
        .body("pageSize", is(10));
  }

  @Test
  public void testSearchFilterByLocation() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].businessUnitCode", is("MWH.012"))
        .body("totalElements", is(1));
  }

  @Test
  public void testSearchFilterByMinCapacity() {
    given()
        .queryParam("minCapacity", 50)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("totalElements", is(2))
        .body("content.capacity", everyItem(greaterThanOrEqualTo(50)));
  }

  @Test
  public void testSearchFilterByMaxCapacity() {
    given()
        .queryParam("maxCapacity", 50)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("totalElements", is(2))
        .body("content.capacity", everyItem(lessThanOrEqualTo(50)));
  }

  @Test
  public void testSearchFilterByCapacityRange() {
    given()
        .queryParam("minCapacity", 40)
        .queryParam("maxCapacity", 60)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].businessUnitCode", is("MWH.012"))
        .body("totalElements", is(1));
  }

  @Test
  public void testSearchCombinedFilters() {
    given()
        .queryParam("location", "ZWOLLE-001")
        .queryParam("minCapacity", 50)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].businessUnitCode", is("MWH.001"));
  }

  @Test
  public void testSearchNoResults() {
    given()
        .queryParam("location", "NONEXISTENT-001")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(0))
        .body("totalElements", is(0))
        .body("totalPages", is(0));
  }

  @Test
  public void testSearchPagination() {
    given()
        .queryParam("pageSize", 2)
        .queryParam("page", 0)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(2))
        .body("totalElements", is(3))
        .body("totalPages", is(2))
        .body("page", is(0))
        .body("pageSize", is(2));

    // Second page
    given()
        .queryParam("pageSize", 2)
        .queryParam("page", 1)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("page", is(1));
  }

  @Test
  public void testSearchSortByCapacityAsc() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "asc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content[0].capacity", is(30))
        .body("content[1].capacity", is(50))
        .body("content[2].capacity", is(100));
  }

  @Test
  public void testSearchSortByCapacityDesc() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content[0].capacity", is(100))
        .body("content[1].capacity", is(50))
        .body("content[2].capacity", is(30));
  }

  @Test
  public void testSearchExcludesArchivedWarehouses() {
    // First verify all 3 are returned
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(3));

    // Archive one warehouse directly via the archive endpoint
    // Use a separate create + archive flow to avoid seed data version issues
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\": \"ARCHIVE-SEARCH-TEST\", \"location\": \"AMSTERDAM-001\", \"capacity\": 50, \"stock\": 10}")
        .when().post("/warehouse")
        .then()
        .statusCode(200);

    given()
        .when().delete("/warehouse/ARCHIVE-SEARCH-TEST")
        .then()
        .statusCode(204);

    // Archived warehouse should not appear in search
    given()
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("content", hasSize(3))
        .body("totalElements", is(3));
  }

  @Test
  public void testSearchPageSizeCappedAt100() {
    given()
        .queryParam("pageSize", 500)
        .when().get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("pageSize", is(100));
  }
}
