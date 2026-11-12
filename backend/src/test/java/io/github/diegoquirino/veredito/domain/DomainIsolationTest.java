package io.github.diegoquirino.veredito.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Constitution Principle III: domain rules depend on nothing but the JDK and themselves. */
@AnalyzeClasses(packages = "io.github.diegoquirino.veredito", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainIsolationTest {

    @ArchTest
    static final ArchRule domainIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage("..veredito.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "java.sql..",
                    "javax.sql..",
                    "jakarta.servlet..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainIsFreeOfOuterLayers = noClasses()
            .that().resideInAPackage("..veredito.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..veredito.api..",
                    "..veredito.web..",
                    "..veredito.persistence..",
                    "..veredito.config..")
            .allowEmptyShould(true);
}
