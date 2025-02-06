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

package org.litesolutions.sonar.grappa;

import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParseEventListener;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.run.events.MatchFailureEvent;
import com.github.fge.grappa.run.events.MatchSuccessEvent;
import com.github.fge.grappa.run.events.PostParseEvent;
import com.github.fge.grappa.run.events.PreParseEvent;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Position;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.net.URI;

/**
 * A parsing listener associated with a Sonar {@link CodeReader} and {@link
 * GrappaSslrLexer}
 *
 * <p>This listener will handle both the consumption of characters from the code
 * reader, and adding tokens to the lexer.</p>
 *
 * <p>For this, it captures the root matcher obtained from the initial context
 * (see {@link #beforeParse(PreParseEvent)}) and waits for this matcher to
 * reappear at level 0. When it reappears, it means the parsing is done, whether
 * successful or not.</p>
 *
 * <p>After the parsing is done, the consumed characters are {@link
 * CodeReader#pop() popped} from the reader and the recorded tokens are added
 * to the lexer.</p>
 *
 * @see Channel#consume(CodeReader, Object)
 * @see ParseRunner
 * @see SonarParserBase
 */
public final class CodeReaderListener
        extends ParseEventListener<Token.Builder> {
    private final GrappaSslrLexer lexer;
    private final CodeReader reader;

    /*
     * The root matcher. We get it from the initial root context.
     */
    private Matcher rootMatcher = null;
    /*
     * The number of characters consumed by the root matcher.
     */
    private int consumed = 0;
    private Position position = null;

    public CodeReaderListener(final CodeReader reader, final GrappaSslrLexer lexer) {
        this.lexer = lexer;
        this.reader = reader;
    }

    @Override
    public void beforeParse(final PreParseEvent<Token.Builder> event) {
        final MatcherContext<Token.Builder> context = event.getContext();
        rootMatcher = context.getMatcher();
    }

    @Override
    public void matchSuccess(final MatchSuccessEvent<Token.Builder> event) {
        final MatcherContext<Token.Builder> context = event.getContext();
        if (!context.inPredicate())
            consumed = Math.max(consumed, context.getCurrentIndex());
            this.position = context.getPosition();
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
    }

    @Override
    public void matchFailure(final MatchFailureEvent<Token.Builder> event) {
        final MatcherContext<Token.Builder> context = event.getContext();
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
    }

    @Override
    public void afterParse(final PostParseEvent<Token.Builder> event) {
        final int length = reader.length();

        /*
         * We want a match
         */

        final ParsingResult<Token.Builder> result = event.getResult();
        if (!result.isSuccess())
            throw new RecognitionException(position.getLine(), "match failure (consumed: "
                    + consumed + " out of " + length + ')');

        /*
         * Check that we did consume all the text
         */

        for (int i = 0; i< consumed; i++) {
            reader.pop();
        }

        if (consumed != length) {
            throw new RecognitionException(reader.getLinePosition(), "Parsing failure");
        }


        final ValueStack<Token.Builder> stack = result.getValueStack();

        final URI uri = lexer.getURI();
        final int size = stack.size();
        Token token;

        for (int index = size - 1; index >= 0; index--) {
            token = stack.peek(index).setURI(uri).build();
            if (token.getType() == GenericTokenType.COMMENT)
                lexer.addTrivia(Trivia.createComment(token));
            else
                lexer.addToken(token);
        }
    }
}
