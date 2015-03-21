package es.litesolutions.sonar.grappa;

import com.github.parboiled1.grappa.backport.EventBasedParseRunner;
import com.github.parboiled1.grappa.backport.ParseRunnerListener;
import com.github.parboiled1.grappa.backport.events.MatchFailureEvent;
import com.github.parboiled1.grappa.backport.events.MatchSuccessEvent;
import com.github.parboiled1.grappa.backport.events.PostParseEvent;
import com.github.parboiled1.grappa.backport.events.PreParseEvent;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.parboiled.MatcherContext;
import org.parboiled.matchers.Matcher;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.ValueStack;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * @see EventBasedParseRunner
 * @see SonarParserBase
 */
public final class CodeReaderListener
    extends ParseRunnerListener<Token.Builder>
{
    private final Lexer lexer;
    private final CodeReader reader;

    /*
     * The root matcher. We get it from the initial root context.
     */
    private Matcher rootMatcher;
    /*
     * The number of characters consumed by the root matcher.
     */
    private int consumed;

    public CodeReaderListener(final Lexer lexer, final CodeReader reader)
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
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
        consumed = context.getCurrentIndex();
    }

    @Override
    public void matchFailure(final MatchFailureEvent<Token.Builder> event)
    {
        final MatcherContext<Token.Builder> context = event.getContext();
        if (context.getLevel() != 0)
            return;
        if (context.getMatcher() != rootMatcher)
            throw new IllegalStateException("was expecting root rule here");
        consumed = context.getCurrentIndex();
    }

    @Override
    public void afterParse(final PostParseEvent<Token.Builder> event)
    {
        /*
         * We want a match
         */
        final ParsingResult<Token.Builder> result = event.getResult();
        if (!result.isSuccess())
            throw new IllegalStateException("match failure (consumed: "
                + consumed);

        /*
         * Pop all the consumed characters from the code reader.
         *
         * Since grappa 1.0.x/parboiled is used, we must take care not to go
         * beyond the end of input which may happen if the EOI matcher is used.
         */
        final int length = reader.length();

        final int realConsumed = Math.min(consumed, length);

        if (realConsumed != length)
            throw new IllegalStateException("was expecting to fully match, but"
                + " only " + realConsumed + " chars were matched out of "
                + length);

        for (int i = 0; i < realConsumed; i++)
            reader.pop();

        final ValueStack<Token.Builder> stack = result.getValueStack();

        final List<Token.Builder> list = new ArrayList<>(stack.size());
        while (!stack.isEmpty())
            list.add(stack.pop());

        Collections.reverse(list);

        final URI uri = lexer.getURI();
        for (final Token.Builder builder: list)
            lexer.addToken(builder.setURI(uri).build());
    }
}
