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

package io.hexaglue.plugin.rest;

import io.hexaglue.arch.model.AggregateRoot;
import io.hexaglue.arch.model.DrivingPort;
import io.hexaglue.arch.model.Field;
import io.hexaglue.arch.model.FieldRole;
import io.hexaglue.arch.model.Identifier;
import io.hexaglue.arch.model.Parameter;
import io.hexaglue.arch.model.UseCase;
import io.hexaglue.arch.model.ValueObject;
import io.hexaglue.arch.model.index.DomainIndex;
import io.hexaglue.syntax.TypeRef;
import java.util.List;
import java.util.Set;

/**
 * Provides a complete banking domain model for integration and golden file tests.
 *
 * <p>The model includes three aggregates (Account, Customer, Transfer),
 * three identifiers, two value objects, and three driving ports with
 * realistic use cases exercising most HTTP verb strategies.
 *
 * @since 3.1.0
 */
public final class BankingTestModel {

    /** Base package for the banking domain. */
    public static final String BASE_PACKAGE = "com.acme.banking.core";

    /** API package for generated code. */
    public static final String API_PACKAGE = "com.acme.banking.api";

    /** Model package for domain types. */
    public static final String MODEL_PACKAGE = BASE_PACKAGE + ".model";

    /** Port package for driving ports. */
    public static final String PORT_PACKAGE = BASE_PACKAGE + ".port.in";

    /** Exception package for domain exceptions. */
    public static final String EXCEPTION_PACKAGE = BASE_PACKAGE + ".exception";

    private BankingTestModel() {
        /* prevent instantiation */
    }

    // === Identifiers ===

    /**
     * AccountId identifier wrapping Long.
     *
     * @return the AccountId identifier
     */
    public static Identifier accountId() {
        return TestUseCaseFactory.identifier(MODEL_PACKAGE + ".AccountId", "java.lang.Long");
    }

    /**
     * CustomerId identifier wrapping Long.
     *
     * @return the CustomerId identifier
     */
    public static Identifier customerId() {
        return TestUseCaseFactory.identifier(MODEL_PACKAGE + ".CustomerId", "java.lang.Long");
    }

    /**
     * TransferId identifier wrapping Long.
     *
     * @return the TransferId identifier
     */
    public static Identifier transferId() {
        return TestUseCaseFactory.identifier(MODEL_PACKAGE + ".TransferId", "java.lang.Long");
    }

    // === Value Objects ===

    /**
     * Money value object with amount and currency fields.
     *
     * @return the Money value object
     */
    public static ValueObject money() {
        return TestUseCaseFactory.multiFieldValueObject(
                MODEL_PACKAGE + ".Money",
                List.of(
                        Field.of("amount", TypeRef.of("java.math.BigDecimal")),
                        Field.of("currency", TypeRef.of("java.lang.String"))));
    }

    /**
     * Email single-field value object.
     *
     * @return the Email value object
     */
    public static ValueObject email() {
        return TestUseCaseFactory.singleFieldValueObject(MODEL_PACKAGE + ".Email", "value", "java.lang.String");
    }

    // === Aggregate Roots ===

    /**
     * Account aggregate with 6 fields.
     *
     * @return the Account aggregate root
     */
    public static AggregateRoot account() {
        Field idField = Field.builder("id", TypeRef.of(MODEL_PACKAGE + ".AccountId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field accountNumber = Field.of("accountNumber", TypeRef.of("java.lang.String"));
        Field balance = Field.of("balance", TypeRef.of(MODEL_PACKAGE + ".Money"));
        Field type = Field.of("type", TypeRef.of(MODEL_PACKAGE + ".AccountType"));
        Field active = Field.of("active", TypeRef.of("boolean"));
        Field custId = Field.builder("customerId", TypeRef.of(MODEL_PACKAGE + ".CustomerId"))
                .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                .build();

        return TestUseCaseFactory.aggregateRoot(
                MODEL_PACKAGE + ".Account", idField, List.of(idField, accountNumber, balance, type, active, custId));
    }

    /**
     * Customer aggregate with 3 fields.
     *
     * @return the Customer aggregate root
     */
    public static AggregateRoot customer() {
        Field idField = Field.builder("id", TypeRef.of(MODEL_PACKAGE + ".CustomerId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field name = Field.of("name", TypeRef.of("java.lang.String"));
        Field emailField = Field.of("email", TypeRef.of(MODEL_PACKAGE + ".Email"));

        return TestUseCaseFactory.aggregateRoot(
                MODEL_PACKAGE + ".Customer", idField, List.of(idField, name, emailField));
    }

    /**
     * Transfer aggregate with 5 fields.
     *
     * @return the Transfer aggregate root
     */
    public static AggregateRoot transfer() {
        Field idField = Field.builder("id", TypeRef.of(MODEL_PACKAGE + ".TransferId"))
                .wrappedType(TypeRef.of("java.lang.Long"))
                .roles(Set.of(FieldRole.IDENTITY))
                .build();
        Field fromAcct = Field.builder("fromAccountId", TypeRef.of(MODEL_PACKAGE + ".AccountId"))
                .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                .build();
        Field toAcct = Field.builder("toAccountId", TypeRef.of(MODEL_PACKAGE + ".AccountId"))
                .roles(Set.of(FieldRole.AGGREGATE_REFERENCE))
                .build();
        Field amount = Field.of("amount", TypeRef.of(MODEL_PACKAGE + ".Money"));
        Field status = Field.of("status", TypeRef.of("java.lang.String"));

        return TestUseCaseFactory.aggregateRoot(
                MODEL_PACKAGE + ".Transfer", idField, List.of(idField, fromAcct, toAcct, amount, status));
    }

    // === Domain Index ===

    /**
     * Creates a DomainIndex containing all banking types.
     *
     * @return the domain index
     */
    public static DomainIndex domainIndex() {
        return TestUseCaseFactory.domainIndex(
                account(), customer(), transfer(), accountId(), customerId(), transferId(), money(), email());
    }

    // === Exception TypeRefs ===

    /**
     * TypeRef for AccountNotFoundException.
     *
     * @return the exception type ref
     */
    public static TypeRef accountNotFoundExceptionRef() {
        return TypeRef.of(EXCEPTION_PACKAGE + ".AccountNotFoundException");
    }

    /**
     * TypeRef for InsufficientFundsException.
     *
     * @return the exception type ref
     */
    public static TypeRef insufficientFundsExceptionRef() {
        return TypeRef.of(EXCEPTION_PACKAGE + ".InsufficientFundsException");
    }

    /**
     * TypeRef for TransferRejectedException.
     *
     * @return the exception type ref
     */
    public static TypeRef transferRejectedExceptionRef() {
        return TypeRef.of(EXCEPTION_PACKAGE + ".TransferRejectedException");
    }

    // === Driving Ports ===

    /**
     * AccountUseCases driving port with 6 use cases.
     *
     * <p>Use cases:
     * <ol>
     *   <li>{@code getAccount(AccountId)} — QUERY, GetByIdStrategy</li>
     *   <li>{@code openAccount(CustomerId, AccountType, String)} — COMMAND_QUERY, CreateStrategy</li>
     *   <li>{@code deposit(AccountId, Money)} — COMMAND, SubResourceActionStrategy</li>
     *   <li>{@code withdraw(AccountId, Money)} — COMMAND, SubResourceActionStrategy</li>
     *   <li>{@code closeAccount(AccountId)} — COMMAND, DeleteStrategy</li>
     *   <li>{@code getAllAccounts()} — QUERY, GetCollectionStrategy</li>
     * </ol>
     *
     * @return the AccountUseCases driving port
     */
    public static DrivingPort accountUseCases() {
        TypeRef accountRef = TypeRef.of(MODEL_PACKAGE + ".Account");
        TypeRef accountIdRef = TypeRef.of(MODEL_PACKAGE + ".AccountId");
        TypeRef customerIdRef = TypeRef.of(MODEL_PACKAGE + ".CustomerId");
        TypeRef moneyRef = TypeRef.of(MODEL_PACKAGE + ".Money");
        TypeRef accountTypeRef = TypeRef.of(MODEL_PACKAGE + ".AccountType");

        UseCase getAccount = TestUseCaseFactory.queryWithParams(
                "getAccount", accountRef, List.of(Parameter.of("accountId", accountIdRef)));

        UseCase openAccount = TestUseCaseFactory.commandQueryWithParams(
                "openAccount",
                accountRef,
                List.of(
                        Parameter.of("customerId", customerIdRef),
                        Parameter.of("type", accountTypeRef),
                        Parameter.of("accountNumber", TypeRef.of("java.lang.String"))));

        UseCase deposit = TestUseCaseFactory.commandWithParamsAndExceptions(
                "deposit",
                List.of(Parameter.of("accountId", accountIdRef), Parameter.of("amount", moneyRef)),
                List.of(insufficientFundsExceptionRef()));

        UseCase withdraw = TestUseCaseFactory.commandWithParamsAndExceptions(
                "withdraw",
                List.of(Parameter.of("accountId", accountIdRef), Parameter.of("amount", moneyRef)),
                List.of(insufficientFundsExceptionRef()));

        UseCase closeAccount =
                TestUseCaseFactory.commandWithParams("closeAccount", List.of(Parameter.of("accountId", accountIdRef)));

        UseCase getAllAccounts =
                TestUseCaseFactory.queryWithCollectionReturn("getAllAccounts", MODEL_PACKAGE + ".Account", List.of());

        return TestUseCaseFactory.drivingPort(
                PORT_PACKAGE + ".AccountUseCases",
                List.of(getAccount, openAccount, deposit, withdraw, closeAccount, getAllAccounts));
    }

    /**
     * CustomerUseCases driving port with 3 use cases.
     *
     * <p>Use cases:
     * <ol>
     *   <li>{@code getCustomer(CustomerId)} — QUERY, GetByIdStrategy</li>
     *   <li>{@code createCustomer(String, Email)} — COMMAND_QUERY, CreateStrategy</li>
     *   <li>{@code updateCustomer(CustomerId, String, Email)} — COMMAND, UpdateStrategy</li>
     * </ol>
     *
     * @return the CustomerUseCases driving port
     */
    public static DrivingPort customerUseCases() {
        TypeRef customerRef = TypeRef.of(MODEL_PACKAGE + ".Customer");
        TypeRef customerIdRef = TypeRef.of(MODEL_PACKAGE + ".CustomerId");
        TypeRef emailRef = TypeRef.of(MODEL_PACKAGE + ".Email");

        UseCase getCustomer = TestUseCaseFactory.queryWithParams(
                "getCustomer", customerRef, List.of(Parameter.of("customerId", customerIdRef)));

        UseCase createCustomer = TestUseCaseFactory.commandQueryWithParams(
                "createCustomer",
                customerRef,
                List.of(Parameter.of("name", TypeRef.of("java.lang.String")), Parameter.of("email", emailRef)));

        UseCase updateCustomer = TestUseCaseFactory.commandWithParams(
                "updateCustomer",
                List.of(
                        Parameter.of("customerId", customerIdRef),
                        Parameter.of("name", TypeRef.of("java.lang.String")),
                        Parameter.of("email", emailRef)));

        return TestUseCaseFactory.drivingPort(
                PORT_PACKAGE + ".CustomerUseCases", List.of(getCustomer, createCustomer, updateCustomer));
    }

    /**
     * TransferUseCases driving port with 3 use cases.
     *
     * <p>Use cases:
     * <ol>
     *   <li>{@code getTransfer(TransferId)} — QUERY, GetByIdStrategy</li>
     *   <li>{@code initiateTransfer(AccountId, AccountId, Money)} — COMMAND_QUERY, CreateStrategy</li>
     *   <li>{@code getTransfersByAccount(AccountId)} — QUERY, GetByPropertyStrategy</li>
     * </ol>
     *
     * @return the TransferUseCases driving port
     */
    public static DrivingPort transferUseCases() {
        TypeRef transferRef = TypeRef.of(MODEL_PACKAGE + ".Transfer");
        TypeRef transferIdRef = TypeRef.of(MODEL_PACKAGE + ".TransferId");
        TypeRef accountIdRef = TypeRef.of(MODEL_PACKAGE + ".AccountId");
        TypeRef moneyRef = TypeRef.of(MODEL_PACKAGE + ".Money");

        UseCase getTransfer = TestUseCaseFactory.queryWithParams(
                "getTransfer", transferRef, List.of(Parameter.of("transferId", transferIdRef)));

        UseCase initiateTransfer = TestUseCaseFactory.commandQueryWithParamsAndExceptions(
                "initiateTransfer",
                transferRef,
                List.of(
                        Parameter.of("fromAccountId", accountIdRef),
                        Parameter.of("toAccountId", accountIdRef),
                        Parameter.of("amount", moneyRef)),
                List.of(transferRejectedExceptionRef()));

        UseCase getTransfersByAccount = TestUseCaseFactory.queryWithCollectionReturn(
                "getTransfersByAccount", MODEL_PACKAGE + ".Transfer", List.of(Parameter.of("accountId", accountIdRef)));

        return TestUseCaseFactory.drivingPort(
                PORT_PACKAGE + ".TransferUseCases", List.of(getTransfer, initiateTransfer, getTransfersByAccount));
    }
}
