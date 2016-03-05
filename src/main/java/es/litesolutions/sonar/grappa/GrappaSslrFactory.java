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
import com.github.fge.grappa.rules.Rule;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import es.litesolutions.sonar.grappa.injector.GrammarInjector;
import es.litesolutions.sonar.grappa.injector.LegacyGrammarInjector;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public final class GrappaSslrFactory
{
    private final Rule rule;
    private final GrammarInjector injector;
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
        injector = builder.injector;
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
        injector.injectInto(builder);
        return builder;
    }

    public static final class Builder<P extends SonarParserBase>
    {
        private final Class<P> parserClass;
        @SuppressWarnings("InstanceVariableMayNotBeInitialized")
        private Function<P, Rule> ruleFunction;

        @SuppressWarnings("InstanceVariableMayNotBeInitialized")
        private GrammarRuleKey entryPoint;

        @SuppressWarnings("InstanceVariableMayNotBeInitialized")
        private GrammarInjector injector;

        private final Collection<ListenerSupplier> suppliers = new HashSet<>();

        private Builder(final Class<P> parserClass)
        {
            this.parserClass = Objects.requireNonNull(parserClass);
        }

        public Builder<P> withGrammarClass(
            final Class<? extends GrammarRuleKey> grammarClass)
        {
            Objects.requireNonNull(grammarClass);
            injector = new LegacyGrammarInjector(grammarClass);
            return this;
        }

        public Builder<P> withGrammarInjector(final GrammarInjector injector)
        {
            this.injector = Objects.requireNonNull(injector);
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
            Objects.requireNonNull(ruleFunction, "no rule has been defined");
            Objects.requireNonNull(injector, "no grammar injector has "
                + "been defined");
            Objects.requireNonNull(entryPoint, "no grammar entry point has been"
                + " defined");
            return new GrappaSslrFactory(this);
        }
    }
}
