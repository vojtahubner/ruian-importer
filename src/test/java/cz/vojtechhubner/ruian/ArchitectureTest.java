package cz.vojtechhubner.ruian;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enforces the hexagonal layout of the production code: a framework-free domain, an application layer that knows no
 * adapters, adapters that know nothing of each other, and JPA and transactions confined to the persistence adapter.
 */
@AnalyzeClasses(packages = "cz.vojtechhubner.ruian", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnJavaAndJSpecify = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..", "org.jspecify..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdaptersOrConfig = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..");

    @ArchTest
    static final ArchRule adaptersDoNotDependOnEachOther = slices()
            .matching("..adapter.(*).(*)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule configDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule jpaStaysInThePersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule noTransactionsInApplicationLayer = CompositeArchRule
            .of(noClasses()
                    .that().resideInAPackage("..application..")
                    .should().beAnnotatedWith(Transactional.class))
            .and(noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..application..")
                    .should().beAnnotatedWith(Transactional.class));

    @ArchTest
    static final ArchRule persistenceAdaptersAreTransactional = methods()
            .that().arePublic()
            .and().areDeclaredInClassesThat().resideInAPackage("..adapter.out.persistence..")
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("PersistenceAdapter")
            .and().areNotStatic()
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule noCyclesBetweenTopLevelPackages = slices()
            .matching("cz.vojtechhubner.ruian.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllersLiveInTheWebAdapter = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..adapter.in.web..");
}
