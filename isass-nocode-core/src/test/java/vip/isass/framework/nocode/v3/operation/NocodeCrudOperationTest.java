package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCrudOperationTest {

    @Test
    void resolvesStandardOperationName() {
        assertThat(NocodeCrudOperation.fromOperationName("findById"))
                .contains(NocodeCrudOperation.FIND_BY_ID);
        assertThat(NocodeCrudOperation.fromOperationName("missing"))
                .isEmpty();
    }
}
