package org.kkobi.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServletConfigComponentScanTest {

    @Test
    void componentScanIncludesProductControllerPackage() {
        ComponentScan scan = ServletConfig.class.getAnnotation(ComponentScan.class);
        List<String> basePackages = Arrays.asList(scan.basePackages());

        assertTrue(
                basePackages.contains("org.kkobi.product.controller"),
                "ServletConfig의 @ComponentScan basePackages에 org.kkobi.product.controller가 없습니다."
        );
    }
}
