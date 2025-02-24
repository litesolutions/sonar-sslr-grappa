/*
 * Copyright (c) 2016 Lite Solutions
 *
 *  This code is licensed under the Apache Software License version 2.
 *  For more information, see the LICENSE file at the root of this package.
 *
 *  Should you not have the source code available, and the file above is
 *  unavailable, you can obtain a copy of the license here:
 *
 *  https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 */

 package org.litesolutions.sonar.grappa.listeners;

 import javax.annotation.Nonnull;

 import org.litesolutions.sonar.grappa.GrappaChannel;
 import org.litesolutions.sonar.grappa.GrappaSslrLexer;
 import org.sonar.sslr.channel.CodeReader;

 import com.github.fge.grappa.run.ParseEventListener;
 import com.sonar.sslr.api.Token;
 import com.sonar.sslr.impl.Lexer;

 /**
  * Create a {@link ParseEventListener} given a {@link CodeReader} and a {@link
  * Lexer} as arguments
  *
  * <p>This allows you to register listeners to a {@link GrappaChannel}.</p>
  */
 @FunctionalInterface
 public interface ListenerSupplier
 {
     @Nonnull
     ParseEventListener<Token.Builder> create(final CodeReader reader,
         final GrappaSslrLexer lexer);
 }
