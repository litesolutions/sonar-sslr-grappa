package es.litesolutions.sonar.grappa;

import com.github.parboiled1.grappa.backport.EventBasedParseRunner;
import com.github.parboiled1.grappa.backport.tracer.TracingListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.parboiled.Rule;
import org.parboiled.buffers.InputBuffer;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class GrappaChannel
    extends Channel<Lexer>
{
    private static final Path TRACEPATH = Paths.get("/tmp/trace.zip");

    private final Rule rule;

    public GrappaChannel(final Rule rule)
    {
        this.rule = rule;
    }

    @Override
    public boolean consume(final CodeReader code, final Lexer output)
    {
        final InputBuffer buffer = new CodeReaderInputBuffer(code);

        final CodeReaderListener listener
            = new CodeReaderListener(output, code);

        final TracingListener<Token.Builder> tracer;
        try {
            tracer = new TracingListener<>(TRACEPATH, true);
        } catch (IOException e) {
            throw new RuntimeException(e);

        }

        final EventBasedParseRunner<Token.Builder> runner
            = new EventBasedParseRunner<>(rule);

        runner.registerListener(listener);
        runner.registerListener(tracer);

        runner.run(buffer);

        return true;
    }
}
