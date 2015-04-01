package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.exceptions.GrappaException;
import com.github.fge.grappa.rules.Rule;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import es.litesolutions.sonar.grappa.lookup.EntryPoint;
import es.litesolutions.sonar.grappa.lookup.GrammarLookup;
import es.litesolutions.sonar.grappa.lookup.GrammarRule;
import es.litesolutions.sonar.grappa.lookup.MainRule;
import es.litesolutions.sonar.grappa.lookup.RuleLookup;
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

/**
 * A factory to create Sonar {@link Parser}s using a {@link GrappaChannel}
 *
 * <p>You cannot instantiate this class directly; you have to go through a
 * {@link GrappaSslrFactory.Builder} using the {@link #builder(Class, Class)}
 * method.</p>
 *
 * <p>This factory can create parser instances for either the full grammar or
 * only part of it, provided you supply it with the correct information.</p>
 *
 * <p>Note that it is required that your grammar provide a method with the
 * following signature:</p>
 *
 * <pre>
 *     public static void injectInto(LexerfulGrammarBuilder builder);
 * </pre>
 *
 * <p>The factory will look up this method and automatically build the grammar.
 * </p>
 *
 * <p>Provider your (Grappa) parser and grammar are annotated appropriately,
 * building a factory will be as simple as:</p>
 *
 * <pre>
 *     GrappaSslrFactory.builder(parserClass, grammarClass).build();
 * </pre>
 *
 * @see EntryPoint
 * @see MainRule
 * @see GrammarRule
 * @see SonarParserBase
 */
@ParametersAreNonnullByDefault
public final class GrappaSslrFactory
{
    private final RuleLookup<?> ruleLookup;
    private final MethodHandle grammarInjector;

    private final Rule mainRule;
    private final GrammarRuleKey entryPoint;

    private final Collection<ListenerSupplier> suppliers;

    /**
     * Create a new builder with a given parser class and (lexerful) grammar
     *
     * @param parserClass the parser class
     * @param grammarClass the grammar class
     * @param <P> type parameter of the parser
     * @param <G> type parameter of the grammar
     * @return a new builder
     *
     * @throws GrappaException grammar class has no injection method (see
     * description)
     */
    public static <P extends SonarParserBase, G extends Enum<G> & GrammarRuleKey>
        Builder<P, G> builder(final Class<P> parserClass,
        final Class<G> grammarClass)
    {
        return new Builder<>(parserClass, grammarClass);
    }

    private GrappaSslrFactory(final Builder<?, ?> builder)
    {
        ruleLookup = builder.ruleLookup;
        grammarInjector = builder.grammarInjector;
        mainRule = builder.mainRule;
        entryPoint = builder.entryPoint;
        suppliers = Collections.unmodifiableCollection(builder.suppliers);
    }

    /**
     * Create a full parser
     *
     * @return a parser
     * @see GrappaSslrFactory.Builder#withEntryPoint(GrammarRuleKey)
     * @see GrappaSslrFactory.Builder#withMainRule(String)
     */
    public Parser<Grammar> getFullParser()
    {
        return doGetParser(mainRule, entryPoint);
    }

    /**
     * Create a parser parsing only a given part of the grammar
     *
     * <p>This requires that there is a rule in your parser annotated with a
     * {@link GrammarRule} for this grammar rule key. If this is not the case,
     * use {@link #getParser(String, GrammarRuleKey)}.</p>
     *
     * @param key the key
     * @return a parser
     */
    public Parser<Grammar> getParser(final GrammarRuleKey key)
    {
        final Rule rule = ruleLookup.getRuleByKey(key);
        return doGetParser(rule, key);
    }


    /**
     * Create a parser parsing only a given part of the grammar, specifying the
     * rule
     *
     * <p>Use this if you want to use a parser rule which <em>is not</em>
     * annotated with {@link GrammarRule}.</p>
     *
     * @param ruleName the name of the rule
     * @param key the grammar key
     * @return a parser
     */
    public Parser<Grammar> getParser(final String ruleName,
        final GrammarRuleKey key)
    {
        Objects.requireNonNull(ruleName);
        Objects.requireNonNull(key);

        final Rule rule = ruleLookup.getRuleByName(ruleName);
        return doGetParser(rule, key);
    }

    private Parser<Grammar> doGetParser(final Rule rule,
        final GrammarRuleKey key)
    {
        final GrappaChannel channel = new GrappaChannel(rule);

        for (final ListenerSupplier supplier: suppliers)
            channel.addListenerSupplier(supplier);

        final LexerfulGrammarBuilder builder = getGrammarBuilder();
        builder.setRootRule(key);

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

    /**
     * Builder class for a {@link GrappaSslrFactory}
     *
     * @param <P> type parameter of the (Grappa) parser class
     * @param <G> type parameter of the grammar
     */
    public static final class Builder<P extends SonarParserBase, G extends Enum<G> & GrammarRuleKey>
    {
        private static final MethodHandles.Lookup LOOKUP
            = MethodHandles.publicLookup();
        private static final String INJECTOR_NAME = "injectInto";
        private static final MethodType INJECTOR_TYPE
            = MethodType.methodType(void.class, LexerfulGrammarBuilder.class);

        private final RuleLookup<P> ruleLookup;
        private final MethodHandle grammarInjector;

        private Rule mainRule;
        private GrammarRuleKey entryPoint;

        private final Collection<ListenerSupplier> suppliers = new HashSet<>();

        private Builder(final Class<P> parserClass, final Class<G> grammarClass)
        {
            Objects.requireNonNull(parserClass);
            Objects.requireNonNull(grammarClass);

            ruleLookup = new RuleLookup<>(parserClass);
            grammarInjector = findInjector(grammarClass);
            entryPoint = GrammarLookup.entryPoint(grammarClass);
        }

        /**
         * Declare the entry point of the grammar, if necessary
         *
         * <p>Use this if your grammar has no key annotated with {@link
         * EntryPoint}.</p>
         *
         * @param entryPoint the key to use as an entry point
         * @return this
         */
        public Builder<P, G> withEntryPoint(final GrammarRuleKey entryPoint)
        {
            this.entryPoint = Objects.requireNonNull(entryPoint);
            return this;
        }

        /**
         * Declare the main rule of the Grappa parser, if necessary
         *
         * <p>Use this if your Grappa parser has no rule annotated with {@link
         * MainRule}.</p>
         *
         * @param ruleName the name of the rule
         * @return this
         */
        public Builder<P, G> withMainRule(final String ruleName)
        {
            Objects.requireNonNull(ruleName);
            mainRule = ruleLookup.getRuleByName(ruleName);
            return this;
        }

        /**
         * Add a supplier of parsing run listener
         *
         * @param supplier the supplier
         * @return this
         */
        public Builder<P, G> addListenerSupplier(
            final ListenerSupplier supplier)
        {
            suppliers.add(Objects.requireNonNull(supplier));
            return this;
        }

        /**
         * Build the factory
         *
         * @return the factory
         * @throws NullPointerException either the main rule or the grammar
         * entry point are missing
         */
        public GrappaSslrFactory build()
        {
            if (mainRule == null)
                mainRule = ruleLookup.getMainRule();
            Objects.requireNonNull(entryPoint, "grammar entry point has not "
                + "been set");
            return new GrappaSslrFactory(this);
        }

        private MethodHandle findInjector(final Class<G> grammarClass)
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
