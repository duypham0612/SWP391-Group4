package com.cafe.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.servlet.http.HttpServlet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.cafe", importOptions = ImportOption.DoNotIncludeTests.class)
class MvcArchitectureTest {
    @ArchTest
    static final ArchRule web_must_not_access_database = noClasses()
            .that().resideInAnyPackage("..controller..", "..filter..", "..listener..", "..web..")
            .should().dependOnClassesThat().resideInAnyPackage("..dao..", "java.sql..")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("com.cafe.config.DBConnection");

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_controllers = noClasses()
            .that().resideInAPackage("..controller..").and().areAssignableTo(HttpServlet.class)
            .should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage("..controller..")
                    .and(JavaClass.Predicates.assignableTo(HttpServlet.class)));

    @ArchTest
    static final ArchRule services_must_not_depend_on_web = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..", "..filter..", "..listener..", "..web..");

    @ArchTest
    static final ArchRule daos_must_not_depend_on_upper_layers = noClasses()
            .that().resideInAPackage("..dao..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..web..");

    @ArchTest
    static final ArchRule domain_models_must_stay_independent = noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.sql..", "jakarta.servlet..", "..dao..", "..service..", "..web..");

    @ArchTest
    static final ArchRule common_must_not_depend_on_web_or_jdbc = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "jakarta.servlet..");

    @ArchTest
    static final ArchRule jdbc_is_limited_to_persistence_and_transaction_layers = noClasses()
            .that().resideOutsideOfPackages("..dao..", "..config..", "..service..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..");

    @ArchTest
    static final ArchRule service_packages_must_be_acyclic = SlicesRuleDefinition.slices()
            .matching("com.cafe.service.(*)..")
            .should().beFreeOfCycles();
}
