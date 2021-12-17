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

package org.litesolutions.sonar.grappa.injector;

import com.github.fge.grappa.exceptions.GrappaException;
import org.litesolutions.sonar.grappa.GrappaSslrFactory;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Legacy grammar injector
 *
 * <p>This injector requires that a static method named {@code injectInto}
 * exists in the class you pass as an argument to the constructor, and this
 * method must takes {@link LexerfulGrammarBuilder} as an argument.</p>
 *
 * <p>Typically, your grammar will look like:</p>
 *
 * <pre>
 *     public enum MyGrammar
 *         implements GrammarRuleKey
 *     {
 *         KEY1,
 *         KEY2,
 *         ;
 *
 *         public static void injectInto(final LexerfulGrammarBuilder builder)
 *         {
 *             builder.rule(KEY1).is(...)
 *             // etc
 *         }
 *     }
 * </pre>
 *
 * @see GrappaSslrFactory.Builder#withParserClass(Class)
 */
public final class LegacyGrammarInjector
    implements GrammarInjector
{
    private static final MethodHandles.Lookup LOOKUP
        = MethodHandles.publicLookup();
    private static final String INJECTOR_NAME = "injectInto";
    private static final MethodType INJECTOR_TYPE
        = MethodType.methodType(void.class, LexerfulGrammarBuilder.class);
    private final MethodHandle injector;

    /**
     * Constructor
     *
     * @param c the grammar class
     */
    public LegacyGrammarInjector(final Class<? extends GrammarRuleKey> c)
    {
        injector = findInjector(c);
    }

    @Override
    public void injectInto(final LexerfulGrammarBuilder builder)
    {
        try {
            injector.invokeExact(builder);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable throwable) {
            throw new GrappaException("unable to invoke grammar injector",
                throwable);
        }
    }

    private static MethodHandle findInjector(final Class<?> grammarClass)
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
