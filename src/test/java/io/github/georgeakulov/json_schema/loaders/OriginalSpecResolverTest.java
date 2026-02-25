package io.github.georgeakulov.json_schema.loaders;

import io.github.georgeakulov.json_schema.dialects.Defaults;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class OriginalSpecResolverTest {
    @Test
    void testPreload() {
        var spec = new OriginalSpecResolver();
        var resolved = spec.resolve(Defaults.DIALECT_2020_12.toString(), null);
        Assertions.assertNotNull(resolved);
    }
}