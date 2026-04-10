package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WarehouseRepository} covering all branches
 * including error paths for update, remove, and findByBusinessUnitCode.
 */
@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject
  WarehouseRepository repository;

  @Inject
  EntityManager em;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode LIKE 'REPO-%'").executeUpdate();
  }

  @Test
  @Transactional
  public void testCreateAndFindByBusinessUnitCode() {
    Warehouse w = buildWarehouse("REPO-001", "AMSTERDAM-001", 50, 10);
    repository.create(w);

    Warehouse found = repository.findByBusinessUnitCode("REPO-001");
    assertNotNull(found);
    assertEquals("AMSTERDAM-001", found.location);
  }

  @Test
  @Transactional
  public void testFindByBusinessUnitCodeReturnsNullWhenNotFound() {
    Warehouse found = repository.findByBusinessUnitCode("NONEXISTENT");
    assertNull(found);
  }

  @Test
  @Transactional
  public void testGetAllReturnsAllWarehouses() {
    repository.create(buildWarehouse("REPO-A", "AMSTERDAM-001", 50, 10));
    repository.create(buildWarehouse("REPO-B", "ZWOLLE-001", 30, 5));

    var all = repository.getAll();
    assertTrue(all.size() >= 2, "Should contain at least the 2 created warehouses");
  }

  @Test
  @Transactional
  public void testUpdateWarehouseSuccessfully() {
    repository.create(buildWarehouse("REPO-UPD", "AMSTERDAM-001", 50, 10));

    Warehouse toUpdate = new Warehouse();
    toUpdate.businessUnitCode = "REPO-UPD";
    toUpdate.location = "ZWOLLE-001";
    toUpdate.capacity = 30;
    toUpdate.stock = 15;
    toUpdate.archivedAt = null;

    repository.update(toUpdate);

    Warehouse updated = repository.findByBusinessUnitCode("REPO-UPD");
    assertEquals("ZWOLLE-001", updated.location);
    assertEquals(30, updated.capacity);
  }

  @Test
  @Transactional
  public void testUpdateNonExistentWarehouseThrowsException() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "NONEXISTENT";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> repository.update(w));
    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  @Transactional
  public void testRemoveWarehouseSuccessfully() {
    repository.create(buildWarehouse("REPO-DEL", "AMSTERDAM-001", 50, 10));

    Warehouse toRemove = new Warehouse();
    toRemove.businessUnitCode = "REPO-DEL";
    repository.remove(toRemove);

    assertNull(repository.findByBusinessUnitCode("REPO-DEL"));
  }

  @Test
  @Transactional
  public void testRemoveNonExistentWarehouseThrowsException() {
    Warehouse w = new Warehouse();
    w.businessUnitCode = "NONEXISTENT";

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> repository.remove(w));
    assertTrue(ex.getMessage().contains("not found"));
  }

  private Warehouse buildWarehouse(String code, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    w.createdAt = java.time.LocalDateTime.now();
    return w;
  }
}
