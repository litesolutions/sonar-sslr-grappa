/*
 * Copyright (c) 2016 Lite Solutions
 *
 *  This code is licensed under the Apache Software License version 2.
 *  For more information, see the LICENSE file at the root of this package.
 *
 *  Should you not have the source code available, and the file above is
 *  unavailable, you can obtain a copy of the license here:
 *
 *  https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 */

package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.exceptions.GrappaException;
import com.github.fge.grappa.rules.Rule;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public final class GrappaSslrFactory
{
    private final Rule rule;
    private final MethodHandle grammarInjector;
    private final GrammarRuleKey entryPoint;

    private final Collection<ListenerSupplier> suppliers;

    public static <P extends SonarParserBase> Builder<P> withParserClass(
        final Class<P> parserClass)
    {
        return new Builder<>(parserClass);
    }

    private <P extends SonarParserBase> GrappaSslrFactory(
        final Builder<P> builder)
    {
        final P parser = Grappa.createParser(builder.parserClass);
        rule = builder.ruleFunction.apply(parser);
        grammarInjector = builder.grammarInjector;
        entryPoint = builder.entryPoint;
        suppliers = Collections.unmodifiableCollection(builder.suppliers);
    }

    public Parser<Grammar> getParser()
    {
        final GrappaChannel channel = new GrappaChannel(rule);

        suppliers.forEach(channel::addListenerSupplier);

        final LexerfulGrammarBuilder builder = getGrammarBuilder();
        builder.setRootRule(entryPoint);

        final Lexer lexer = Lexer.builder()
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(channel)
            .build();

        return Parser.builder(builder.build())
            .withLexer(lexer)
            .build();
    }

    private LexerfulGrammarBuilder getGrammarBuilder()
    {
        final LexerfulGrammarBuilder builder = LexerfulGrammarBuilder.create();
        try {
            grammarInjector.invokeExact(builder);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new GrappaException("unable to invoke grammar injector",
                throwable);
        }
        return builder;
    }

    public static final class Builder<P extends SonarParserBase>
    {
        private static final MethodHandles.Lookup LOOKUP
            = MethodHandles.publicLookup();
        private static final String INJECTOR_NAME = "injectInto";
        private static final MethodType INJECTOR_TYPE
            = MethodType.methodType(void.class, LexerfulGrammarBuilder.class);

        private final Class<P> parserClass;
        private Function<P, Rule> ruleFunction;

        private MethodHandle grammarInjector;
        private GrammarRuleKey entryPoint;

        private final Collection<ListenerSupplier> suppliers = new HashSet<>();

        private Builder(final Class<P> parserClass)
        {
            this.parserClass = Objects.requireNonNull(parserClass);
        }

        public Builder<P> withGrammarClass(
            final Class<? extends GrammarRuleKey> grammarClass)
        {
            Objects.requireNonNull(grammarClass);
            grammarInjector = findInjector(grammarClass);
            return this;
        }

        public Builder<P> withEntryPoint(final GrammarRuleKey entryPoint)
        {
            this.entryPoint = Objects.requireNonNull(entryPoint);
            return this;
        }

        public Builder<P> withMainRule(final Function<P, Rule> ruleFunction)
        {
            this.ruleFunction = Objects.requireNonNull(ruleFunction);
            return this;
        }

        public Builder<P> addListenerSupplier(final ListenerSupplier supplier)
        {
            suppliers.add(Objects.requireNonNull(supplier));
            return this;
        }

        public GrappaSslrFactory build()
        {
            Objects.requireNonNull(parserClass, "no parser class has been "
                + "defined");
            Objects.requireNonNull(ruleFunction, "no rule has been defined");
            Objects.requireNonNull(grammarInjector, "no grammar class has "
                + "been defined");
            Objects.requireNonNull(entryPoint, "no grammar rule has been "
                + "defined");
            return new GrappaSslrFactory(this);
        }

        private MethodHandle findInjector(final Class<?> grammarClass)
        {
            try {
                return LOOKUP.findStatic(grammarClass, INJECTOR_NAME,
                    INJECTOR_TYPE);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new GrappaException("unable to find injection method "
                    + "for class " + grammarClass.getName(), e);
            }
        }
    }
}
