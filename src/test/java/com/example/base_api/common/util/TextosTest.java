package com.example.base_api.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextosTest {

    @Test
    void blankToNullApagaVazioEApara() {
        assertThat(Textos.blankToNull(null)).isNull();
        assertThat(Textos.blankToNull("   ")).isNull();
        assertThat(Textos.blankToNull("  Ana  ")).isEqualTo("Ana");
    }
}
