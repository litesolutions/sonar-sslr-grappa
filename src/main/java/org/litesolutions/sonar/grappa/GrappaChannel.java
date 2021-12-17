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

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ListeningParseRunner;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.litesolutions.sonar.grappa.listeners.ListenerSupplier;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * The one and only channel necessary for a Grappa-based {@link Lexer}
 *
 */
@ParametersAreNonnullByDefault
public final class GrappaChannel
    extends Channel<GrappaSslrLexer>
{
    private final Rule rule;

    private final Collection<ListenerSupplier> suppliers = new ArrayList<>();

    /**
     * Constructor
     *
     * @param rule the (grappa) parser rule
     */
    public GrappaChannel(final Rule rule)
    {
        this.rule = rule;
        suppliers.add(CodeReaderListener::new);
    }

    /**
     * Add one listener to the parsing process
     *
     * <p>By default, the only listener defined will be a {@link
     * CodeReaderListener}, since it is necessary to add the generated tokens
     * to the lexer.</p>
     *
     * <p>You can use this method to add more listeners if you wish.</p>
     *
     * @param supplier the supplier
     */
    public void addListenerSupplier(final ListenerSupplier supplier)
    {
        suppliers.add(Objects.requireNonNull(supplier));
    }

    @Override
    public boolean consume(final CodeReader code, final GrappaSslrLexer output)
    {
        final InputBuffer buffer = new CodeReaderInputBuffer(code);

        final ListeningParseRunner<Token.Builder> runner
            = new ListeningParseRunner<>(rule);

        suppliers.stream().map(supplier -> supplier.create(code, output))
            .forEach(runner::registerListener);

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
