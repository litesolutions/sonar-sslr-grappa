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

import com.github.fge.grappa.parsers.ListeningParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.context.Context;
import com.github.fge.grappa.support.Position;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Lexer;
import es.litesolutions.sonar.grappa.tokentypes.CaseInsensitive;
import es.litesolutions.sonar.grappa.tokentypes.WithValue;

/**
 * Basic parser definition for a {@link GrappaChannel}
 *
 * <p>This parser defines rules allowing you to pubish the results of a rule as
 * a Sonar {@link Token}. The generic method is {@link #pushToken(TokenType)}.
 * </p>
 *
 * <p>You also have convenience methods to allow you to both match input and
 * push the matching token; which are recognized by the parser and acted upon
 * See the javadoc of the defined methods for more details.</p>
 *
 * @see CodeReaderListener
 */
@SuppressWarnings("AutoBoxing")
public abstract class SonarParserBase
    extends ListeningParser<Token.Builder>
{
    /**
     * Generic method to publish a token from a {@link TokenType}
     *
     * <p>This method will push a token <em>builder</em> on the stack, filling it
     * with all the necessary information to produce the token, except for the URI
     * of the lexer (this is done by the {@link CodeReaderListener}.</p>
     *
     * <p>Note that the input text associated with the published token will be
     * that of the <em>immediately preceding rule</em>. This, for instance:</p>
     *
     * <pre>
     *     Rule foobar()
     *     {
     *         return sequence("foo", "bar", pushToken(FOOBAR);
     *     }
     * </pre>
     *
     * <p>will associated the token with input {@code bar}, not {@code foobar}!
     * </p>
     *
     * @param tokenType the token type
     * @return always true
     */
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

    /**
     * Associate the immediately preceding matched text with a comment
     *
     * <p>In fact, this calls {@link #pushToken(TokenType)} with a token type
     * of {@link GenericTokenType#COMMENT}; the listener will recognize this
     * when the parsing is done and also associate a {@link Trivia} with the
     * matched text.</p>
     *
     * @return always true
     *
     * @see Lexer#addTrivia(Trivia...)
     * @see Trivia.TriviaKind#COMMENT
     */
    public boolean setAsComment()
    {
        return pushToken(GenericTokenType.COMMENT);
    }

    /**
     * Try and match a TokenType's {@link TokenType#getValue()} and publish the
     * token if matched (optionally case insensitive)
     *
     * @param tokenType the token type
     * @param <T> the type parameter for the token
     * @return always true
     *
     * @see WithValue
     * @see CaseInsensitive
     */
    protected <T extends TokenType & WithValue> Rule token(final T tokenType)
    {
        return tokenType instanceof CaseInsensitive
            ? sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType))
                .label(tokenType.getValue())
            : sequence(tokenType.getValue(), pushToken(tokenType))
                .label(tokenType.getValue());
    }
}
