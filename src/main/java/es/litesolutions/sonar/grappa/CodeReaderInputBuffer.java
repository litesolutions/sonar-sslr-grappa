/*
 * Copyright (C) 2014 Francis Galiegue <fgaliegue@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.litesolutions.sonar.grappa;

import com.github.fge.grappa.buffers.CharSequenceInputBuffer;
import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.buffers.LineCounter;
import com.github.fge.grappa.support.Chars;
import com.github.fge.grappa.support.IndexRange;
import com.github.fge.grappa.support.Position;
import org.sonar.sslr.channel.CodeBuffer;
import org.sonar.sslr.channel.CodeReader;
import r.com.google.common.base.Preconditions;
import r.com.google.common.collect.Range;
import r.com.google.common.util.concurrent.Futures;
import r.com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.nio.CharBuffer;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * An {@link InputBuffer} over a {@link CodeReader}
 *
 * <p>Unfortunately, this is required. Sonar's {@link CodeBuffer} does not
 * support {@link CharSequence#subSequence(int, int)}... So this is basically a
 * {@link CharSequenceInputBuffer} with subsequence extraction rewritten.</p>
 *
 */
@Immutable
public final class CodeReaderInputBuffer
    implements InputBuffer
{
    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        final ThreadFactory factory = new ThreadFactoryBuilder()
            .setDaemon(true).setNameFormat("linecounter-thread-%d").build();
        EXECUTOR_SERVICE = Executors.newCachedThreadPool(factory);
    }

    private final CodeReader reader;
    private final Future<LineCounter> lineCounter;

    public CodeReaderInputBuffer(@Nonnull final CodeReader reader)
    {
        this.reader = Objects.requireNonNull(reader);
        lineCounter = EXECUTOR_SERVICE.submit(new Callable<LineCounter>()
        {
            @Override
            public LineCounter call()
            {
                return new LineCounter(reader);
            }
        });
    }

    @Override
    public char charAt(final int index)
    {
        return index >= 0 && index < reader.length()
            ? reader.charAt(index) : Chars.EOI;
    }

    /**
     * Returns the Unicode code point starting at a given index
     * <p>If the index is greater than, or equal to, the buffer's length, this
     * method returns -1.</p>
     *
     * @param index the index
     * @return the code point at this index, or -1 if the end of input has been
     * reached
     *
     * @throws IllegalArgumentException index is negative
     */
    @Override
    public int codePointAt(final int index)
    {
        final int length = reader.length();
        if (index >= length)
            return -1;
        if (index < 0)
            throw new IllegalArgumentException("index is negative");

        final char c = reader.charAt(index);
        if (!Character.isHighSurrogate(c))
            return c;
        if (index == length - 1)
            return c;
        final char c2 = reader.charAt(index + 1);
        return Character.isLowSurrogate(c2) ? Character.toCodePoint(c, c2) : c;
    }

    @Override
    public boolean test(final int index, final char[] characters)
    {
        final int length = characters.length;
        if (index + length > reader.length())
            return false;
        for (int i = 0; i < length; i++)
            if (reader.charAt(index + i) != characters[i])
                return false;
        return true;
    }

    @Override
    public String extract(final int start, final int end)
    {
        final int realStart = Math.max(start, 0);
        final int realEnd = Math.min(end, reader.length());
        final CharBuffer buf = CharBuffer.allocate(realEnd - realStart);
        for (int i = realStart; i < realEnd; i++)
            buf.put(charAt(i));
        return new String(buf.array());
    }

    @Override
    public String extract(final IndexRange range)
    {
        return extract(range.start, range.end);
    }

    @Override
    public Position getPosition(final int index)
    {
        /*
         * A CodeReader column index starts at 0, not 1; we therefore need to
         * substract one from the extracted position...
         */
        final Position position
            = Futures.getUnchecked(lineCounter).toPosition(index);
        return new Position(position.getLine(), position.getColumn() - 1);
    }

    @Override
    public String extractLine(final int lineNumber)
    {
        Preconditions.checkArgument(lineNumber > 0, "line number is negative");
        final LineCounter counter = Futures.getUnchecked(lineCounter);
        final Range<Integer> range = counter.getLineRange(lineNumber);
        final int start = range.lowerEndpoint();
        int end = range.upperEndpoint();
        if (charAt(end - 1) == '\n')
            end--;
        if (charAt(end - 1) == '\r')
            end--;
        return extract(start, end);
    }

    /**
     * Get the index range matching a given line number
     *
     * @param lineNumber the line number
     * @return the index range
     */
    @Override
    public IndexRange getLineRange(final int lineNumber)
    {
        final Range<Integer> range
            = Futures.getUnchecked(lineCounter).getLineRange(lineNumber);
        return new IndexRange(range.lowerEndpoint(), range.upperEndpoint());
    }

    @Override
    public int getLineCount()
    {
        return Futures.getUnchecked(lineCounter).getNrLines();
    }

    @Override
    public int length()
    {
        return reader.length();
    }
}
