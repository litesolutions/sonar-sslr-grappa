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

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A token having a fixed set of values
 *
 * @see SonarParserBase#values(WithValues)
 */
// TODO: default method for .hasToBeSkippedFromAst()
public interface WithValues
    extends TokenType
{
    @Nullable
    Collection<String> getValues();
}
