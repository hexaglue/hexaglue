package com.ecommerce;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
 * <p>Covers all three aggregates (Product, Customer, Order), their CRUD operations,
 * business rules, state machine transitions, and error conditions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcommerceEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Helper methods ─────────────────────────────────────────────

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

    private MvcResult createCustomer(String firstName, String lastName, String email) throws Exception {
        String body = """
                {
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s"
                }
                """.formatted(firstName, lastName, email);

        return mockMvc.perform(post("/api/managing-customerses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.firstName", is(firstName)))
                .andReturn();
    }

    private MvcResult createOrder(UUID customerId) throws Exception {
        String body = """
                {
                    "customerId": "%s",
                    "street": "123 Main St",
                    "city": "Paris",
                    "postalCode": "75001",
                    "country": "France"
                }
                """.formatted(customerId);

        return mockMvc.perform(post("/api/ordering-productses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();
    }

    private MvcResult addLineItem(UUID orderId, UUID productId, int quantity) throws Exception {
        String body = """
                {
                    "orderId": "%s",
                    "productId": "%s",
                    "quantity": %d
                }
                """.formatted(orderId, productId, quantity);

        return mockMvc.perform(post("/api/ordering-productses/add-line-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private UUID extractId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    // ── Product ────────────────────────────────────────────────────

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
            MvcResult createResult = createProduct("Gadget", "A gadget", 25.0, 8);
            UUID productId = extractId(createResult);

            String deactivateBody = """
                    { "productId": "%s" }
                    """.formatted(productId);

            mockMvc.perform(post("/api/managing-productses/deactivate-product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deactivateBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(false)));

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

        @Test
        void should_list_only_active_products() throws Exception {
            createProduct("Active Product", "Should appear", 10.0, 5);
            MvcResult inactiveResult = createProduct("Inactive Product", "Should not appear", 20.0, 3);
            UUID inactiveId = extractId(inactiveResult);

            String deactivateBody = """
                    { "productId": "%s" }
                    """.formatted(inactiveId);

            mockMvc.perform(post("/api/managing-productses/deactivate-product")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deactivateBody))
                    .andExpect(status().isOk());

            // GET / returns only active products
            mockMvc.perform(get("/api/managing-productses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            // GET /list-all-products returns all products regardless of active flag
            mockMvc.perform(get("/api/managing-productses/list-all-products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void should_return_500_when_stock_goes_negative() throws Exception {
            MvcResult createResult = createProduct("Limited", "Low stock", 5.0, 3);
            UUID productId = extractId(createResult);

            String body = """
                    {
                        "productId": "%s",
                        "quantityDelta": -10
                    }
                    """.formatted(productId);

            mockMvc.perform(post("/api/managing-productses/{id}/adjust-stock", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void should_create_product_with_full_response_validation() throws Exception {
            String body = """
                    {
                        "name": "Full Product",
                        "description": "Complete validation",
                        "amount": 49.99,
                        "currency": "EUR",
                        "initialStock": 25
                    }
                    """;

            mockMvc.perform(post("/api/managing-productses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Full Product")))
                    .andExpect(jsonPath("$.description", is("Complete validation")))
                    .andExpect(jsonPath("$.priceAmount", is(49.99)))
                    .andExpect(jsonPath("$.priceCurrency", is("EUR")))
                    .andExpect(jsonPath("$.stockQuantity", is(25)))
                    .andExpect(jsonPath("$.active", is(true)));
        }
    }

    // ── Customer ───────────────────────────────────────────────────

    @Nested
    class CustomerEndToEnd {

        @Test
        void should_register_and_retrieve_customer() throws Exception {
            MvcResult registerResult = createCustomer("John", "Doe", "john.doe@example.com");
            UUID customerId = extractId(registerResult);

            // Verify registration response (CustomerResponse DTO: flattened fields)
            JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
            assert registerJson.get("email").asText().equals("john.doe@example.com");
            assert registerJson.get("billingAddressStreet").isNull();

            // Retrieve by ID (returns raw Customer domain object: nested value objects)
            mockMvc.perform(get("/api/managing-customerses/{id}", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName", is("John")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.email.value", is("john.doe@example.com")))
                    .andExpect(jsonPath("$.billingAddress", nullValue()));
        }

        @Test
        void should_update_customer_profile() throws Exception {
            MvcResult registerResult = createCustomer("John", "Doe", "john@example.com");
            UUID customerId = extractId(registerResult);

            String profileBody = """
                    {
                        "customerId": "%s",
                        "firstName": "Jane",
                        "lastName": "Smith",
                        "email": "jane.smith@example.com"
                    }
                    """.formatted(customerId);

            mockMvc.perform(put("/api/managing-customerses/update-profile/{id}", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(profileBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName", is("Jane")))
                    .andExpect(jsonPath("$.lastName", is("Smith")))
                    .andExpect(jsonPath("$.email", is("jane.smith@example.com")));
        }

        @Test
        void should_update_billing_address() throws Exception {
            MvcResult registerResult = createCustomer("Bob", "Martin", "bob@example.com");
            UUID customerId = extractId(registerResult);

            String addressBody = """
                    {
                        "customerId": "%s",
                        "street": "10 Rue de Rivoli",
                        "city": "Paris",
                        "postalCode": "75001",
                        "country": "France"
                    }
                    """.formatted(customerId);

            mockMvc.perform(put("/api/managing-customerses/{id}", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addressBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billingAddressStreet", is("10 Rue de Rivoli")))
                    .andExpect(jsonPath("$.billingAddressCity", is("Paris")))
                    .andExpect(jsonPath("$.billingAddressPostalCode", is("75001")))
                    .andExpect(jsonPath("$.billingAddressCountry", is("France")));
        }

        @Test
        void should_find_customer_by_email() throws Exception {
            createCustomer("Jane", "Doe", "jane.doe@example.com");

            // findByEmail returns raw Optional<Customer> (nested value objects)
            mockMvc.perform(get("/api/managing-customerses/by-email/{email}", "jane.doe@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName", is("Jane")))
                    .andExpect(jsonPath("$.lastName", is("Doe")))
                    .andExpect(jsonPath("$.email.value", is("jane.doe@example.com")));
        }

        @Test
        void should_list_all_customers() throws Exception {
            createCustomer("Alice", "A", "alice@example.com");
            createCustomer("Bob", "B", "bob@example.com");

            // listAllCustomers returns raw List<Customer>
            mockMvc.perform(get("/api/managing-customerses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void should_delete_customer() throws Exception {
            MvcResult registerResult = createCustomer("ToDelete", "Customer", "delete@example.com");
            UUID customerId = extractId(registerResult);

            String deleteBody = """
                    { "customerId": "%s" }
                    """.formatted(customerId);

            mockMvc.perform(delete("/api/managing-customerses/{id}", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deleteBody))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/managing-customerses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));
        }

        @Test
        void should_return_400_for_invalid_email() throws Exception {
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
    }

    // ── Order ──────────────────────────────────────────────────────

    @Nested
    class OrderEndToEnd {

        @Test
        void should_create_order_with_shipping_address() throws Exception {
            UUID customerId = UUID.randomUUID();

            mockMvc.perform(post("/api/ordering-productses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "customerId": "%s",
                                        "street": "456 Oak Ave",
                                        "city": "Lyon",
                                        "postalCode": "69001",
                                        "country": "France"
                                    }
                                    """.formatted(customerId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.shippingAddressStreet", is("456 Oak Ave")))
                    .andExpect(jsonPath("$.shippingAddressCity", is("Lyon")))
                    .andExpect(jsonPath("$.shippingAddressPostalCode", is("69001")))
                    .andExpect(jsonPath("$.shippingAddressCountry", is("France")))
                    .andExpect(jsonPath("$.lines", empty()));
        }

        @Test
        void should_add_line_items_to_order() throws Exception {
            MvcResult product1Result = createProduct("Book", "Java in Action", 39.99, 50);
            UUID product1Id = extractId(product1Result);
            MvcResult product2Result = createProduct("Pen", "Blue pen", 2.99, 200);
            UUID product2Id = extractId(product2Result);

            MvcResult orderResult = createOrder(UUID.randomUUID());
            UUID orderId = extractId(orderResult);

            addLineItem(orderId, product1Id, 2);
            MvcResult afterSecondLine = addLineItem(orderId, product2Id, 5);

            JsonNode response = objectMapper.readTree(afterSecondLine.getResponse().getContentAsString());
            assert response.get("lines").size() == 2;
        }

        @Test
        void should_remove_line_item_from_order() throws Exception {
            MvcResult product1Result = createProduct("Item A", "First item", 10.0, 10);
            UUID product1Id = extractId(product1Result);
            MvcResult product2Result = createProduct("Item B", "Second item", 20.0, 10);
            UUID product2Id = extractId(product2Result);

            MvcResult orderResult = createOrder(UUID.randomUUID());
            UUID orderId = extractId(orderResult);

            addLineItem(orderId, product1Id, 1);
            addLineItem(orderId, product2Id, 1);

            String removeBody = """
                    {
                        "orderId": "%s",
                        "productId": "%s"
                    }
                    """.formatted(orderId, product1Id);

            mockMvc.perform(post("/api/ordering-productses/remove-line-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(removeBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lines", hasSize(1)));
        }

        @Test
        void should_retrieve_order_by_id() throws Exception {
            UUID customerId = UUID.randomUUID();
            MvcResult orderResult = createOrder(customerId);
            UUID orderId = extractId(orderResult);

            mockMvc.perform(get("/api/ordering-productses/{id}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(orderId.toString())))
                    .andExpect(jsonPath("$.customerId", is(customerId.toString())))
                    .andExpect(jsonPath("$.shippingAddressCity", is("Paris")));
        }

        @Test
        void should_get_customer_orders() throws Exception {
            UUID customerId = UUID.randomUUID();
            createOrder(customerId);
            createOrder(customerId);

            // getCustomerOrders returns raw List<Order> (nested value objects)
            mockMvc.perform(get("/api/ordering-productses/get-customer-orders")
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].status", is("DRAFT")))
                    .andExpect(jsonPath("$[1].status", is("DRAFT")));
        }

        @Test
        void should_confirm_order_with_lines_and_address() throws Exception {
            MvcResult productResult = createProduct("Confirmable", "Product", 10.0, 5);
            UUID productId = extractId(productResult);
            UUID customerId = UUID.randomUUID();
            MvcResult orderResult = createOrder(customerId);
            UUID orderId = extractId(orderResult);

            addLineItem(orderId, productId, 1);

            String confirmBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);

            mockMvc.perform(post("/api/ordering-productses/{id}/confirm-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isOk());

            // Verify status persisted correctly via getCustomerOrders (raw domain objects)
            mockMvc.perform(get("/api/ordering-productses/get-customer-orders")
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("PLACED")));
        }

        @Test
        void should_cancel_draft_order() throws Exception {
            UUID customerId = UUID.randomUUID();
            MvcResult orderResult = createOrder(customerId);
            UUID orderId = extractId(orderResult);

            String cancelBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);

            mockMvc.perform(post("/api/ordering-productses/{id}/cancel-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody))
                    .andExpect(status().isOk());

            // Verify status persisted correctly
            mockMvc.perform(get("/api/ordering-productses/get-customer-orders")
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("CANCELLED")));
        }

        @Test
        void should_cancel_placed_order() throws Exception {
            MvcResult productResult = createProduct("Cancellable", "Product", 10.0, 5);
            UUID productId = extractId(productResult);
            UUID customerId = UUID.randomUUID();
            MvcResult orderResult = createOrder(customerId);
            UUID orderId = extractId(orderResult);

            addLineItem(orderId, productId, 1);

            // Confirm: DRAFT → PLACED
            String confirmBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);
            mockMvc.perform(post("/api/ordering-productses/{id}/confirm-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isOk());

            // Cancel: PLACED → CANCELLED
            String cancelBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);
            mockMvc.perform(post("/api/ordering-productses/{id}/cancel-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelBody))
                    .andExpect(status().isOk());

            // Verify status persisted correctly
            mockMvc.perform(get("/api/ordering-productses/get-customer-orders")
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("CANCELLED")));
        }

        @Test
        void should_return_error_when_confirming_empty_order() throws Exception {
            MvcResult orderResult = createOrder(UUID.randomUUID());
            UUID orderId = extractId(orderResult);

            String confirmBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);

            mockMvc.perform(post("/api/ordering-productses/{id}/confirm-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void should_return_error_when_modifying_placed_order() throws Exception {
            MvcResult productResult = createProduct("Locked", "Product", 10.0, 5);
            UUID productId = extractId(productResult);
            MvcResult orderResult = createOrder(UUID.randomUUID());
            UUID orderId = extractId(orderResult);

            addLineItem(orderId, productId, 1);

            // Confirm: DRAFT → PLACED
            String confirmBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);
            mockMvc.perform(post("/api/ordering-productses/{id}/confirm-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isOk());

            // Try to add a line to a PLACED order → error
            MvcResult product2Result = createProduct("Extra", "Product", 5.0, 10);
            UUID product2Id = extractId(product2Result);

            String addLineBody = """
                    {
                        "orderId": "%s",
                        "productId": "%s",
                        "quantity": 1
                    }
                    """.formatted(orderId, product2Id);

            mockMvc.perform(post("/api/ordering-productses/add-line-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addLineBody))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void should_return_error_when_shipping_unpaid_order() throws Exception {
            MvcResult productResult = createProduct("Unshippable", "Product", 10.0, 5);
            UUID productId = extractId(productResult);
            MvcResult orderResult = createOrder(UUID.randomUUID());
            UUID orderId = extractId(orderResult);
            addLineItem(orderId, productId, 1);

            // Confirm: DRAFT → PLACED
            String confirmBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);
            mockMvc.perform(post("/api/ordering-productses/{id}/confirm-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(confirmBody))
                    .andExpect(status().isOk());

            // Try to ship (requires PAID status, but order is PLACED) → error
            String shipBody = """
                    { "orderId": "%s" }
                    """.formatted(orderId);
            mockMvc.perform(post("/api/ordering-productses/{id}/ship-order", orderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(shipBody))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── Error Handling ─────────────────────────────────────────────

    @Nested
    class ErrorHandling {

        @Test
        void should_return_400_for_validation_errors() throws Exception {
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
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
        }

        @Test
        void should_return_500_for_nonexistent_order() throws Exception {
            mockMvc.perform(get("/api/ordering-productses/{id}", UUID.randomUUID()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")));
        }

        @Test
        void should_return_500_for_nonexistent_product() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            String body = """
                    {
                        "productId": "%s",
                        "name": "Ghost",
                        "description": "Does not exist",
                        "amount": 10.0,
                        "currency": "EUR"
                    }
                    """.formatted(nonExistentId);

            mockMvc.perform(put("/api/managing-productses/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError());
        }
    }
}
