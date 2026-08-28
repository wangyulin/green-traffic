package com.greentraffic.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.greentraffic.core")
public class AdditionalArchitectureTest {

    @ArchTest
    static final ArchRule coreShouldNotDependOnRocketMQOrSpring = noClasses()
            .that().resideInAPackage("com.greentraffic.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.apache.rocketmq..",
                    "org.springframework.."
            ).allowEmptyShould(true);

    @ArchTest
    static final ArchRule portsNamedPortShouldBeInterfaces = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Port")
            .and().resideInAPackage("com.greentraffic.core..")
            .should().beInterfaces()
            .allowEmptyShould(true);
}
