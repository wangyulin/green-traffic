package com.greentraffic.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.greentraffic.core")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule coreShouldNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("com.greentraffic.core..")
            .should().dependOnClassesThat().resideInAPackage("com.greentraffic.infrastructure..")
            .because("Core must be framework-agnostic and not depend on infrastructure");

    @ArchTest
    static final ArchRule coreShouldNotDependOnBootstrap = noClasses()
            .that().resideInAPackage("com.greentraffic.core..")
            .should().dependOnClassesThat().resideInAPackage("com.greentraffic.bootstrap..")
            .because("Core must not depend on bootstrap/composition root");
}
