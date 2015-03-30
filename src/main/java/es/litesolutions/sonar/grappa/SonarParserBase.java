package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.parsers.ListeningParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.context.Context;
import com.github.fge.grappa.support.Position;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import es.litesolutions.sonar.grappa.tokentypes.CaseInsensitive;
import es.litesolutions.sonar.grappa.tokentypes.ShortValueTokenType;
import es.litesolutions.sonar.grappa.tokentypes.WithValue;

@SuppressWarnings("AutoBoxing")
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

    public boolean setAsComment()
    {
        return pushToken(GenericTokenType.COMMENT);
    }

    protected Rule shortValueToken(final ShortValueTokenType tokenType)
    {
        return tokenType instanceof CaseInsensitive
            ? doShortCaseInsensitive(tokenType).label(tokenType.getValue())
            : doShortCaseSensitive(tokenType).label(tokenType.getValue());
    }

    protected <T extends TokenType & WithValue> Rule token(final T tokenType)
    {
        return tokenType instanceof CaseInsensitive
            ? sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType))
                .label(tokenType.getValue())
            : sequence(tokenType.getValue(), pushToken(tokenType))
                .label(tokenType.getValue());
    }

    protected Rule doShortCaseInsensitive(final ShortValueTokenType tokenType)
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

    protected Rule doShortCaseSensitive(final ShortValueTokenType tokenType)
    {
        return tokenType.getShortValue() == null
            ? sequence(
                tokenType.getValue(),
                pushToken(tokenType)
            )
            : sequence(
                firstOf(
                    tokenType.getValue(),
                    tokenType.getShortValue()
                ),
                pushToken(tokenType)
            );
    }
}
