package com.han.commerce;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.han.commerce");

    @Test
    @DisplayName("도메인 계층은 다른 계층을 참조하지 않는다")
    void domainDoesNotDependOnOtherLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..presentation..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("응용 계층은 인프라 계층과 표현 계층을 참조하지 않는다")
    void applicationDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..presentation..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("주문의 응용 계층은 다른 도메인을 그 도메인의 응용 계층으로만 다룬다")
    void orderUsesOtherDomainsOnlyThroughTheirServices() {
        noClasses().that().resideInAPackage("..order.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..stock.domain..", "..stock.infrastructure..",
                        "..coupon.domain..", "..coupon.infrastructure..",
                        "..point.domain..", "..point.infrastructure..",
                        "..product.domain..", "..product.infrastructure..",
                        "..payment.domain..", "..payment.infrastructure..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("dto는 자바 표준 클래스, 도메인, 다른 dto, 포트의 요청·결과 객체만 참조한다")
    void dtoDependsOnlyOnDataClasses() {
        classes().that().resideInAPackage("..dto..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "..domain..", "..dto..", "..port.request..", "..port.result..")
                .check(CLASSES);
    }

}