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

package org.litesolutions.sonar.grappa;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.trace.TracingListener;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import org.litesolutions.sonar.grappa.injector.GrammarInjector;
import org.litesolutions.sonar.grappa.injector.LegacyGrammarInjector;
import org.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * A factory to create a Sonar parser based on a lexerful {@link Grammar} and
 * a Grappa parser
 *
 * <p>The syntax to create a factory is:</p>
 *
 * <pre>
 *     final GrappaSslrFactory factory = GrappaSslrFactory
 *         .withParserClass(MyParser.class)
 *         .withInjector(MyGrammar::myMethod)
 *         .withEntryPoint(someGrammarRuleKey)
 *         .build();
 * </pre>
 *
 * <p>You then use the {@link #getParser()} method to build a parser, which you
 * will then use in an .</p>
 *
 * <p>Unless otherwise noted, all methods of this class do not accept null
 * arguments; if a null argument is passed, a {@link NullPointerException} will
 * be thrown.</p>
 */
@ParametersAreNonnullByDefault
public final class GrappaSslrFactory
{
    private final Rule rule;
    private final GrammarInjector injector;
    private final GrammarRuleKey entryPoint;

    private final Collection<ListenerSupplier> suppliers;

    /**
     * Initialize a builder for a new factory
     *
     * @param parserClass the parser class
     * @param <P> type of the parser
     * @return a new builder
     */
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

    /**
     * Get a Sonar {@link Parser} from this factory
     *
     * @return a new parser instance
     */
    public GrappaSslrParser<Grammar> getParser()
    {
        return getParserWithCharset(null);
    }

    /**
     * Get a Sonar {@link Parser} from this factory
     *
     * @return a new parser instance
     */
    public GrappaSslrParser<Grammar> getParserWithCharset(@Nullable String charsetName)
    {
        final GrappaChannel channel = new GrappaChannel(rule);

        suppliers.forEach(channel::addListenerSupplier);

        final LexerfulGrammarBuilder builder = getGrammarBuilder();
        builder.setRootRule(entryPoint);

        GrappaSslrLexer lexer = getLexer(channel, charsetName);

        return GrappaSslrParser.grappaBuilder(builder.build())
            .withLexer(lexer)
            .build();
    }

    private GrappaSslrLexer getLexer(GrappaChannel channel,@Nullable String charsetName) {
    	if(charsetName!=null) {
    		Charset charset = getCharset(charsetName);
    		return GrappaSslrLexer.builder().withCharset(charset)
    	            .withFailIfNoChannelToConsumeOneCharacter(true)
    	            .withChannel(channel)
    	            .build();
    	}else return GrappaSslrLexer.builder()
                .withFailIfNoChannelToConsumeOneCharacter(true)
                .withChannel(channel)
                .build();
    }

    private Charset getCharset(String charset) {

    	try {
			return Charset.forName(charset);
		} catch (UnsupportedCharsetException e) {
			return Charset.defaultCharset();
		}


    }

    private LexerfulGrammarBuilder getGrammarBuilder()
    {
        final LexerfulGrammarBuilder builder = LexerfulGrammarBuilder.create();
        injector.injectInto(builder);
        return builder;
    }

    /**
     * A builder for a {@link GrappaSslrFactory}
     *
     * <p>This class is not directly instantiable; use {@link
     * GrappaSslrFactory#withParserClass(Class)} to create a new instance.</p>
     *
     * @param <P> the type of the parser used
     */
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

        /**
         * Set the grammar for the factory
         *
         * <p>The grammar class, in this case, is expected to have a static
         * method named {@code injectInto} which takes a {@link
         * LexerfulGrammarBuilder} as its sole argument.</p>
         *
         * @param grammarClass the class
         * @return this
         *
         * @deprecated use {@link #withGrammarInjector(GrammarInjector)} instead
         */
        @Deprecated
        public Builder<P> withGrammarClass(
            final Class<? extends GrammarRuleKey> grammarClass)
        {
            Objects.requireNonNull(grammarClass);
            injector = new LegacyGrammarInjector(grammarClass);
            return this;
        }

        /**
         * Sets the grammar injector for this factory
         *
         * <p>Since Java 8 is used, and a {@link GrammarInjector} is a
         * functional interface, you can use method references here.</p>
         *
         * @param injector the injector
         * @return this
         */
        public Builder<P> withGrammarInjector(final GrammarInjector injector)
        {
            this.injector = Objects.requireNonNull(injector);
            return this;
        }

        /**
         * Define the grammar entry point for the grammar
         *
         * @param entryPoint the entry point, as a {@link GrammarRuleKey}
         * @return this
         *
         * @see LexerfulGrammarBuilder#setRootRule(GrammarRuleKey)
         */
        public Builder<P> withEntryPoint(final GrammarRuleKey entryPoint)
        {
            this.entryPoint = Objects.requireNonNull(entryPoint);
            return this;
        }

        /**
         * Define the main rule for the parser as a {@link Function}
         *
         * <p>Typically, if your parser class is {@code MyParser} and the rule
         * you want as a main rule is called {@code myRule}, the argument to
         * this method will be {@code MyParser::myRule}.</p>
         *
         * @param ruleFunction the function providing the rule
         * @return this
         */
        public Builder<P> withMainRule(final Function<P, Rule> ruleFunction)
        {
            this.ruleFunction = Objects.requireNonNull(ruleFunction);
            return this;
        }

        /**
         * Add a {@link ListenerSupplier} to the factory
         *
         * <p>Since a {@link ParseRunner} is used, it means you can add
         * further parsing listeners when the file is parsed; for instance, you
         * may want to add a {@link TracingListener} to debug the parsing
         * process.</p>
         *
         * @param supplier the supplier
         * @return this
         */
        public Builder<P> addListenerSupplier(final ListenerSupplier supplier)
        {
            suppliers.add(Objects.requireNonNull(supplier));
            return this;
        }

        /**
         * Build the factory
         *
         * @return the factory
         */
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
