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

package es.litesolutions.sonar.grappa.injector;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import es.litesolutions.sonar.grappa.GrappaSslrFactory;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

/**
 * Inject a grammar into a {@link LexerfulGrammarBuilder}
 *
 * <p>This interface is used by {@link GrappaSslrFactory} to prepare a grammar
 * for a Sonar {@link Parser}. The process is as follows:</p>
 *
 * <ul>
 *     <li>a new {@code LexerfulGrammarBuilder} is created;</li>
 *     <li>implementations of this interface are called on this builder;</li>
 *     <li>the grammar is built (see {@link LexerfulGrammarBuilder#build()};
 *     </li>
 *     <li>it is given as an argument to the parser (see {@link
 *     Parser#builder(Grammar)}.</li>
 * </ul>
 */
@FunctionalInterface
public interface GrammarInjector
{
    void injectInto(LexerfulGrammarBuilder builder);
}
