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
