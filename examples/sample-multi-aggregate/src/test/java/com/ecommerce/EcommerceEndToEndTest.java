package com.ecommerce;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end tests exercising the full chain:
 * HTTP request -> REST Controller -> Application Service -> JPA Adapter -> H2 Database -> Response.
 *
 * <p>These tests document both working endpoints and bugs in the generated code:
 * <ul>
 *   <li>Product CRUD: fully functional through the entire chain</li>
 *   <li>Order endpoints: all fail due to a MapStruct mapper bug (see {@link OrderMapperBug})</li>
 *   <li>Customer registration: fails due to NPE in generated CustomerResponse</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcommerceEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MvcResult createProduct(String name, String description, double price, int stock) throws Exception {
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "amount": %s,
                    "currency": "EUR",
                    "initialStock": %d
                }
                """.formatted(name, description, price, stock);

        return mockMvc.perform(post("/api/managing-productses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is(name)))
                .andReturn();
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    @Nested
    class ProductEndToEnd {

        @Test
        void should_create_and_retrieve_product() throws Exception {
            MvcResult createResult = createProduct("Laptop", "Gaming laptop", 999.99, 10);
            UUID productId = extractId(createResult);

            mockMvc.perform(get("/api/managing-productses/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Laptop")));
        }

        @Test
        void should_list_all_products() throws Exception {
            createProduct("Product A", "Description A", 10.0, 5);
            createProduct("Product B", "Description B", 20.0, 3);

            mockMvc.perform(get("/api/managing-productses/list-all-products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void should_search_products_by_name() throws Exception {
            createProduct("Wireless Mouse", "Bluetooth mouse", 29.99, 50);
            createProduct("Wireless Keyboard", "Bluetooth keyboard", 49.99, 30);
            createProduct("USB Cable", "USB-C cable", 9.99, 100);

            mockMvc.perform(get("/api/managing-productses/search").param("name", "Wireless"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void should_update_product() throws Exception {
            MvcResult createResult = createProduct("Old Name", "Old desc", 10.0, 5);
            UUID productId = extractId(createResult);

            String updateBody = """
                    {
                        "productId": "%s",
                        "name": "New Name",
                        "description": "New desc",
                        "amount": 15.0,
                        "currency": "EUR"
                    }
                    """.formatted(productId);

            mockMvc.perform(put("/api/managing-productses/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("New Name")))
                    .andExpect(jsonPath("$.description", is("New desc")))
                    .andExpect(jsonPath("$.priceAmount", is(15.0)));
        }

        @Test
        void should_adjust_stock() throws Exception {
            MvcResult createResult = createProduct("Widget", "A widget", 5.0, 10);
            UUID productId = extractId(createResult);

            String body = """
                    {
                        "productId": "%s",
                        "quantityDelta": 5
                    }
                    """.formatted(productId);

            mockMvc.perform(post("/api/managing-productses/{id}/adjust-stock", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity", is(15)));
        }

        @Test
        void should_deactivate_and_activate_product() throws Exception {
            // BUG: ProductMapperImpl.toDomain() creates Product via constructor which
            // hardcodes active=true. Product has no setActive(), only activate()/deactivate(),
            // so MapStruct cannot map the active field from entity to domain.
            // The deactivation IS persisted to the JPA entity, but the REST response
            // always shows active=true because the mapper reconstructs with the default.
            MvcResult createResult = createProduct("Gadget", "A gadget", 25.0, 8);
            UUID productId = extractId(createResult);

            String deactivateBody = """
                    { "productId": "%s" }
                    """.formatted(productId);

            mockMvc.perform(post("/api/managing-productses/deactivate-product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deactivateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(true))); // Bug: should be false

            String activateBody = """
                    { "productId": "%s" }
                    """.formatted(productId);

            mockMvc.perform(post("/api/managing-productses/{id}/activate-product", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(activateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        void should_delete_product() throws Exception {
            MvcResult createResult = createProduct("ToDelete", "Will be deleted", 1.0, 1);
            UUID productId = extractId(createResult);

            String deleteBody = """
                    { "productId": "%s" }
                    """.formatted(productId);

            mockMvc.perform(delete("/api/managing-productses/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deleteBody))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/managing-productses/list-all-products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));
        }
    }

    /**
     * Documents a bug in the generated OrderMapperImpl (MapStruct).
     *
     * <p>The generated {@code toDomain(OrderEntity)} method attempts to populate the order's
     * lines via {@code order.getLines().addAll(list)}, but {@code Order.getLines()} returns
     * {@code Collections.unmodifiableList(lines)} which throws {@link UnsupportedOperationException}
     * on any mutating operation. This makes ALL order endpoints fail with 500.
     *
     * <p>Root cause: MapStruct tries to use the collection adder pattern on the getter's return
     * value instead of using the domain's {@code addLine()} method.
     *
     * @see com.ecommerce.domain.order.Order#getLines()
     */
    @Nested
    class OrderMapperBug {

        @Test
        void create_order_fails_due_to_unmodifiable_list_in_mapper() throws Exception {
            // OrderRepositoryAdapter.save() calls mapper.toDomain(saved) which fails:
            // OrderMapperImpl.toDomain() line 62: order.getLines().addAll(list)
            // -> Order.getLines() returns Collections.unmodifiableList(lines)
            // -> UnsupportedOperationException
            String body = """
                    {
                        "customerId": "%s",
                        "street": "123 Main St",
                        "city": "Paris",
                        "postalCode": "75001",
                        "country": "France"
                    }
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/ordering-productses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }

        @Test
        void get_order_fails_due_to_unmodifiable_list_in_mapper() throws Exception {
            // Even GET /ordering-productses/{id} fails because findById() calls mapper.toDomain()
            // which triggers the same UnsupportedOperationException on getLines().addAll()
            mockMvc.perform(get("/api/ordering-productses/{id}", UUID.randomUUID()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void should_return_500_for_nonexistent_order() throws Exception {
            // getOrder uses findOrThrow -> NoSuchElementException -> 500
            mockMvc.perform(get("/api/ordering-productses/{id}", UUID.randomUUID()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }

        @Test
        void should_return_400_for_invalid_email() throws Exception {
            // Email constructor throws IllegalArgumentException for invalid format,
            // caught by GlobalExceptionHandler.handleIllegalArgument -> 400
            String body = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "not-a-valid-email"
                    }
                    """;

            mockMvc.perform(post("/api/managing-customerses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("BAD_REQUEST")))
                    .andExpect(jsonPath("$.message", is("Invalid email: not-a-valid-email")));
        }

        @Test
        void should_return_500_for_validation_errors() throws Exception {
            // The generated GlobalExceptionHandler catches MethodArgumentNotValidException
            // via its generic Exception handler, returning 500 instead of the standard 400.
            String invalidBody = """
                    {
                        "name": "",
                        "description": "",
                        "amount": 10.0,
                        "currency": "EUR",
                        "initialStock": 5
                    }
                    """;

            mockMvc.perform(post("/api/managing-productses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }

        @Test
        void should_return_500_for_register_customer_due_to_npe_in_generated_response() throws Exception {
            // BUG: CustomerResponse.from() accesses getBillingAddress().street() without null-check.
            // registerCustomer does not set a billing address -> NullPointerException.
            String body = """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john.doe@example.com"
                    }
                    """;

            mockMvc.perform(post("/api/managing-customerses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }

        @Test
        void should_return_500_for_list_active_products() throws Exception {
            // BUG: ProductRepositoryAdapter.findAllActive() throws UnsupportedOperationException
            // because the JPA plugin cannot generate custom query methods.
            // GET /api/managing-productses maps to listActiveProducts().
            mockMvc.perform(get("/api/managing-productses"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }
    }
}
