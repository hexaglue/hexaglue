/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.spi.ir;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TypeRef}.
 */
class TypeRefTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTest {

        @Test
        @DisplayName("of() should create simple type reference")
        void ofCreatesSimpleTypeRef() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.qualifiedName()).isEqualTo("com.example.Order");
            assertThat(typeRef.simpleName()).isEqualTo("Order");
            assertThat(typeRef.typeArguments()).isEmpty();
            assertThat(typeRef.primitive()).isFalse();
            assertThat(typeRef.array()).isFalse();
            assertThat(typeRef.cardinality()).isEqualTo(Cardinality.SINGLE);
        }

        @Test
        @DisplayName("of() should extract simple name from qualified name")
        void ofExtractsSimpleName() {
            TypeRef typeRef = TypeRef.of("java.util.UUID");

            assertThat(typeRef.simpleName()).isEqualTo("UUID");
        }

        @Test
        @DisplayName("of() should handle class without package")
        void ofHandlesNoPackage() {
            TypeRef typeRef = TypeRef.of("Order");

            assertThat(typeRef.qualifiedName()).isEqualTo("Order");
            assertThat(typeRef.simpleName()).isEqualTo("Order");
        }

        @Test
        @DisplayName("primitive() should create primitive type reference")
        void primitiveCreatesPrimitiveTypeRef() {
            TypeRef typeRef = TypeRef.primitive("int");

            assertThat(typeRef.qualifiedName()).isEqualTo("int");
            assertThat(typeRef.simpleName()).isEqualTo("int");
            assertThat(typeRef.primitive()).isTrue();
            assertThat(typeRef.cardinality()).isEqualTo(Cardinality.SINGLE);
        }

        @Test
        @DisplayName("parameterized() should create parameterized type with inferred cardinality")
        void parameterizedCreatesWithInferredCardinality() {
            TypeRef elementType = TypeRef.of("com.example.Order");
            TypeRef listType = TypeRef.parameterized("java.util.List", elementType);

            assertThat(listType.qualifiedName()).isEqualTo("java.util.List");
            assertThat(listType.simpleName()).isEqualTo("List");
            assertThat(listType.typeArguments()).hasSize(1);
            assertThat(listType.typeArguments().get(0)).isEqualTo(elementType);
            assertThat(listType.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }

        @Test
        @DisplayName("parameterized() should infer OPTIONAL cardinality for Optional types")
        void parameterizedInfersOptionalCardinality() {
            TypeRef optionalType = TypeRef.parameterized("java.util.Optional", TypeRef.of("java.lang.String"));

            assertThat(optionalType.cardinality()).isEqualTo(Cardinality.OPTIONAL);
        }

        @Test
        @DisplayName("array() should create array type reference")
        void arrayCreatesArrayTypeRef() {
            TypeRef componentType = TypeRef.of("com.example.Order");
            TypeRef arrayType = TypeRef.array(componentType, 2);

            assertThat(arrayType.array()).isTrue();
            assertThat(arrayType.arrayDimensions()).isEqualTo(2);
            assertThat(arrayType.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }
    }

    @Nested
    @DisplayName("isParameterized()")
    class IsParameterizedTest {

        @Test
        @DisplayName("should return false for simple types")
        void returnsFalseForSimpleTypes() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.isParameterized()).isFalse();
        }

        @Test
        @DisplayName("should return true for parameterized types")
        void returnsTrueForParameterizedTypes() {
            TypeRef listType = TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Order"));

            assertThat(listType.isParameterized()).isTrue();
        }
    }

    @Nested
    @DisplayName("isOptionalLike()")
    class IsOptionalLikeTest {

        @Test
        @DisplayName("should return true for java.util.Optional")
        void returnsTrueForOptional() {
            TypeRef optionalType = TypeRef.of("java.util.Optional");

            assertThat(optionalType.isOptionalLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for OptionalInt")
        void returnsTrueForOptionalInt() {
            TypeRef optionalInt = TypeRef.of("java.util.OptionalInt");

            assertThat(optionalInt.isOptionalLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for OptionalLong")
        void returnsTrueForOptionalLong() {
            TypeRef optionalLong = TypeRef.of("java.util.OptionalLong");

            assertThat(optionalLong.isOptionalLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for OptionalDouble")
        void returnsTrueForOptionalDouble() {
            TypeRef optionalDouble = TypeRef.of("java.util.OptionalDouble");

            assertThat(optionalDouble.isOptionalLike()).isTrue();
        }

        @Test
        @DisplayName("should return false for non-optional types")
        void returnsFalseForNonOptional() {
            TypeRef stringType = TypeRef.of("java.lang.String");

            assertThat(stringType.isOptionalLike()).isFalse();
        }
    }

    @Nested
    @DisplayName("isCollectionLike()")
    class IsCollectionLikeTest {

        @Test
        @DisplayName("should return true for List")
        void returnsTrueForList() {
            TypeRef listType = TypeRef.of("java.util.List");

            assertThat(listType.isCollectionLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for Set")
        void returnsTrueForSet() {
            TypeRef setType = TypeRef.of("java.util.Set");

            assertThat(setType.isCollectionLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for Collection")
        void returnsTrueForCollection() {
            TypeRef collectionType = TypeRef.of("java.util.Collection");

            assertThat(collectionType.isCollectionLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for ArrayList")
        void returnsTrueForArrayList() {
            TypeRef arrayListType = TypeRef.of("java.util.ArrayList");

            assertThat(arrayListType.isCollectionLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for arrays")
        void returnsTrueForArrays() {
            TypeRef arrayType = TypeRef.array(TypeRef.of("java.lang.String"), 1);

            assertThat(arrayType.isCollectionLike()).isTrue();
        }

        @Test
        @DisplayName("should return false for non-collection types")
        void returnsFalseForNonCollection() {
            TypeRef stringType = TypeRef.of("java.lang.String");

            assertThat(stringType.isCollectionLike()).isFalse();
        }
    }

    @Nested
    @DisplayName("isMapLike()")
    class IsMapLikeTest {

        @Test
        @DisplayName("should return true for Map")
        void returnsTrueForMap() {
            TypeRef mapType = TypeRef.of("java.util.Map");

            assertThat(mapType.isMapLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for HashMap")
        void returnsTrueForHashMap() {
            TypeRef hashMapType = TypeRef.of("java.util.HashMap");

            assertThat(hashMapType.isMapLike()).isTrue();
        }

        @Test
        @DisplayName("should return true for TreeMap")
        void returnsTrueForTreeMap() {
            TypeRef treeMapType = TypeRef.of("java.util.TreeMap");

            assertThat(treeMapType.isMapLike()).isTrue();
        }

        @Test
        @DisplayName("should return false for non-map types")
        void returnsFalseForNonMap() {
            TypeRef listType = TypeRef.of("java.util.List");

            assertThat(listType.isMapLike()).isFalse();
        }
    }

    @Nested
    @DisplayName("firstArgument()")
    class FirstArgumentTest {

        @Test
        @DisplayName("should return first type argument for parameterized types")
        void returnsFirstArgumentForParameterized() {
            TypeRef elementType = TypeRef.of("com.example.Order");
            TypeRef listType = TypeRef.parameterized("java.util.List", elementType);

            assertThat(listType.firstArgument()).isEqualTo(elementType);
        }

        @Test
        @DisplayName("should return null for non-parameterized types")
        void returnsNullForNonParameterized() {
            TypeRef simpleType = TypeRef.of("com.example.Order");

            assertThat(simpleType.firstArgument()).isNull();
        }
    }

    @Nested
    @DisplayName("unwrapElement()")
    class UnwrapElementTest {

        @Test
        @DisplayName("should unwrap Optional to get element type")
        void unwrapsOptional() {
            TypeRef innerType = TypeRef.of("com.example.Order");
            TypeRef optionalType = TypeRef.parameterized("java.util.Optional", innerType);

            TypeRef unwrapped = optionalType.unwrapElement();

            assertThat(unwrapped).isEqualTo(innerType);
        }

        @Test
        @DisplayName("should unwrap List to get element type")
        void unwrapsList() {
            TypeRef innerType = TypeRef.of("com.example.Order");
            TypeRef listType = TypeRef.parameterized("java.util.List", innerType);

            TypeRef unwrapped = listType.unwrapElement();

            assertThat(unwrapped).isEqualTo(innerType);
        }

        @Test
        @DisplayName("should unwrap Set to get element type")
        void unwrapsSet() {
            TypeRef innerType = TypeRef.of("com.example.Order");
            TypeRef setType = TypeRef.parameterized("java.util.Set", innerType);

            TypeRef unwrapped = setType.unwrapElement();

            assertThat(unwrapped).isEqualTo(innerType);
        }

        @Test
        @DisplayName("should return self for non-wrapper types")
        void returnsSelfForNonWrapper() {
            TypeRef simpleType = TypeRef.of("com.example.Order");

            TypeRef unwrapped = simpleType.unwrapElement();

            assertThat(unwrapped).isSameAs(simpleType);
        }

        @Test
        @DisplayName("should return self for non-parameterized collection types")
        void returnsSelfForNonParameterizedCollection() {
            TypeRef rawList = TypeRef.of("java.util.List");

            TypeRef unwrapped = rawList.unwrapElement();

            assertThat(unwrapped).isSameAs(rawList);
        }
    }

    @Nested
    @DisplayName("is()")
    class IsTest {

        @Test
        @DisplayName("should return true when qualified names match")
        void returnsTrueWhenNamesMatch() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.is("com.example.Order")).isTrue();
        }

        @Test
        @DisplayName("should return false when qualified names differ")
        void returnsFalseWhenNamesDiffer() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.is("com.example.Customer")).isFalse();
        }
    }

    @Nested
    @DisplayName("isCollectionOf()")
    class IsCollectionOfTest {

        @Test
        @DisplayName("should return true for matching collection element type")
        void returnsTrueForMatchingElementType() {
            TypeRef listOfOrders = TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Order"));

            assertThat(listOfOrders.isCollectionOf("com.example.Order")).isTrue();
        }

        @Test
        @DisplayName("should return false for non-matching element type")
        void returnsFalseForNonMatchingElementType() {
            TypeRef listOfOrders = TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Order"));

            assertThat(listOfOrders.isCollectionOf("com.example.Customer")).isFalse();
        }

        @Test
        @DisplayName("should return false for non-collection types")
        void returnsFalseForNonCollection() {
            TypeRef simpleType = TypeRef.of("com.example.Order");

            assertThat(simpleType.isCollectionOf("com.example.Order")).isFalse();
        }

        @Test
        @DisplayName("should return false for non-parameterized collection")
        void returnsFalseForRawCollection() {
            TypeRef rawList = TypeRef.of("java.util.List");

            assertThat(rawList.isCollectionOf("com.example.Order")).isFalse();
        }
    }

    @Nested
    @DisplayName("requiresImport()")
    class RequiresImportTest {

        @Test
        @DisplayName("should return false for primitive types")
        void returnsFalseForPrimitives() {
            TypeRef intType = TypeRef.primitive("int");

            assertThat(intType.requiresImport()).isFalse();
        }

        @Test
        @DisplayName("should return false for java.lang types")
        void returnsFalseForJavaLang() {
            TypeRef stringType = TypeRef.of("java.lang.String");

            assertThat(stringType.requiresImport()).isFalse();
        }

        @Test
        @DisplayName("should return true for java.lang nested types")
        void returnsTrueForJavaLangNested() {
            TypeRef nestedType = TypeRef.of("java.lang.ProcessBuilder.Redirect");

            assertThat(nestedType.requiresImport()).isTrue();
        }

        @Test
        @DisplayName("should return true for java.util types")
        void returnsTrueForJavaUtil() {
            TypeRef listType = TypeRef.of("java.util.List");

            assertThat(listType.requiresImport()).isTrue();
        }

        @Test
        @DisplayName("should return true for custom types")
        void returnsTrueForCustomTypes() {
            TypeRef customType = TypeRef.of("com.example.Order");

            assertThat(customType.requiresImport()).isTrue();
        }
    }

    @Nested
    @DisplayName("packageName()")
    class PackageNameTest {

        @Test
        @DisplayName("should return package name for qualified type")
        void returnsPackageName() {
            TypeRef typeRef = TypeRef.of("com.example.domain.Order");

            assertThat(typeRef.packageName()).isEqualTo("com.example.domain");
        }

        @Test
        @DisplayName("should return empty string for primitives")
        void returnsEmptyForPrimitives() {
            TypeRef intType = TypeRef.primitive("int");

            assertThat(intType.packageName()).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for types without package")
        void returnsEmptyForNoPackage() {
            TypeRef simpleType = TypeRef.of("Order");

            assertThat(simpleType.packageName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cardinality inference")
    class CardinalityInferenceTest {

        @Test
        @DisplayName("should infer SINGLE for regular types")
        void infersSingleForRegular() {
            TypeRef typeRef = TypeRef.of("com.example.Order");

            assertThat(typeRef.cardinality()).isEqualTo(Cardinality.SINGLE);
        }

        @Test
        @DisplayName("should infer OPTIONAL for Optional types via parameterized()")
        void infersOptionalForOptional() {
            TypeRef optionalType = TypeRef.parameterized("java.util.Optional", TypeRef.of("java.lang.String"));

            assertThat(optionalType.cardinality()).isEqualTo(Cardinality.OPTIONAL);
        }

        @Test
        @DisplayName("should infer COLLECTION for List types via parameterized()")
        void infersCollectionForList() {
            TypeRef listType = TypeRef.parameterized("java.util.List", TypeRef.of("com.example.Order"));

            assertThat(listType.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }

        @Test
        @DisplayName("should infer COLLECTION for Set types via parameterized()")
        void infersCollectionForSet() {
            TypeRef setType = TypeRef.parameterized("java.util.Set", TypeRef.of("com.example.Order"));

            assertThat(setType.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }

        @Test
        @DisplayName("should infer COLLECTION for arrays")
        void infersCollectionForArrays() {
            TypeRef arrayType = TypeRef.array(TypeRef.of("java.lang.String"), 1);

            assertThat(arrayType.cardinality()).isEqualTo(Cardinality.COLLECTION);
        }
    }
}
