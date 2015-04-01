package es.litesolutions.sonar.grappa.lookup;

import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Convenience utility class to look up a grammar's "entry point"
 *
 * <p>This grammar rule key will be annotated with {@link EntryPoint}; this is
 * the rule you would normally declare as the root rule of a {@link
 * LexerfulGrammarBuilder} using {@link
 * LexerfulGrammarBuilder#setRootRule(GrammarRuleKey)}.</p>
 *
 * @param <G> type parameter of the grammar
 */
@ParametersAreNonnullByDefault
public final class GrammarLookup<G extends Enum<G> & GrammarRuleKey>
{
    private final Class<G> grammarClass;

    @Nullable
    public static <E extends Enum<E> & GrammarRuleKey> GrammarRuleKey
        entryPoint(final Class<E> grammarClass)
    {
        return new GrammarLookup<>(grammarClass).getEntryPoint();
    }


    private GrammarLookup(final Class<G> grammarClass)
    {
        this.grammarClass = Objects.requireNonNull(grammarClass);
    }

    @Nullable
    public GrammarRuleKey getEntryPoint()
    {
        final Field[] fields = grammarClass.getDeclaredFields();

        Field candidate = null;

        for (final Field field: fields) {
            if (field.getAnnotation(EntryPoint.class) == null)
                continue;
            if (candidate != null)
                throw new IllegalStateException("only one rule key can be "
                    + "annotated with @EntryPoint");
            candidate = field;
        }

        if (candidate == null)
            return null;

        return Enum.valueOf(grammarClass, candidate.getName());
    }
}
