package io.hexaglue.core.frontend.spoon;

import static org.assertj.core.api.Assertions.*;

import io.hexaglue.core.frontend.*;
import io.hexaglue.core.frontend.JavaFrontend.JavaAnalysisInput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpoonFrontendTest {

    @TempDir
    Path tempDir;

    private SpoonFrontend frontend;

    @BeforeEach
    void setUp() {
        frontend = new SpoonFrontend();
    }

    @Test
    void shouldParseSimpleClass() throws IOException {
        // Given
        writeSource(
                "com/example/domain/Customer.java",
                """
                package com.example.domain;

                public class Customer {
                    private String name;
                    private int age;

                    public Customer(String name, int age) {
                        this.name = name;
                        this.age = age;
                    }

                    public String getName() {
                        return name;
                    }

                    public int getAge() {
                        return age;
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        // When
        JavaSemanticModel model = frontend.build(input);

        // Then
        List<JavaType> types = model.types().toList();
        assertThat(types).hasSize(1);

        JavaType customer = types.get(0);
        assertThat(customer.qualifiedName()).isEqualTo("com.example.domain.Customer");
        assertThat(customer.simpleName()).isEqualTo("Customer");
        assertThat(customer.form()).isEqualTo(JavaForm.CLASS);
        assertThat(customer.modifiers()).contains(JavaModifier.PUBLIC);
    }

    @Test
    void shouldParseFields() throws IOException {
        writeSource(
                "com/example/domain/Order.java",
                """
                package com.example.domain;

                import java.util.UUID;

                public class Order {
                    private final UUID id;
                    private String description;
                    protected int quantity;
                    public static final String TYPE = "ORDER";

                    public Order(UUID id) {
                        this.id = id;
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType order = model.types().findFirst().orElseThrow();

        List<JavaField> fields = order.fields();
        assertThat(fields).hasSize(4);

        // Check field by name
        JavaField idField = fields.stream()
                .filter(f -> f.simpleName().equals("id"))
                .findFirst()
                .orElseThrow();
        assertThat(idField.modifiers()).containsExactlyInAnyOrder(JavaModifier.PRIVATE, JavaModifier.FINAL);
        assertThat(idField.type().rawQualifiedName()).isEqualTo("java.util.UUID");

        JavaField typeField = fields.stream()
                .filter(f -> f.simpleName().equals("TYPE"))
                .findFirst()
                .orElseThrow();
        assertThat(typeField.modifiers())
                .containsExactlyInAnyOrder(JavaModifier.PUBLIC, JavaModifier.STATIC, JavaModifier.FINAL);
    }

    @Test
    void shouldParseMethods() throws IOException {
        writeSource(
                "com/example/domain/Calculator.java",
                """
                package com.example.domain;

                public class Calculator {
                    public int add(int a, int b) {
                        return a + b;
                    }

                    private void reset() {}

                    protected String format(String pattern, Object... args) {
                        return String.format(pattern, args);
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType calculator = model.types().findFirst().orElseThrow();

        List<JavaMethod> methods = calculator.methods();
        assertThat(methods).hasSize(3);

        JavaMethod addMethod = methods.stream()
                .filter(m -> m.simpleName().equals("add"))
                .findFirst()
                .orElseThrow();
        assertThat(addMethod.returnType().rawQualifiedName()).isEqualTo("int");
        assertThat(addMethod.parameters()).hasSize(2);
        assertThat(addMethod.modifiers()).contains(JavaModifier.PUBLIC);

        JavaMethod formatMethod = methods.stream()
                .filter(m -> m.simpleName().equals("format"))
                .findFirst()
                .orElseThrow();
        assertThat(formatMethod.parameters()).hasSize(2);
    }

    @Test
    void shouldParseConstructors() throws IOException {
        writeSource(
                "com/example/domain/Product.java",
                """
                package com.example.domain;

                public class Product {
                    private String name;
                    private double price;

                    public Product() {}

                    public Product(String name) {
                        this.name = name;
                    }

                    public Product(String name, double price) {
                        this.name = name;
                        this.price = price;
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType product = model.types().findFirst().orElseThrow();

        List<JavaConstructor> constructors = product.constructors();
        assertThat(constructors).hasSize(3);

        // Verify constructor parameter counts
        Set<Integer> paramCounts =
                constructors.stream().map(c -> c.parameters().size()).collect(Collectors.toSet());
        assertThat(paramCounts).containsExactlyInAnyOrder(0, 1, 2);
    }

    @Test
    void shouldParseInterface() throws IOException {
        writeSource(
                "com/example/ports/CustomerRepository.java",
                """
                package com.example.ports;

                import java.util.Optional;

                public interface CustomerRepository {
                    void save(Object customer);
                    Optional<Object> findById(String id);
                    void deleteById(String id);
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType repo = model.types().findFirst().orElseThrow();

        assertThat(repo.form()).isEqualTo(JavaForm.INTERFACE);
        assertThat(repo.isInterface()).isTrue();
        assertThat(repo.methods()).hasSize(3);
    }

    @Test
    void shouldParseRecord() throws IOException {
        writeSource(
                "com/example/domain/CustomerId.java",
                """
                package com.example.domain;

                import java.util.UUID;

                public record CustomerId(UUID value) {
                    public static CustomerId generate() {
                        return new CustomerId(UUID.randomUUID());
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType customerId = model.types().findFirst().orElseThrow();

        assertThat(customerId.form()).isEqualTo(JavaForm.RECORD);
        assertThat(customerId.isRecord()).isTrue();
        // Records have implicit fields for components
        assertThat(customerId.fields()).hasSize(1);
    }

    @Test
    void shouldParseEnum() throws IOException {
        writeSource(
                "com/example/domain/OrderStatus.java",
                """
                package com.example.domain;

                public enum OrderStatus {
                    PENDING,
                    CONFIRMED,
                    SHIPPED,
                    DELIVERED;

                    public boolean isTerminal() {
                        return this == DELIVERED;
                    }
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType status = model.types().findFirst().orElseThrow();

        assertThat(status.form()).isEqualTo(JavaForm.ENUM);
        assertThat(status.isEnum()).isTrue();
    }

    @Test
    void shouldParseAnnotationType() throws IOException {
        writeSource(
                "com/example/domain/Marker.java",
                """
                package com.example.domain;

                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;

                @Retention(RetentionPolicy.RUNTIME)
                public @interface Marker {
                    String value() default "";
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType marker = model.types().findFirst().orElseThrow();

        assertThat(marker.form()).isEqualTo(JavaForm.ANNOTATION);
    }

    @Test
    void shouldParseAnnotationsOnType() throws IOException {
        writeSource(
                "com/example/domain/Deprecated.java",
                """
                package com.example.domain;

                @java.lang.Deprecated
                @SuppressWarnings("unused")
                public class Deprecated {
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType type = model.types().findFirst().orElseThrow();

        List<JavaAnnotation> annotations = type.annotations();
        assertThat(annotations).hasSizeGreaterThanOrEqualTo(2);

        boolean hasDeprecated = annotations.stream()
                .anyMatch(a -> a.annotationType().rawQualifiedName().equals("java.lang.Deprecated"));
        assertThat(hasDeprecated).isTrue();
    }

    @Test
    void shouldParseGenericTypes() throws IOException {
        writeSource(
                "com/example/domain/Container.java",
                """
                package com.example.domain;

                import java.util.List;
                import java.util.Map;
                import java.util.Optional;

                public class Container {
                    private List<String> items;
                    private Map<String, Integer> counts;
                    private Optional<Double> value;
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType container = model.types().findFirst().orElseThrow();

        List<JavaField> fields = container.fields();

        JavaField itemsField = fields.stream()
                .filter(f -> f.simpleName().equals("items"))
                .findFirst()
                .orElseThrow();
        assertThat(itemsField.type().rawQualifiedName()).isEqualTo("java.util.List");
        assertThat(itemsField.type().arguments()).hasSize(1);
        assertThat(itemsField.type().isCollectionLike()).isTrue();

        JavaField valueField = fields.stream()
                .filter(f -> f.simpleName().equals("value"))
                .findFirst()
                .orElseThrow();
        assertThat(valueField.type().isOptionalLike()).isTrue();
    }

    @Test
    void shouldParseArrayTypes() throws IOException {
        writeSource(
                "com/example/domain/ArrayHolder.java",
                """
                package com.example.domain;

                public class ArrayHolder {
                    private int[] numbers;
                    private String[][] matrix;
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType holder = model.types().findFirst().orElseThrow();

        List<JavaField> fields = holder.fields();

        JavaField numbersField = fields.stream()
                .filter(f -> f.simpleName().equals("numbers"))
                .findFirst()
                .orElseThrow();
        assertThat(numbersField.type().isArray()).isTrue();
        assertThat(numbersField.type().arrayDimensions()).isEqualTo(1);

        JavaField matrixField = fields.stream()
                .filter(f -> f.simpleName().equals("matrix"))
                .findFirst()
                .orElseThrow();
        assertThat(matrixField.type().isArray()).isTrue();
        assertThat(matrixField.type().arrayDimensions()).isEqualTo(2);
    }

    @Test
    void shouldParseSuperclassAndInterfaces() throws IOException {
        writeSource(
                "com/example/domain/BaseEntity.java",
                """
                package com.example.domain;

                public abstract class BaseEntity {
                }
                """);

        writeSource(
                "com/example/domain/Identifiable.java",
                """
                package com.example.domain;

                public interface Identifiable {
                }
                """);

        writeSource(
                "com/example/domain/User.java",
                """
                package com.example.domain;

                import java.io.Serializable;

                public class User extends BaseEntity implements Identifiable, Serializable {
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType user = model.types()
                .filter(t -> t.simpleName().equals("User"))
                .findFirst()
                .orElseThrow();

        Optional<TypeRef> superType = user.superType();
        assertThat(superType).isPresent();
        assertThat(superType.get().rawQualifiedName()).isEqualTo("com.example.domain.BaseEntity");

        List<TypeRef> interfaces = user.interfaces();
        assertThat(interfaces).hasSize(2);

        Set<String> interfaceNames =
                interfaces.stream().map(TypeRef::rawQualifiedName).collect(Collectors.toSet());
        assertThat(interfaceNames).containsExactlyInAnyOrder("com.example.domain.Identifiable", "java.io.Serializable");
    }

    @Test
    void shouldFilterByBasePackage() throws IOException {
        writeSource(
                "com/example/domain/InPackage.java",
                """
                package com.example.domain;
                public class InPackage {}
                """);

        writeSource(
                "com/other/OutOfPackage.java",
                """
                package com.other;
                public class OutOfPackage {}
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        List<String> typeNames = model.types().map(JavaType::qualifiedName).toList();

        assertThat(typeNames).containsExactly("com.example.domain.InPackage");
        assertThat(typeNames).doesNotContain("com.other.OutOfPackage");
    }

    @Test
    void shouldProvideSourceRef() throws IOException {
        writeSource(
                "com/example/domain/Located.java",
                """
                package com.example.domain;

                public class Located {
                    private String field;
                }
                """);

        JavaAnalysisInput input = new JavaAnalysisInput(List.of(tempDir), List.of(), 17, "com.example");

        JavaSemanticModel model = frontend.build(input);
        JavaType located = model.types().findFirst().orElseThrow();

        Optional<SourceRef> sourceRef = located.sourceRef();
        assertThat(sourceRef).isPresent();
        assertThat(sourceRef.get().filePath()).endsWith("Located.java");
        assertThat(sourceRef.get().lineStart()).isGreaterThan(0);
    }

    private void writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
