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

package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.parsers.ListeningParser;
import com.github.fge.grappa.run.context.Context;
import com.github.fge.grappa.support.Position;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;

@SuppressWarnings("AutoBoxing")
// @formatter:off
public abstract class SonarParserBase
    extends ListeningParser<Token.Builder>
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
