/*
 * Copyright 2015 Francis Galiegue, fgaliegue@gmail.com
 *
 * This code is licensed under the Apache Software License version 2. For more information, see the LICENSE file at the root of this package.
 *
 * Should you not have the source code available, and the file above is unavailable, you can obtain a copy of the license here:
 *
 * https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 */

package es.litesolutions.sonar.grappa.lookup;

import com.github.fge.grappa.rules.Rule;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on a (Grappa) {@link Rule} to associate it with one, or more,
 * {@link GrammarRuleKey}(s)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GrammarRule
{
    Key[] value();

    @interface Key
    {
        // TODO: restrict to enums
        Class<? extends GrammarRuleKey> grammar();

        String key();
    }
}
