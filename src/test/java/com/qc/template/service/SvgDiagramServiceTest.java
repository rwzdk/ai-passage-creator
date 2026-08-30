package com.qc.template.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgDiagramServiceTest {

    @Test
    void tightensViewBoxWhenDiagramUsesOnlySmallPartOfCanvas() {
        String svg = """
                <svg viewBox="0 0 800 600">
                  <rect width="800" height="600" fill="#fff"/>
                  <circle cx="400" cy="300" r="50"/>
                  <text x="350" y="220" font-size="24">中心节点</text>
                </svg>
                """;

        String normalized = SvgDiagramService.normalizeSvg(svg, 800, 600);

        assertTrue(normalized.contains("preserveAspectRatio=\"xMidYMid meet\""));
        assertTrue(normalized.contains("width=\"800\""));
        assertFalse(normalized.contains("viewBox=\"0 0 800 600\""));
    }

    @Test
    void rejectsUnsafeSvgElements() {
        String svg = "<svg><script>alert(1)</script><circle cx=\"20\" cy=\"20\" r=\"10\"/></svg>";

        assertThrows(IllegalArgumentException.class, () -> SvgDiagramService.normalizeSvg(svg, 800, 600));
    }
}
