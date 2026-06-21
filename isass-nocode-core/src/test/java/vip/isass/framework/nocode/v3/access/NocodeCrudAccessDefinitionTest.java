package vip.isass.framework.nocode.v3.access;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCrudAccessDefinitionTest {

    @Test
    void exposesDefinitionForEveryCrudOperation() {
        assertThat(NocodeCrudAccessDefinition.all())
                .extracting(NocodeCrudAccessDefinition::operation)
                .containsExactly(NocodeCrudOperation.values());
    }

    @Test
    void describesFindByIdArguments() {
        NocodeCrudAccessDefinition definition = NocodeCrudAccessDefinition.find("findById").orElseThrow();

        assertThat(definition.requiredArguments())
                .containsExactly(NocodeCrudAccessRequests.ARG_ID);
        assertThat(definition.optionalArguments())
                .containsExactly(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS);
        assertThat(definition.requiresArgument(NocodeCrudAccessRequests.ARG_ID)).isTrue();
        assertThat(definition.supportsArgument(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS)).isTrue();
    }

    @Test
    void describesWriteAndDeleteArguments() {
        assertThat(NocodeCrudAccessDefinition.find(NocodeCrudOperation.SAVE).orElseThrow().requiredArguments())
                .containsExactly(NocodeCrudAccessRequests.ARG_BODY);
        assertThat(NocodeCrudAccessDefinition.find(NocodeCrudOperation.UPDATE_BY_ID).orElseThrow().requiredArguments())
                .containsExactly(NocodeCrudAccessRequests.ARG_ID, NocodeCrudAccessRequests.ARG_BODY);
        assertThat(NocodeCrudAccessDefinition.find(NocodeCrudOperation.DELETE_BY_ID).orElseThrow().optionalArguments())
                .containsExactly(NocodeCrudAccessRequests.ARG_DELETE_OPTIONS);
    }
}
