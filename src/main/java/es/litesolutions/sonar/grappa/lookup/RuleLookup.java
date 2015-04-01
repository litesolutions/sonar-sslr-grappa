package es.litesolutions.sonar.grappa.lookup;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.exceptions.GrappaException;
import com.github.fge.grappa.rules.Rule;
import es.litesolutions.sonar.grappa.SonarParserBase;
import org.sonar.sslr.grammar.GrammarRuleKey;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to lookup rules in a parser class
 *
 * <p>There are three ways to lookup rules:</p>
 *
 * <ul>
 *     <li>by name,</li>
 *     <li>by {@link GrammarRuleKey},</li>
 *     <li>annotated with {@link MainRule}.</li>
 * </ul>
 *
 * <p>The second lookup mechanism requires that the rule be annotated with
 * {@link GrammarRule}.</p>
 *
 * <p>Note that the only methods looked up need to obey the following
 * conditions:</p>
 *
 * <ul>
 *     <li>they must return a {@link Rule};</li>
 *     <li>they must have no arguments;</li>
 *     <li>they must be {@code public}.</li>
 * </ul>
 *
 * <p>Also, the parser constructor must not take any arguments.</p>
 *
 * @param <P> type parameter of the parser (which must extend {@link
 * SonarParserBase})
 */
@ParametersAreNonnullByDefault
public final class RuleLookup<P extends SonarParserBase>
{
    private static final MethodHandles.Lookup LOOKUP
        = MethodHandles.publicLookup();
    private static final MethodType RULE_METHOD
        = MethodType.methodType(Rule.class);
    private static final MethodType METHOD_TYPE
        = MethodType.methodType(Rule.class, SonarParserBase.class);

    private final Class<P> parserClass;
    private final P parser;

    private final List<Method> ruleMethods;

    private final Rule mainRule;

    private final Map<String, Rule> rules = new HashMap<>();

    /**
     * Constructor
     *
     * @param parserClass the parser class
     */
    public RuleLookup(final Class<P> parserClass)
    {
        this.parserClass = Objects.requireNonNull(parserClass);
        parser = Grappa.createParser(parserClass);

        ruleMethods = getRuleMethods(parserClass);

        final MethodHandle handle = getMainRuleHandle();
        mainRule = handle != null ? getRule(handle) : null;
    }

    /**
     * Return the rule annotated with {@link MainRule}
     *
     * @return the rule
     * @throws IllegalStateException no rule in the parser is thus annotated
     */
    public Rule getMainRule()
    {
        if (mainRule == null)
            throw new IllegalStateException("parser does not define a "
                + "@MainRule");
        return mainRule;
    }

    /**
     * Return a rule by name
     *
     * @param name the name of the rule (the method name)
     * @return the rule
     */
    public Rule getRuleByName(final String name)
    {
        Objects.requireNonNull(name);

        Rule rule;

        synchronized (rules) {
            rule = rules.get(name);
            if (rule == null) {
                rule = findRuleByName(name);
                rules.put(name, rule);
            }
        }

        return rule;
    }

    /**
     * Return a rule associated with a given {@link GrammarRuleKey}
     *
     * @param key the key
     * @return the rule
     *
     * @see GrammarRule
     */
    public Rule getRuleByKey(final GrammarRuleKey key)
    {
        Objects.requireNonNull(key);

        final Class<?> grammarClass = key.getClass();
        final String name = ((Enum<?>) key).name();

        Method candidate = null;
        GrammarRule grammarRule;

        for (final Method method: ruleMethods) {
            grammarRule = method.getAnnotation(GrammarRule.class);
            if (grammarRule == null)
                continue;
            if (!matchesKey(grammarRule.value(), grammarClass, name))
                continue;
            if (candidate != null)
                throw new IllegalStateException("only one parser rule can be "
                    + "declared to match a grammar rule");
            candidate = method;
        }

        if (candidate == null)
            throw new GrappaException("no rule was found matching grammar rule "
                + key);

        return getRuleByName(candidate.getName());
    }

    private MethodHandle getHandleByName(final String ruleName)
    {
        try {
            final MethodHandle handle
                = LOOKUP.findVirtual(parserClass, ruleName, RULE_METHOD);
            return handle.asType(METHOD_TYPE);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new GrappaException("unable to grab a MethodHandle for rule "
                + ruleName + ", parser class " + parserClass.getName(), e);
        }
    }

    private Rule findRuleByName(final String ruleName)
    {
        final MethodHandle handle = getHandleByName(ruleName);
        return getRule(handle);
    }

    @Nullable
    private MethodHandle getMainRuleHandle()
    {
        Method candidate = null;

        for (final Method method: ruleMethods) {
            if (method.getAnnotation(MainRule.class) == null)
                continue;
            if (candidate != null)
                throw new IllegalStateException("only one rule may be annotated"
                    + " with @MainRule");
            candidate = method;
        }

        if (candidate == null)
            return null;

        return unreflect(candidate);
    }

    private Rule getRule(final MethodHandle handle)
    {
        try {
            return (Rule) handle.invokeExact(parser);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new GrappaException("method handle invocation failed",
                throwable);
        }
    }

    private static List<Method> getRuleMethods(final Class<?> parserClass)
    {
        final List<Method> list = new ArrayList<>();

        for (final Method method: parserClass.getMethods())
            if (isRuleMethod(method))
                list.add(method);

        return Collections.unmodifiableList(list);
    }

    private static boolean isRuleMethod(final Method method)
    {
        return (method.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
            && method.getParameterTypes().length == 0
            && method.getReturnType() == Rule.class;
    }

    private static boolean matchesKey(final GrammarRule.Key[] keys,
        final Class<?> grammarClass, final String name)
    {
        for (final GrammarRule.Key key: keys)
            if (key.grammar().equals(grammarClass) && key.key().equals(name))
                return true;

        return false;
    }

    private static MethodHandle unreflect(final Method method)
    {
        try {
            return LOOKUP.unreflect(method).asType(METHOD_TYPE);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new GrappaException("unable to obtain a method handle from"
                + " main rule " + method.getName(), throwable);
        }
    }
}
