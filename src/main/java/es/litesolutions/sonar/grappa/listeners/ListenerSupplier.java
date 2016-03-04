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

package es.litesolutions.sonar.grappa.listeners;

import com.github.fge.grappa.run.ParseRunnerListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface ListenerSupplier
{
    @Nonnull
    ParseRunnerListener<Token.Builder> create(final CodeReader codeReader,
        final Lexer lexer);
}
