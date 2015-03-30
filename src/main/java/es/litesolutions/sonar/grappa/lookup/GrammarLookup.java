package es.litesolutions.sonar.grappa.lookup;

import org.sonar.sslr.grammar.GrammarRuleKey;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.util.Objects;

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
