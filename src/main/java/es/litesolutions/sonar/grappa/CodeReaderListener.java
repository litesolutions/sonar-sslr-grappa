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

package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.exceptions.GrappaException;
import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParseRunnerListener;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.run.events.MatchFailureEvent;
import com.github.fge.grappa.run.events.MatchSuccessEvent;
import com.github.fge.grappa.run.events.PostParseEvent;
import com.github.fge.grappa.run.events.PreParseEvent;
import com.github.fge.grappa.stack.ValueStack;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Lexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.net.URI;

/**
 * A parsing listener associated with a Sonar {@link CodeReader} and {@link
 * Lexer}
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
 * @see ListeningParseRunner
 * @see SonarParserBase
 */
public final class CodeReaderListener
    extends ParseRunnerListener<Token.Builder>
{
    private final Lexer lexer;
    private final CodeReader reader;
    private static final Logger LOG = LoggerFactory.getLogger(CodeReaderListener.class);

    /*
     * The root matcher. We get it from the initial root context.
     */
    private Matcher rootMatcher = null;
    /*
     * The number of characters consumed by the root matcher.
     */
    private int consumed = 0;

    public CodeReaderListener(final CodeReader reader, final Lexer lexer)
    {
        this.lexer = lexer;
        this.reader = reader;
    }

    @Override
    public void beforeParse(final PreParseEvent<Token.Builder> event)
    {
        final MatcherContext<Token.Builder> context = event.getContext();
        rootMatcher = context.getMatcher();
    }

    @Override
    public void matchSuccess(final MatchSuccessEvent<Token.Builder> event)
    {
        final MatcherContext<Token.Builder> context = event.getContext();
        if (!context.inPredicate())
            consumed = Math.max(consumed, context.getCurrentIndex());
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
    }

    @Override
    public void matchFailure(final MatchFailureEvent<Token.Builder> event)
    {
        final MatcherContext<Token.Builder> context = event.getContext();
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
    }

    @Override
    public void afterParse(final PostParseEvent<Token.Builder> event)
    {
        final int length = reader.length();

        /*
         * We want a match
         */

        final ParsingResult<Token.Builder> result = event.getResult();
        if (!result.isSuccess())
            throw new GrappaException("match failure (consumed: "
                + consumed + " out of " + length + ')');

        /*
         * Check that we did consume all the text
         */

        if (consumed != length){        	
        	int saltoLineaIni = consumed;
        	int saltoLineaEnd = consumed;
        	for(int i=consumed; i>0;i--){
        		char u =  reader.charAt(i);
        		if (u == '\n' || u == '\r'){
        			saltoLineaIni = i;
        			break;
        		}
               
        	}
        	for(int i=consumed; i<length;i++){
        		char u =  reader.charAt(i);
        		if (u == '\n' || u == '\r'){
        			saltoLineaEnd = i;
        			break;
        		}
               
        	}
        	String lineToShow = "";
        	for(int i=saltoLineaIni; i<saltoLineaEnd;i++){
        		lineToShow = lineToShow + reader.charAt(i);
        	}
        	
        	LOG.error("LINE ERROR: "+lineToShow);
        	
        	//Escribir de reader[consumed-10] a reader[consumed+10]
        	throw new GrappaException("was expecting to fully match, but only "
                    + consumed + " chars were matched out of " + length);
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
