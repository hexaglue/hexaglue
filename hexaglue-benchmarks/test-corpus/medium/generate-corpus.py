#!/usr/bin/env python3
"""
Generate additional Java classes for the medium test corpus to reach ~200 types.
"""

import os
from pathlib import Path

BASE_PACKAGE = "com.example.ecommerce"
BASE_DIR = Path("src/main/java/com/example/ecommerce")

# Template for aggregate root
AGGREGATE_TEMPLATE = '''package {package};

import java.time.Instant;

/**
 * Aggregate Root representing {name}.
 */
public class {class_name} {{
    private final {id_type} id;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    public {class_name}({id_type} id, String name) {{
        if (id == null) {{
            throw new IllegalArgumentException("{id_type} cannot be null");
        }}
        if (name == null || name.isBlank()) {{
            throw new IllegalArgumentException("Name cannot be null or blank");
        }}
        this.id = id;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }}

    public {id_type} getId() {{
        return id;
    }}

    public String getName() {{
        return name;
    }}

    public Instant getCreatedAt() {{
        return createdAt;
    }}

    public Instant getUpdatedAt() {{
        return updatedAt;
    }}
}}
'''

# Template for Value Object (ID)
ID_TEMPLATE = '''package {package};

import java.util.UUID;

/**
 * Value Object representing a {name} identifier.
 */
public record {class_name}(UUID value) {{
    public {class_name} {{
        if (value == null) {{
            throw new IllegalArgumentException("{class_name} cannot be null");
        }}
    }}

    public static {class_name} generate() {{
        return new {class_name}(UUID.randomUUID());
    }}

    public static {class_name} of(String value) {{
        return new {class_name}(UUID.fromString(value));
    }}
}}
'''

# Template for simple Value Object
VALUE_OBJECT_TEMPLATE = '''package {package};

/**
 * Value Object representing {description}.
 */
public record {class_name}(String value) {{
    public {class_name} {{
        if (value == null || value.isBlank()) {{
            throw new IllegalArgumentException("{class_name} cannot be null or blank");
        }}
    }}
}}
'''

# Template for Repository (Driven Port)
REPOSITORY_TEMPLATE = '''package {package};

import {domain_package}.{aggregate};
import {domain_package}.{id_type};
import java.util.List;
import java.util.Optional;

/**
 * Driven port (secondary) for {aggregate} persistence.
 */
public interface {class_name} {{
    {aggregate} save({aggregate} entity);

    Optional<{aggregate}> findById({id_type} id);

    List<{aggregate}> findAll();

    void deleteById({id_type} id);

    boolean existsById({id_type} id);
}}
'''

# Template for Service (Driving Port)
SERVICE_TEMPLATE = '''package {package};

import {domain_package}.{id_type};
import {domain_package}.{aggregate};
import java.util.List;

/**
 * Driving port (primary) for {aggregate_lower} operations.
 */
public interface {class_name} {{
    {id_type} create(Create{aggregate}Command command);

    {aggregate} get{aggregate}({id_type} id);

    List<{aggregate}> getAll();

    void update({id_type} id, Update{aggregate}Command command);

    void delete({id_type} id);
}}
'''

# Template for Exception
EXCEPTION_TEMPLATE = '''package {package};

import {domain_package}.{id_type};

/**
 * Exception thrown when {aggregate} is not found.
 */
public class {class_name} extends DomainException {{
    private final {id_type} id;

    public {class_name}({id_type} id) {{
        super("{aggregate} not found: " + id);
        this.id = id;
    }}

    public {id_type} getId() {{
        return id;
    }}
}}
'''

# Template for Command
COMMAND_TEMPLATE = '''package {package};

/**
 * Command for {operation} {aggregate}.
 */
public record {class_name}(
    String name,
    String description
) {{
}}
'''

# Template for Use Case
USECASE_TEMPLATE = '''package {package};

import {port_package}.{service_interface};
import {port_package}.Create{aggregate}Command;
import {port_package}.Update{aggregate}Command;
import {driven_package}.{repository};
import {domain_package}.{aggregate};
import {domain_package}.{id_type};
import java.util.List;

/**
 * Use case implementation for {aggregate} operations.
 */
public class {class_name} implements {service_interface} {{
    private final {repository} repository;

    public {class_name}({repository} repository) {{
        this.repository = repository;
    }}

    @Override
    public {id_type} create(Create{aggregate}Command command) {{
        {aggregate} entity = new {aggregate}(
            {id_type}.generate(),
            command.name()
        );
        {aggregate} saved = repository.save(entity);
        return saved.getId();
    }}

    @Override
    public {aggregate} get{aggregate}({id_type} id) {{
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("{aggregate} not found: " + id));
    }}

    @Override
    public List<{aggregate}> getAll() {{
        return repository.findAll();
    }}

    @Override
    public void update({id_type} id, Update{aggregate}Command command) {{
        {aggregate} entity = get{aggregate}(id);
        repository.save(entity);
    }}

    @Override
    public void delete({id_type} id) {{
        repository.deleteById(id);
    }}
}}
'''

# Domains to generate
DOMAINS = [
    ("Payment", "payment"),
    ("Shipment", "shipment"),
    ("ProductReview", "review"),
    ("Cart", "cart"),
    ("Wishlist", "wishlist"),
    ("Coupon", "promotion"),
    ("Supplier", "supplier"),
    ("Warehouse", "warehouse"),
    ("Invoice", "invoice"),
    ("Return", "returns"),
]

def ensure_dir(path):
    os.makedirs(path, exist_ok=True)

def write_file(path, content):
    with open(path, 'w') as f:
        f.write(content)
    print(f"Created: {path}")

def generate_domain(aggregate, module):
    """Generate complete domain with aggregate, ports, and use cases"""

    id_type = f"{aggregate}Id"
    repository = f"{aggregate}Repository"
    service = f"{aggregate}Service"
    use_case = f"Manage{aggregate}UseCase"
    exception_class = f"{aggregate}NotFoundException"

    domain_pkg = f"{BASE_PACKAGE}.domain.model"
    port_driving_pkg = f"{BASE_PACKAGE}.port.driving"
    port_driven_pkg = f"{BASE_PACKAGE}.port.driven"
    app_pkg = f"{BASE_PACKAGE}.application"
    exc_pkg = f"{BASE_PACKAGE}.domain.exception"

    # Create directories
    domain_dir = BASE_DIR / "domain" / "model"
    port_driving_dir = BASE_DIR / "port" / "driving"
    port_driven_dir = BASE_DIR / "port" / "driven"
    app_dir = BASE_DIR / "application"
    exc_dir = BASE_DIR / "domain" / "exception"

    ensure_dir(domain_dir)
    ensure_dir(port_driving_dir)
    ensure_dir(port_driven_dir)
    ensure_dir(app_dir)
    ensure_dir(exc_dir)

    # Generate ID
    id_content = ID_TEMPLATE.format(
        package=domain_pkg,
        class_name=id_type,
        name=aggregate
    )
    write_file(domain_dir / f"{id_type}.java", id_content)

    # Generate Aggregate
    agg_content = AGGREGATE_TEMPLATE.format(
        package=domain_pkg,
        class_name=aggregate,
        name=f"a {aggregate}",
        id_type=id_type
    )
    write_file(domain_dir / f"{aggregate}.java", agg_content)

    # Generate Repository
    repo_content = REPOSITORY_TEMPLATE.format(
        package=port_driven_pkg,
        class_name=repository,
        aggregate=aggregate,
        id_type=id_type,
        domain_package=domain_pkg
    )
    write_file(port_driven_dir / f"{repository}.java", repo_content)

    # Generate Commands
    create_cmd = COMMAND_TEMPLATE.format(
        package=port_driving_pkg,
        class_name=f"Create{aggregate}Command",
        operation="creating",
        aggregate=aggregate
    )
    write_file(port_driving_dir / f"Create{aggregate}Command.java", create_cmd)

    update_cmd = COMMAND_TEMPLATE.format(
        package=port_driving_pkg,
        class_name=f"Update{aggregate}Command",
        operation="updating",
        aggregate=aggregate
    )
    write_file(port_driving_dir / f"Update{aggregate}Command.java", update_cmd)

    # Generate Service Interface
    service_content = SERVICE_TEMPLATE.format(
        package=port_driving_pkg,
        class_name=service,
        aggregate=aggregate,
        aggregate_lower=aggregate.lower(),
        id_type=id_type,
        domain_package=domain_pkg
    )
    write_file(port_driving_dir / f"{service}.java", service_content)

    # Generate Use Case
    usecase_content = USECASE_TEMPLATE.format(
        package=app_pkg,
        class_name=use_case,
        aggregate=aggregate,
        id_type=id_type,
        repository=repository,
        service_interface=service,
        port_package=port_driving_pkg,
        driven_package=port_driven_pkg,
        domain_package=domain_pkg
    )
    write_file(app_dir / f"{use_case}.java", usecase_content)

    # Generate Exception
    exc_content = EXCEPTION_TEMPLATE.format(
        package=exc_pkg,
        class_name=exception_class,
        aggregate=aggregate,
        id_type=id_type,
        domain_package=domain_pkg
    )
    write_file(exc_dir / f"{exception_class}.java", exc_content)

# Generate additional value objects
VALUE_OBJECTS = [
    ("CardNumber", "a credit card number"),
    ("CardholderName", "a cardholder name"),
    ("CVV", "a card CVV"),
    ("ExpiryDate", "a card expiry date"),
    ("ShipmentStatus", "a shipment status"),
    ("Carrier", "a shipping carrier"),
    ("Rating", "a product rating"),
    ("ReviewText", "review text content"),
    ("ReviewId", "a review identifier (UUID)"),
    ("CartId", "a shopping cart identifier (UUID)"),
    ("WishlistId", "a wishlist identifier (UUID)"),
    ("CouponId", "a coupon identifier (UUID)"),
    ("CouponCode", "a coupon code"),
    ("SupplierId", "a supplier identifier (UUID)"),
    ("SupplierName", "a supplier name"),
    ("WarehouseId", "a warehouse identifier (UUID)"),
    ("WarehouseCode", "a warehouse code"),
    ("InvoiceId", "an invoice identifier (UUID)"),
    ("InvoiceNumber", "an invoice number"),
    ("ReturnId", "a return identifier (UUID)"),
    ("ReturnReason", "a return reason"),
]

def generate_value_objects():
    domain_dir = BASE_DIR / "domain" / "model"
    ensure_dir(domain_dir)

    for vo_name, description in VALUE_OBJECTS:
        if "UUID" in description:
            # Use ID template for UUID-based VOs
            content = ID_TEMPLATE.format(
                package=f"{BASE_PACKAGE}.domain.model",
                class_name=vo_name,
                name=vo_name
            )
        else:
            content = VALUE_OBJECT_TEMPLATE.format(
                package=f"{BASE_PACKAGE}.domain.model",
                class_name=vo_name,
                description=description
            )
        write_file(domain_dir / f"{vo_name}.java", content)

if __name__ == "__main__":
    print("Generating medium test corpus...")

    # Generate each domain
    for aggregate, module in DOMAINS:
        print(f"\nGenerating domain: {aggregate}")
        generate_domain(aggregate, module)

    # Generate additional value objects
    print("\nGenerating additional value objects...")
    generate_value_objects()

    print("\nDone! Run 'mvn compile' to verify.")
