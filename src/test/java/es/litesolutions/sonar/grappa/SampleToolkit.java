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

package es.litesolutions.sonar.grappa;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.toolkit.AbstractConfigurationModel;
import org.sonar.sslr.toolkit.ConfigurationProperty;

import java.util.Collections;
import java.util.List;

public final class SampleToolkit
{
    private SampleToolkit()
    {
        throw new Error("nice try!");
    }

    public static void main(final String... args)
    {
        /*
         * Initialize your language parser and your rule
         */

        /*
        final MyParser parser
            = Parboiled.createParser(MyParser.class);
        final Rule rule = parser.someRule();
        final Channel<Lexer> channel = new GrappaChannel(rule);
        */

        /*
         * Initialize the lexer; only one channel!
         */

        /*
        final Lexer lexer = Lexer.builder()
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(channel)
            .build();
        */

        /*
         * Initialize your rules; here we take the example of a grammar which
         * has no root rule, but which we inject into a builder.
         *
         * Match the GrammarRuleKey with the grappa parser rule.
         */

        /*
        final LexerfulGrammarBuilder builder = LexerfulGrammarBuilder.create();
        MyGrammar.injectInto(builder);
        builder.setRootRule(MyGrammar.MY_RULE_KEY);
        */

        /*
         * Finally, create the Grammar object, the Parser object, initialize
         * the toolkit and run it
         */

        /*
        final Grammar grammar = builder.build();

        final Parser<Grammar> grammarParser = Parser.builder(grammar)
            .withLexer(lexer).build();

        final Toolkit toolkit = new Toolkit("test",
            new DummyConfigurationModel(grammarParser));

        toolkit.run();
        */
    }

    private static final class DummyConfigurationModel
        extends AbstractConfigurationModel
    {
        private final Parser<Grammar> parser;

        private DummyConfigurationModel(final Parser<Grammar> parser)
        {
            this.parser = parser;
        }

        @Override
        public Parser doGetParser()
        {
            return parser;
        }

        @Override
        public List<Tokenizer> doGetTokenizers()
        {
            return Collections.emptyList();
        }

        @Override
        public List<ConfigurationProperty> getProperties()
        {
            return Collections.emptyList();
        }
    }
}
