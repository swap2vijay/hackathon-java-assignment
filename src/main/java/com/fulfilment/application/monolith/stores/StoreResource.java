package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST resource for managing stores.
 *
 * <p>Provides CRUD operations for store entities. Store mutations fire CDI events
 * that are observed by {@link StoreEventObserver} to synchronize changes with the
 * legacy store management system. Events are processed only after successful
 * transaction commit to ensure data consistency.</p>
 */
@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;
  
  @Inject Event<StoreCreatedEvent> storeCreatedEvent;
  
  @Inject Event<StoreUpdatedEvent> storeUpdatedEvent;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  /**
   * Lists all stores sorted alphabetically by name.
   *
   * @return list of all stores
   */
  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  /**
   * Retrieves a single store by its ID.
   *
   * @param id the store ID
   * @return the store entity
   * @throws WebApplicationException 404 if store not found
   */
  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  /**
   * Creates a new store and fires a {@link StoreCreatedEvent} for legacy system sync.
   *
   * @param store the store data (id must be null)
   * @return 201 response with the created store
   * @throws WebApplicationException 422 if id is set on request
   */
  @POST
  @Transactional
  public Response create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    store.persist();
    storeCreatedEvent.fire(new StoreCreatedEvent(store));

    return Response.ok(store).status(201).build();
  }

  /**
   * Fully updates a store by ID and fires a {@link StoreUpdatedEvent} for legacy system sync.
   *
   * @param id the store ID
   * @param updatedStore the new store data (name is required)
   * @return the updated store entity
   * @throws WebApplicationException 422 if name is null, 404 if store not found
   */
  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;

    storeUpdatedEvent.fire(new StoreUpdatedEvent(entity));

    return entity;
  }

  /**
   * Partially updates a store by ID. Only non-null/non-zero fields are updated.
   * Fires a {@link StoreUpdatedEvent} for legacy system sync.
   *
   * @param id the store ID
   * @param updatedStore the partial store data (name is required)
   * @return the patched store entity
   * @throws WebApplicationException 422 if name is null, 404 if store not found
   */
  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    if (entity.name != null) {
      entity.name = updatedStore.name;
    }

    if (entity.quantityProductsInStock != 0) {
      entity.quantityProductsInStock = updatedStore.quantityProductsInStock;
    }

    storeUpdatedEvent.fire(new StoreUpdatedEvent(entity));

    return entity;
  }

  /**
   * Deletes a store by ID.
   *
   * @param id the store ID
   * @return 204 No Content on success
   * @throws WebApplicationException 404 if store not found
   */
  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.status(204).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      LOGGER.error("Failed to handle request", exception);

      int code = 500;
      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      ObjectNode exceptionJson = objectMapper.createObjectNode();
      exceptionJson.put("exceptionType", exception.getClass().getName());
      exceptionJson.put("code", code);

      if (exception.getMessage() != null) {
        exceptionJson.put("error", exception.getMessage());
      }

      return Response.status(code).entity(exceptionJson).build();
    }
  }
}
