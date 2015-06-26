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

package es.litesolutions.sonar.grappa.tokentypes;

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
    extends WithValue
{
    @Nullable
    String getShortValue();
}
