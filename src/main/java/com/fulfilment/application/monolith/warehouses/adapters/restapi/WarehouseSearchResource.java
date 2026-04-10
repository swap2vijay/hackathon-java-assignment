package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST resource for searching and filtering warehouse units.
 *
 * <p>Provides a paginated search endpoint with optional filters for location,
 * capacity range, and sorting. Archived warehouses are always excluded.</p>
 */
@Path("warehouse/search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class WarehouseSearchResource {

  @Inject
  EntityManager em;

  /**
   * Searches for active (non-archived) warehouses with optional filters.
   *
   * <p>All parameters are optional. Multiple filters use AND logic.
   * Results are paginated and sorted by the specified field.</p>
   *
   * @param location    filter by location identifier (e.g. "AMSTERDAM-001")
   * @param minCapacity filter warehouses with capacity >= this value
   * @param maxCapacity filter warehouses with capacity <= this value
   * @param sortBy      sort field: "createdAt" (default) or "capacity"
   * @param sortOrder   "asc" (default) or "desc"
   * @param page        page number, 0-indexed (default: 0)
   * @param pageSize    page size (default: 10, max: 100)
   * @return paginated list of matching warehouses with metadata
   */
  @GET
  @Transactional
  public Response search(
      @QueryParam("location") String location,
      @QueryParam("minCapacity") Integer minCapacity,
      @QueryParam("maxCapacity") Integer maxCapacity,
      @QueryParam("sortBy") @DefaultValue("createdAt") String sortBy,
      @QueryParam("sortOrder") @DefaultValue("asc") String sortOrder,
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("pageSize") @DefaultValue("10") int pageSize) {

    // Validate and cap pageSize
    if (pageSize < 1) pageSize = 10;
    if (pageSize > 100) pageSize = 100;
    if (page < 0) page = 0;

    // Validate sortBy
    if (!"createdAt".equals(sortBy) && !"capacity".equals(sortBy)) {
      sortBy = "createdAt";
    }

    // Validate sortOrder
    if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
      sortOrder = "asc";
    }

    // Build dynamic JPQL
    StringBuilder jpql = new StringBuilder("SELECT w FROM DbWarehouse w WHERE w.archivedAt IS NULL");
    StringBuilder countJpql = new StringBuilder("SELECT COUNT(w) FROM DbWarehouse w WHERE w.archivedAt IS NULL");
    Map<String, Object> params = new HashMap<>();

    if (location != null && !location.isBlank()) {
      jpql.append(" AND w.location = :location");
      countJpql.append(" AND w.location = :location");
      params.put("location", location);
    }

    if (minCapacity != null) {
      jpql.append(" AND w.capacity >= :minCapacity");
      countJpql.append(" AND w.capacity >= :minCapacity");
      params.put("minCapacity", minCapacity);
    }

    if (maxCapacity != null) {
      jpql.append(" AND w.capacity <= :maxCapacity");
      countJpql.append(" AND w.capacity <= :maxCapacity");
      params.put("maxCapacity", maxCapacity);
    }

    // Sorting
    jpql.append(" ORDER BY w.").append(sortBy).append(" ").append(sortOrder);

    // Count query
    TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
    params.forEach(countQuery::setParameter);
    long totalElements = countQuery.getSingleResult();

    // Data query with pagination
    TypedQuery<DbWarehouse> dataQuery = em.createQuery(jpql.toString(), DbWarehouse.class);
    params.forEach(dataQuery::setParameter);
    dataQuery.setFirstResult(page * pageSize);
    dataQuery.setMaxResults(pageSize);

    List<DbWarehouse> results = dataQuery.getResultList();

    // Build response
    List<Map<String, Object>> content = new ArrayList<>();
    for (DbWarehouse w : results) {
      Map<String, Object> item = new HashMap<>();
      item.put("businessUnitCode", w.businessUnitCode);
      item.put("location", w.location);
      item.put("capacity", w.capacity);
      item.put("stock", w.stock);
      item.put("createdAt", w.createdAt != null ? w.createdAt.toString() : null);
      content.add(item);
    }

    Map<String, Object> response = new HashMap<>();
    response.put("content", content);
    response.put("page", page);
    response.put("pageSize", pageSize);
    response.put("totalElements", totalElements);
    response.put("totalPages", (int) Math.ceil((double) totalElements / pageSize));

    return Response.ok(response).build();
  }
}
