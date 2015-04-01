package es.litesolutions.sonar.grappa.lookup;

import org.sonar.sslr.grammar.GrammarRuleKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used in a {@link GrammarRuleKey} enumeration to specify this
 * grammar's entry point
 *
 * <p>Note that only one such key may exist per grammar.</p>
 *
 * @see GrammarLookup
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EntryPoint
{
}
