package com.example.myhexagonalpokedex.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.myhexagonalpokedex")
class DomainPackageRuleTest {

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_depend_on_classes_inside_pokeapiadapter_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..pokeapiadapter..");

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_access_classes_inside_pokeapiadapter_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .accessClassesThat()
                    .resideInAnyPackage("..pokeapiadapter..");

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_depend_on_classes_inside_postresadapter_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..postgresadapter..");

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_access_classes_inside_postresadapter_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .accessClassesThat()
                    .resideInAnyPackage("..postgresadapter..");

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_depend_on_classes_inside_application_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..");

    @ArchTest
    public static final ArchRule no_classes_inside_domain_package_should_access_classes_inside_application_package =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .accessClassesThat()
                    .resideInAnyPackage("..application..");
}
