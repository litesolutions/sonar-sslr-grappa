package es.litesolutions.sonar.grappa.tokentypes;

import com.sonar.sslr.api.TokenType;
import es.litesolutions.sonar.grappa.SonarParserBase;

import javax.annotation.Nonnull;

/**
 * Interface to implement along with {@link TokenType} if the value of the token
 * is the text to match
 *
 * @see SonarParserBase#token(TokenType)
 * @see CaseInsensitive
 */
public interface WithValue
{
    @Nonnull
    String getValue();
}
