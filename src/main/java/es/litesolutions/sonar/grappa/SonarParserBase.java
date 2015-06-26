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

import com.github.fge.grappa.annotations.Cached;
import com.github.fge.grappa.annotations.DontExtend;
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
import es.litesolutions.sonar.grappa.tokentypes.WithValues;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    public boolean setAsComment()
    {
        return pushToken(GenericTokenType.COMMENT);
    }

    protected Rule shortValueToken(final ShortValueTokenType tokenType)
    {
        return tokenType instanceof CaseInsensitive
            ? doShortCaseInsensitive(tokenType)
            : doShortCaseSensitive(tokenType);
    }

    protected <T extends WithValue> Rule token(final T tokenType)
    {
        return tokenType instanceof CaseInsensitive
            ? sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType))
            : sequence(tokenType.getValue(), pushToken(tokenType));
    }

    protected Rule doShortCaseInsensitive(final ShortValueTokenType tokenType)
    {
        return tokenType.getShortValue() == null
            ? sequence(ignoreCase(tokenType.getValue()), pushToken(tokenType))
            : sequence(
                trieIgnoreCase(tokenType.getValue(), tokenType.getShortValue()),
                pushToken(tokenType)
            );
    }

    protected Rule doShortCaseSensitive(final ShortValueTokenType tokenType)
    {
        return tokenType.getShortValue() == null
            ? sequence(tokenType.getValue(), pushToken(tokenType))
            : sequence(
                trie(tokenType.getValue(), tokenType.getShortValue()),
                pushToken(tokenType)
            );
    }

    @SuppressWarnings("ConstantConditions")
    @Cached
    public Rule values(final WithValues withValues)
    {
        final Rule rule = withValues instanceof CaseInsensitive
            ? trieIgnoreCase(withValues.getValues())
            : trie(withValues.getValues());
        return sequence(rule, pushToken(withValues));
    }

    @SuppressWarnings("unchecked")
    @Cached
    public <T extends WithValue> Rule oneTokenAmong(final Function<String, T> f,
        final T... tokens)
    {
        return sequence(
            buildTrie(tokens),
            pushToken(f.apply(match()))
        );
    }

    @SuppressWarnings("unchecked")
    @DontExtend
    protected <T extends WithValue> Rule buildTrie(final T... tokens)
    {
        if (tokens.length == 0)
            throw new IllegalStateException("token list must not be empty");

        final T first = tokens[0];
        final boolean withShortValue = first instanceof ShortValueTokenType;

        final List<String> list = new ArrayList<>();

        for (final T token: tokens) {
            list.add(token.getValue());
            if (withShortValue
                && ((ShortValueTokenType) token).getShortValue() != null)
                list.add(((ShortValueTokenType) token).getShortValue());
        }

        return first instanceof CaseInsensitive ? trieIgnoreCase(list)
            : trie(list);
    }
}
