package com.fulfilment.application.monolith.products;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
 * REST resource for managing products.
 *
 * <p>Provides standard CRUD operations for product entities.
 * Products are persisted via {@link ProductRepository} using Hibernate Panache.</p>
 */
@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductResource {

  @Inject ProductRepository productRepository;

  private static final Logger LOGGER = Logger.getLogger(ProductResource.class.getName());

  /**
   * Lists all products sorted alphabetically by name.
   *
   * @return list of all products
   */
  @GET
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  /**
   * Retrieves a single product by its ID.
   *
   * @param id the product ID
   * @return the product entity
   * @throws WebApplicationException 404 if product not found
   */
  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  /**
   * Creates a new product.
   *
   * @param product the product data (id must be null)
   * @return 201 response with the created product
   * @throws WebApplicationException 422 if id is set on request
   */
  @POST
  @Transactional
  public Response create(Product product) {
    if (product.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  /**
   * Updates an existing product by ID.
   *
   * @param id the product ID
   * @param product the new product data (name is required)
   * @return the updated product entity
   * @throws WebApplicationException 422 if name is null, 404 if product not found
   */
  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, Product product) {
    if (product.name == null) {
      throw new WebApplicationException("Product Name was not set on request.", 422);
    }

    Product entity = productRepository.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }

    entity.name = product.name;
    entity.description = product.description;
    entity.price = product.price;
    entity.stock = product.stock;

    productRepository.persist(entity);

    return entity;
  }

  /**
   * Deletes a product by ID.
   *
   * @param id the product ID
   * @return 204 No Content on success
   * @throws WebApplicationException 404 if product not found
   */
  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Product with id of " + id + " does not exist.", 404);
    }
    productRepository.delete(entity);
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
