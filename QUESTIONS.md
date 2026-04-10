# Questions

Here are 2 questions related to the codebase. There's no right or wrong answer - we want to understand your reasoning.

## Question 1: API Specification Approaches

When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded everything directly. 

What are your thoughts on the pros and cons of each approach? Which would you choose and why?

**Answer:**
```txt
OpenAPI Code Generation (Warehouse approach):

Pros:
- Contract-first design ensures the API spec is the single source of truth. Frontend 
  teams, QA, and external consumers can work from the spec before implementation exists.
- Generated interfaces enforce consistency — you cannot accidentally drift from the 
  contract without a compilation error.
- Swagger UI is auto-generated from the spec, keeping documentation always in sync.
- Schema validation and request/response models are generated, reducing boilerplate.
- Easier to version and review API changes through YAML diffs.

Cons:
- Adds build complexity — the OpenAPI generator plugin must run during compilation, 
  and generated code can be hard to debug.
- Less flexibility for quick iterations; every API change requires updating the YAML 
  first, then regenerating.
- Generated code may not follow project conventions (naming, structure), and 
  customizing it often requires workarounds.
- Developers need to understand both the spec format and the generator's quirks.

Hand-Coded (Product/Store approach):

Pros:
- Full control over the code — easy to add custom annotations, error handling, 
  and framework-specific features like CDI events.
- Faster iteration cycle: change the code, test, done. No intermediate generation step.
- Easier to debug since all code is visible and written by the team.
- Lower learning curve for developers unfamiliar with OpenAPI tooling.

Cons:
- API documentation can drift from the actual implementation unless manually maintained.
- No compile-time guarantee that the API matches any external contract.
- Harder to share the API contract with other teams before implementation.
- More boilerplate — request/response models, validation, and mapping must be 
  written manually.

My choice:
For a production system with multiple consumers (frontend, mobile, third-party 
integrations), I would choose the OpenAPI contract-first approach. The upfront cost 
of maintaining the YAML spec pays off through guaranteed consistency, auto-generated 
documentation, and the ability to parallelize frontend and backend development.

However, for internal-only APIs or rapid prototyping, hand-coded endpoints with 
SmallRye OpenAPI annotations (@Operation, @APIResponse) offer a good middle ground — 
you get Swagger docs generated from code annotations without the build-time generation 
complexity.

In this codebase specifically, I would standardize on one approach. Having two different 
patterns increases cognitive load for developers maintaining the system.
```

---

## Question 2: Testing Strategy

Given the need to balance thorough testing with time and resource constraints, how would you prioritize tests for this project? 

Which types of tests (unit, integration, parameterized, etc.) would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order (highest to lowest):

1. Unit Tests for Domain Use Cases (highest priority)
   These test pure business logic (Create, Archive, Replace validations) in isolation 
   with mocked dependencies. They are fast, deterministic, and catch the most critical 
   bugs — incorrect validation rules, missing edge cases, wrong state transitions. 
   Every business rule from the BRIEFING should have a corresponding unit test.

2. Integration Tests for REST Endpoints
   Tests like WarehouseEndpointIT and StoreTransactionIntegrationTest verify the full 
   request-response cycle including serialization, HTTP status codes, transaction 
   boundaries, and CDI event firing. These catch wiring issues that unit tests miss.

3. Parameterized Tests for Validation Edge Cases
   As demonstrated in WarehouseValidationTest, parameterized tests systematically cover 
   boundary conditions (capacity limits, invalid locations, stock > capacity) without 
   repetitive test methods. High value-to-effort ratio for input validation logic.

4. Concurrency Tests
   Tests like WarehouseConcurrencyIT and the optimistic locking tests are essential for 
   a system handling concurrent warehouse operations. They verify that @Version-based 
   optimistic locking prevents lost updates and that duplicate business unit codes are 
   properly rejected under race conditions.

5. Database Constraint Tests
   Tests like WarehouseTestcontainersIT verify that database-level constraints (unique 
   indexes, NOT NULL) act as a safety net when application-level validation is bypassed.

Ensuring coverage remains effective over time:

- JaCoCo integration (already added) with a minimum line coverage threshold enforced 
  during the build. This prevents coverage from silently degrading as new code is added.

- Require tests for every new use case or business rule change in code review. 
  No PR merges without corresponding test updates.

- Separate fast unit tests (run on every commit) from slower integration tests 
  (run on PR/nightly). This keeps the feedback loop fast without sacrificing coverage.

- Use mutation testing (e.g., PIT) periodically to verify that tests actually catch 
  bugs, not just execute code paths. High line coverage with weak assertions is a 
  false sense of security.

- Maintain a concurrency test suite specifically for operations that modify shared 
  state (archive, replace, stock updates). These are the hardest bugs to reproduce 
  in production and the most valuable to catch early.
```
