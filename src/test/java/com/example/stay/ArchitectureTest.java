package com.example.stay;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 패키지 의존 규칙. PR 에서 규칙을 어기면 테스트가 실패한다.
 *   1) common 은 어떤 feature 도 import 하지 않는다.
 *   2) feature 끼리는 서로 import 하지 않는다. 공유할 것은 common 으로 올리거나, id(Long)로만 참조한다.
 * 새 feature 를 추가하면 FEATURES 에 패키지명을 추가하고 아래 규칙 한 쌍을 복사해 두면 된다.
 */
@AnalyzeClasses(packages = "com.example.stay", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule common_does_not_depend_on_features =
            noClasses().that().resideInAPackage("com.example.stay.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.example.stay.auth..",
                            "com.example.stay.accommodation..");

    @ArchTest
    static final ArchRule auth_does_not_depend_on_other_features =
            noClasses().that().resideInAPackage("com.example.stay.auth..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.example.stay.accommodation..");

    @ArchTest
    static final ArchRule accommodation_does_not_depend_on_other_features =
            noClasses().that().resideInAPackage("com.example.stay.accommodation..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.example.stay.auth..");
}
