package com.greentraffic.core;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.greentraffic.core")
public class CoreArchitectureTest {

    @ArchTest
    static final ArchRule coreShouldNotDependOnInfrastructure =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.greentraffic.infrastructure.."
                    );

    @ArchTest
    static final ArchRule coreShouldNotDependOnModel =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.greentraffic.model.."
                    );

    @ArchTest
    static final ArchRule coreShouldNotDependOnSpring =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.."
                    );

    @ArchTest
    static final ArchRule coreShouldNotDependOnPersistence =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.hibernate.."
                    );
}
