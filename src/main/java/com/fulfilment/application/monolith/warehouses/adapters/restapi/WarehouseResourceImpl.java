package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST resource for managing warehouse units.
 *
 * <p>This controller implements the OpenAPI-generated {@link WarehouseResource} interface.
 * All endpoints delegate business logic to domain use cases following hexagonal architecture.
 * Validation errors from use cases are translated to appropriate HTTP status codes.</p>
 *
 * @see WarehouseResource
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOG = Logger.getLogger(WarehouseResourceImpl.class);

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  /**
   * Lists all warehouse units in the system, including archived ones.
   *
   * @return list of all warehouses
   */
  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    LOG.info("Listing all warehouse units");
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  /**
   * Creates a new warehouse unit after validating business rules.
   *
   * <p>Validations performed:
   * <ul>
   *   <li>Business unit code must be unique</li>
   *   <li>Location must exist in the system</li>
   *   <li>Capacity must not exceed the location's max capacity</li>
   *   <li>Stock must not exceed the warehouse capacity</li>
   * </ul>
   *
   * @param data the warehouse data to create
   * @return the created warehouse
   * @throws WebApplicationException 400 if validation fails
   */
  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = data.getBusinessUnitCode();
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Create warehouse through use case (includes validations)
      createWarehouseOperation.create(domainWarehouse);
      LOG.infof("Warehouse '%s' created via REST", data.getBusinessUnitCode());
      
      // Return the created warehouse
      return toWarehouseResponse(domainWarehouse);
    } catch (IllegalArgumentException e) {
      LOG.warnf("Failed to create warehouse '%s': %s", data.getBusinessUnitCode(), e.getMessage());
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  /**
   * Retrieves a single warehouse unit by its business unit code.
   *
   * @param id the business unit code of the warehouse
   * @return the warehouse matching the given code
   * @throws WebApplicationException 404 if no warehouse is found
   */
  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);
    
    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }
    
    return toWarehouseResponse(domainWarehouse);
  }

  /**
   * Archives a warehouse unit by setting its archivedAt timestamp.
   *
   * <p>An archived warehouse cannot be modified or archived again.
   * Only active (non-archived) warehouses can be archived.</p>
   *
   * @param id the business unit code of the warehouse to archive
   * @throws WebApplicationException 404 if warehouse not found, 400 if already archived
   */
  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);

    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }

    try {
      // Archive warehouse through use case (includes validations)
      archiveWarehouseOperation.archive(domainWarehouse);
      LOG.infof("Warehouse '%s' archived via REST", id);
    } catch (IllegalArgumentException e) {
      LOG.warnf("Failed to archive warehouse '%s': %s", id, e.getMessage());
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  /**
   * Replaces an active warehouse with new data while preserving its business unit code.
   *
   * <p>The existing warehouse's location, capacity, and stock are updated.
   * Archived warehouses cannot be replaced. The new values must pass the same
   * validations as warehouse creation (valid location, capacity within limits, etc.).</p>
   *
   * @param businessUnitCode the business unit code of the warehouse to replace
   * @param data the new warehouse data
   * @return the updated warehouse
   * @throws WebApplicationException 404 if not found, 400 if archived or validation fails
   */
  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = businessUnitCode; // Use businessUnitCode from path
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Replace warehouse through use case (includes validations)
      replaceWarehouseOperation.replace(domainWarehouse);
      LOG.infof("Warehouse '%s' replaced via REST", businessUnitCode);

      // Return the updated warehouse
      var updated = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toWarehouseResponse(updated);
    } catch (IllegalArgumentException e) {
      LOG.warnf("Failed to replace warehouse '%s': %s", businessUnitCode, e.getMessage());
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
