package com.smarttourplanner.planner;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class PlannerArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.smarttourplanner");

    @Test
    void plannerStaysFreeOfFrameworksAndOtherModules() {
        noClasses().that().resideInAPackage("..planner..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "tools.jackson..",
                        "java.sql..", "com.smarttourplanner.catalog..", "com.smarttourplanner.service..")
                .check(classes);
    }

    @Test
    void catalogDoesNotDependOnThePagesLayer() {
        noClasses().that().resideInAPackage("..catalog..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.smarttourplanner.service..", "com.smarttourplanner.controller..",
                        "com.smarttourplanner.model..")
                .check(classes);
    }
}
