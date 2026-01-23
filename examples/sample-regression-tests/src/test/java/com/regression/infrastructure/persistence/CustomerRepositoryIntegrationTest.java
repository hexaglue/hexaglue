package com.regression.infrastructure.persistence;

import com.regression.RegressionTestsApplication;
import com.regression.domain.customer.Customer;
import com.regression.domain.customer.CustomerId;
import com.regression.domain.customer.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CustomerRepositoryAdapter.
 *
 * <p>These tests validate that the generated JPA infrastructure works correctly:
 * <ul>
 *   <li>Entity mapping and persistence</li>
 *   <li>Mapper conversion between domain and JPA entities</li>
 *   <li>Repository method implementations</li>
 *   <li>Specific bug fixes (M12: boolean return types)</li>
 * </ul>
 */
@SpringBootTest(classes = RegressionTestsApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepositoryAdapter repository;

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("should save and retrieve customer by ID")
        void shouldSaveAndRetrieveCustomer() {
            // Given
            Customer customer = Customer.create("John Doe", Email.of("john@example.com"));

            // When
            Customer saved = repository.save(customer);
            var found = repository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("John Doe");
            assertThat(found.get().email().value()).isEqualTo("john@example.com");
            assertThat(found.get().active()).isTrue();
        }

        @Test
        @DisplayName("should find customer by email")
        void shouldFindCustomerByEmail() {
            // Given
            Customer customer = Customer.create("Jane Doe", Email.of("jane@example.com"));
            repository.save(customer);

            // When
            var found = repository.findByEmail(Email.of("jane@example.com"));

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().name()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("should find all customers")
        void shouldFindAllCustomers() {
            // Given
            repository.save(Customer.create("Alice", Email.of("alice@example.com")));
            repository.save(Customer.create("Bob", Email.of("bob@example.com")));

            // When
            var customers = repository.findAll();

            // Then
            assertThat(customers).hasSize(2);
        }

        @Test
        @DisplayName("should delete customer")
        void shouldDeleteCustomer() {
            // Given
            Customer customer = Customer.create("ToDelete", Email.of("delete@example.com"));
            Customer saved = repository.save(customer);

            // When
            repository.delete(saved);
            var found = repository.findById(saved.id());

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("M12: Boolean return type - existsByEmail")
    class BooleanReturnType {

        @Test
        @DisplayName("existsByEmail should return true when email exists")
        void existsByEmailShouldReturnTrueWhenExists() {
            // Given
            Customer customer = Customer.create("Existing", Email.of("exists@example.com"));
            repository.save(customer);

            // When
            boolean exists = repository.existsByEmail(Email.of("exists@example.com"));

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByEmail should return false when email does not exist")
        void existsByEmailShouldReturnFalseWhenNotExists() {
            // When
            boolean exists = repository.existsByEmail(Email.of("notexists@example.com"));

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    class DomainEntityMapping {

        @Test
        @DisplayName("should preserve all domain properties through save/load cycle")
        void shouldPreserveAllDomainProperties() {
            // Given
            Customer customer = Customer.create("Full Test", Email.of("full@example.com"))
                    .deactivate();

            // When
            Customer saved = repository.save(customer);
            var found = repository.findById(saved.id());

            // Then
            assertThat(found).isPresent();
            Customer loaded = found.get();
            assertThat(loaded.id()).isEqualTo(saved.id());
            assertThat(loaded.name()).isEqualTo("Full Test");
            assertThat(loaded.email()).isEqualTo(Email.of("full@example.com"));
            assertThat(loaded.active()).isFalse(); // deactivated
        }
    }
}
