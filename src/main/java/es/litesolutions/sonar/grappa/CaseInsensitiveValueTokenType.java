package es.litesolutions.sonar.grappa;

import com.sonar.sslr.api.TokenType;

/**
 * Interface for token types whose value is case insensitive
 *
 * <p>Implement this interface if this token type's {@link TokenType#getValue()}
 * method returns a case insensitive literal representing the parsed value.</p>
 */
public interface CaseInsensitiveValueTokenType
    extends TokenType
{
}
