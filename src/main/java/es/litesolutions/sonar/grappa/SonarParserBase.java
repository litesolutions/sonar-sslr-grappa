package es.litesolutions.sonar.grappa;

import com.github.parboiled1.grappa.parsers.EventBusParser;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.Lexer;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.support.Position;

/**
 * Basic parser to use with a {@link GrappaChannel}
 *
 * <p>The basic method is {@link #pushToken(TokenType)}; it will grab the text
 * matched by the previous rule and build a partially constructed {@link
 * Token.Builder} with the following information:</p>
 *
 * <ul>
 *     <li>the matched text (fed into the builder using {@link
 *     Token.Builder#setValueAndOriginalValue(String, String)};</li>
 *     <li>the line and column where the match occurred (using {@link
 *     Token.Builder#setLine(int)} and {@link
 *     Token.Builder#setColumn(int)} respectively);</li>
 *     <li>the type (obviously) using {@link
 *     Token.Builder#setType(TokenType)}.</li>
 * </ul>
 *
 * <p>The URI of the match will be set by the associated {@link
 * CodeReaderListener} and fed into the {@link Lexer}.</p>
 */
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
            .setLine(position.line)
            .setColumn(position.column)
            .setType(tokenType);

        return push(token);
    }

    public Rule valueToken(final ValueTokenType tokenType)
    {
        return sequence(tokenType.getValue(), pushToken(tokenType));
    }

    public Rule shortValueToken(final ShortValueTokenType tokenType)
    {
        return tokenType.getShortValue() == null
            ? sequence(
                ignoreCase(tokenType.getValue()),
                pushToken(tokenType)
            )
            : sequence(
                firstOf(
                    ignoreCase(tokenType.getValue()),
                    ignoreCase(tokenType.getShortValue())
                ),
                pushToken(tokenType)
            );
    }

    public Rule ciToken(final CaseInsensitiveValueTokenType tokenType)
    {
        return sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType));
    }
}
