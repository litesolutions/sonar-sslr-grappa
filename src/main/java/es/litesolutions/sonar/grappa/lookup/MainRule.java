package es.litesolutions.sonar.grappa.lookup;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on a parser's entry point
 *
 * <p>Note that only one rule in the parser can use this annotation.</p>
 *
 * @see RuleLookup#getMainRule()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MainRule
{
}
