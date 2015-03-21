package es.litesolutions.sonar.grappa;

import com.github.parboiled1.grappa.parsers.EventBusParser;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.support.Position;

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

    protected Rule valueToken(final ValueTokenType tokenType)
    {
        return sequence(tokenType.getValue(), pushToken(tokenType));
    }

    protected Rule shortValueToken(final ShortValueTokenType tokenType)
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

    protected Rule ciToken(final CaseInsensitiveValueTokenType tokenType)
    {
        return sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType));
    }
}
