package es.litesolutions.sonar.grappa;

import com.sonar.sslr.api.TokenType;

/**
 * Interface for token types having well known values
 *
 * <p>Implement this interface if this token type's {@link TokenType#getValue()}
 * method returns a literal representing the parsed value.</p>
 */
public interface ValueTokenType
    extends TokenType
{
}
