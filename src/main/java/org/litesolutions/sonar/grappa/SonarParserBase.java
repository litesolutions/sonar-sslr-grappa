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

package org.litesolutions.sonar.grappa;

import com.github.fge.grappa.parsers.EventBusParser;
import com.github.fge.grappa.run.context.Context;
import com.github.fge.grappa.support.Position;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Token.Builder;
import com.sonar.sslr.api.TokenType;

/**
 * The base (grappa) parser class to extend
 *
 * <p>This base parser class only defines a single method whose argument is
 * a {@link TokenType} and always returns true.</p>
 *
 * <p>Typically, the usage will be:</p>
 *
 * <pre>
 *     public Rule myRule()
 *     {
 *         return sequence(otherRule(), pushToken(myToken));
 *     }
 * </pre>
 *
 * <p>This method then builds a {@link Builder} using information from the
 * parsing context to obtain the start and end of the match, and associates this
 * match with the token type.</p>
 */
@SuppressWarnings({ "AutoBoxing", "AbstractClassNeverImplemented" })
// @formatter:off
public abstract class SonarParserBase
    extends EventBusParser<Token.Builder>
{
    public boolean pushToken(final TokenType tokenType)
    {
        final Context<Token.Builder> context = getContext();
        final int startIndex = context.getMatchStartIndex();
        final Position position
            = context.getInputBuffer().getPosition(startIndex);

        final Token.Builder token = Token.builder()
            .setValueAndOriginalValue(match())
            .setLine(position.getLine())
            .setColumn(position.getColumn())
            .setType(tokenType);

        return push(token);
    }
}
