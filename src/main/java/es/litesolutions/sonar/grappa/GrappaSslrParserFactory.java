package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.exceptions.GrappaException;
import com.github.fge.grappa.rules.Rule;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.Parser;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import es.litesolutions.sonar.grappa.lookup.GrammarLookup;
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

@ParametersAreNonnullByDefault
public final class GrappaSslrParserFactory
{
    private final RuleLookup<?> ruleLookup;
    private final MethodHandle grammarInjector;

    private final Rule mainRule;
    private final GrammarRuleKey entryPoint;

    private final Collection<ListenerSupplier> suppliers;

    public static <P extends SonarParserBase, G extends Enum<G> & GrammarRuleKey>
        Builder<P, G> builder(final Class<P> parserClass,
        final Class<G> grammarClass)
    {
        return new Builder<>(parserClass, grammarClass);
    }

    private GrappaSslrParserFactory(final Builder<?, ?> builder)
    {
        ruleLookup = builder.ruleLookup;
        grammarInjector = builder.grammarInjector;
        mainRule = builder.mainRule;
        entryPoint = builder.entryPoint;
        suppliers = Collections.unmodifiableCollection(builder.suppliers);
    }

    public Parser<Grammar> getFullParser()
    {
        return doGetParser(mainRule, entryPoint);
    }

    public Parser<Grammar> getParser(final GrammarRuleKey key)
    {
        final Rule rule = ruleLookup.getRuleByKey(key);
        return doGetParser(rule, key);
    }


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

        public Builder<P, G> withEntryPoint(final GrammarRuleKey entryPoint)
        {
            this.entryPoint = Objects.requireNonNull(entryPoint);
            return this;
        }

        public Builder<P, G> withMainRule(final String ruleName)
        {
            Objects.requireNonNull(ruleName);
            mainRule = ruleLookup.getRuleByName(ruleName);
            return this;
        }

        public Builder<P, G> addListenerSupplier(
            final ListenerSupplier supplier)
        {
            suppliers.add(Objects.requireNonNull(supplier));
            return this;
        }

        public GrappaSslrParserFactory build()
        {
            if (mainRule == null)
                mainRule = ruleLookup.getMainRule();
            Objects.requireNonNull(entryPoint, "grammar entry point has not "
                + "been set");
            return new GrappaSslrParserFactory(this);
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
