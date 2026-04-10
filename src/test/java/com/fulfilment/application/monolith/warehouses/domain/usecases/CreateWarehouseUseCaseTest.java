package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CreateWarehouseUseCase}.
 * Uses Mockito mocks for pure unit testing without database dependencies.
 */
@ExtendWith(MockitoExtension.class)
public class CreateWarehouseUseCaseTest {

  @Mock
  private WarehouseStore warehouseStore;

  @Mock
  private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setup() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  // --- Positive Tests ---

  @Test
  void testCreateWarehouseSuccessfully() {
    when(warehouseStore.findByBusinessUnitCode("NEW-001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse warehouse = buildWarehouse("NEW-001", "AMSTERDAM-001", 50, 10);
    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
    assertNotNull(warehouse.createdAt, "createdAt should be set");
  }

  @Test
  void testCreateWarehouseAtMaxCapacity() {
    when(warehouseStore.findByBusinessUnitCode("MAX-CAP")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));

    Warehouse warehouse = buildWarehouse("MAX-CAP", "ZWOLLE-001", 40, 40);
    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void testCreateWarehouseWithZeroStock() {
    when(warehouseStore.findByBusinessUnitCode("ZERO-STOCK")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse warehouse = buildWarehouse("ZERO-STOCK", "AMSTERDAM-001", 50, 0);
    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  // --- Negative Tests ---

  @Test
  void testCreateWarehouseWithDuplicateCodeThrowsException() {
    Warehouse existing = buildWarehouse("DUP-001", "AMSTERDAM-001", 50, 10);
    when(warehouseStore.findByBusinessUnitCode("DUP-001")).thenReturn(existing);

    Warehouse warehouse = buildWarehouse("DUP-001", "ZWOLLE-001", 30, 5);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("already exists"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateWarehouseWithInvalidLocationThrowsException() {
    when(warehouseStore.findByBusinessUnitCode("BAD-LOC")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

    Warehouse warehouse = buildWarehouse("BAD-LOC", "INVALID-LOC", 50, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("not valid"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateWarehouseCapacityExceedsLocationMaxThrowsException() {
    when(warehouseStore.findByBusinessUnitCode("OVER-CAP")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));

    Warehouse warehouse = buildWarehouse("OVER-CAP", "ZWOLLE-001", 50, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("exceeds location max capacity"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateWarehouseStockExceedsCapacityThrowsException() {
    when(warehouseStore.findByBusinessUnitCode("OVER-STOCK")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    Warehouse warehouse = buildWarehouse("OVER-STOCK", "AMSTERDAM-001", 50, 60);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void testCreateWarehouseWithEmptyLocationThrowsException() {
    when(warehouseStore.findByBusinessUnitCode("EMPTY-LOC")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("")).thenReturn(null);

    Warehouse warehouse = buildWarehouse("EMPTY-LOC", "", 50, 10);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> useCase.create(warehouse));
    assertTrue(ex.getMessage().contains("not valid"));
  }

  // --- Helper ---

  private Warehouse buildWarehouse(String code, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
