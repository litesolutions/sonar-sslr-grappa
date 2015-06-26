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

package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.github.fge.grappa.run.ParseRunnerListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import es.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashSet;

/**
 * An SSL {@link Channel} associated with a Grappar parser {@link Rule}
 *
 * <p>This channel requires that the parser already be initialized; generally,
 * you will not initialize a channel directly but go through a {@link
 * GrappaSslrFactory} instead.</p>
 *
 * <p>Such a channel will always be asociated with at least a {@link
 * CodeReaderListener}, but you can add more listeners if you wish by
 * registering one or more {@link ListenerSupplier}.</p>
 *
 * @see CodeReaderListener
 */
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

    /**
     * Run the parser on the supplied {@link CodeReader}
     *
     * <p>This method will instantiate a {@link CodeReaderInputBuffer} over the
     * supplied code reader and feed it to the parser rule. If one or more
     * {@link ListenerSupplier}s are present, it also initializes the associated
     * parsing run listeners and registers them.</p>
     *
     * <p>The real token creation process is delegated to the generated {@link
     * CodeReaderListener}.</p>
     *
     * @param code the code to parse
     * @param output the lexer associated with the code
     * @return true or false if the code could be consumed
     *
     * @see ListeningParseRunner#registerListener(ParseRunnerListener)
     */
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

        /*
         * Because of the CodeReaderListener here, we know that we have consumed
         * the full input text; if this isn't the case (because of a parsing
         * failure or because not all the input was consumed), an exception will
         * have been thrown.
         *
         * We therefore pop() all the contents of the reader at this point...
         */

        final int length = code.length();

        for (int i = 0; i < length; i++)
            code.pop();

        return true;
    }
}
