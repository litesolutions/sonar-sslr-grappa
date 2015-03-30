package es.litesolutions.sonar.grappa.lookup;

import org.sonar.sslr.grammar.GrammarRuleKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GrammarRule
{
    Key[] value();

    @interface Key
    {
        Class<? extends GrammarRuleKey> grammar();

        String key();
    }
}
