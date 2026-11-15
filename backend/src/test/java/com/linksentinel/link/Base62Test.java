package com.linksentinel.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Base62Test {

    @Test
    void encodesZeroAndAlphabetBoundaries() {
        assertThat(Base62.encode(0)).isEqualTo("0");
        assertThat(Base62.encode(10)).isEqualTo("A");
        assertThat(Base62.encode(35)).isEqualTo("Z");
        assertThat(Base62.encode(36)).isEqualTo("a");
        assertThat(Base62.encode(61)).isEqualTo("z");
        assertThat(Base62.encode(62)).isEqualTo("10");
        assertThat(Base62.encode(3844)).isEqualTo("100");
    }

    @Test
    void rejectsNegativeValues() {
        assertThatThrownBy(() -> Base62.encode(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
