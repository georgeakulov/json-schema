package org.gasoft.json_schema.compilers;

import org.gasoft.json_schema.compilers.v2019.AdditionalItemsCompiler;
import org.gasoft.json_schema.compilers.v2019.Items2019Compiler;
import org.gasoft.json_schema.compilers.v2019.RecursiveRefCompiler;
import org.gasoft.json_schema.compilers.v2020.Items2020CompilerFactory;
import org.gasoft.json_schema.compilers.v2020.PrefixItemsFactory;

public class CommonCompilersFactory {

    private static final CompilerRegistry COMPILER_REGISTRY = new CompilerRegistry();

    static {

        COMPILER_REGISTRY
                .addCompiler(new PropertiesCompiler())
                .addCompiler(new PatternPropertiesCompiler())
                .addCompiler(new AdditionalPropertiesCompiler())
                .addCompiler(new PropertyNamesCompiler())

                .addCompiler(new MinLengthCompiler())
                .addCompiler(new MaxLengthCompiler())
                .addCompiler(new PatternCompiler())

                .addCompiler(new TypeCompiler())
                .addCompiler(new EnumCompiler())
                .addCompiler(new ConstCompiler())
                .addCompiler(new MultipleOfCompiler())

                .addCompiler(new MaxItemsCompiler())
                .addCompiler(new MinItemsCompiler())

                .addCompiler(new MaximumCompiler())
                .addCompiler(new ExclusiveMaximumCompiler())
                .addCompiler(new MinimumCompiler())
                .addCompiler(new ExclusiveMinimumCompiler())
                .addCompiler(new MinPropertiesCompiler())
                .addCompiler(new MaxPropertiesCompiler())

                .addCompiler(new PrefixItemsFactory())
                .addCompiler(new Items2020CompilerFactory())
                .addCompiler(new Items2019Compiler())

                .addCompiler(new ContainsCompilerFactory())
                .addCompiler(new RequiredCompiler())
                .addCompiler(new NotCompiler())
                .addCompiler(new DependentRequiredCompiler())
                .addCompiler(new IfThenElseCompilerFactory())
                .addCompiler(new RefCompiler())

                .addCompiler(new AllOfCompiler())
                .addCompiler(new AnyOfCompiler())
                .addCompiler(new OneOfCompiler())
                .addCompiler(new UniqueItemsCompiler())
                .addCompiler(new DependentSchemasCompiler())
                .addCompiler(new IdCompiler())
                .addCompiler(new UnevaluatedPropertiesCompiler())
                .addCompiler(new DynamicRefCompiler())
                .addCompiler(new UnevaluatedItemsCompiler())
                .addCompiler(new DependenciesCompiler())
                .addCompiler(new EmptyCompilerFactory())
                .addCompiler(new DefsCompiler())
                .addCompiler(new FormatCompiler())
                .addCompiler(new AdditionalItemsCompiler())
                .addCompiler(new RecursiveRefCompiler())
        ;
    }

    public static CompilerRegistry getCompilerRegistry() {
        return COMPILER_REGISTRY;
    }
}
