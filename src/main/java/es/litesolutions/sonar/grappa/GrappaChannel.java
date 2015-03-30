package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashSet;

@ParametersAreNonnullByDefault
public final class GrappaChannel
    extends Channel<Lexer>
{
    private final Rule rule;

    private final Collection<ListenerSupplier> suppliers = new HashSet<>();

    public GrappaChannel(final Rule rule)
    {
        this.rule = rule;
    }

    public void addListenerSupplier(final ListenerSupplier supplier)
    {
        suppliers.add(supplier);
    }

    @Override
    public boolean consume(final CodeReader code, final Lexer output)
    {
        final InputBuffer buffer = new CodeReaderInputBuffer(code);

        final CodeReaderListener listener
            = new CodeReaderListener(output, code);

        final ListeningParseRunner<Token.Builder> runner
            = new ListeningParseRunner<>(rule);

        runner.registerListener(listener);
        for (final ListenerSupplier supplier: suppliers)
            runner.registerListener(supplier.create(code, output));

        runner.run(buffer);

        return true;
    }
}
