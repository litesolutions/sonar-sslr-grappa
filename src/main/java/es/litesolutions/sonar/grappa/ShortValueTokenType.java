package es.litesolutions.sonar.grappa;

import com.sonar.sslr.api.TokenType;

import javax.annotation.Nullable;

/**
 * Token types which also have a short value (potentially null)
 *
 * <p>Implement this interface when a token type meets the following conditions:
 * </p>
 *
 * <ul>
 *     <li>its {@link TokenType#getValue()} method returns a <strong>case
 *     insensitive</strong> string to match the long version;</li>
 *     <li>its {@link #getShortValue()} method returns a <strong>case
 *     insensitive</strong> string to match the short version.</li>
 * </ul>
 */
public interface ShortValueTokenType
    extends TokenType
{
    @Nullable
    String getShortValue();
}
