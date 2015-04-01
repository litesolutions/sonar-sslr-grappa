package es.litesolutions.sonar.grappa.listeners;

import com.github.fge.grappa.run.ParseRunnerListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import es.litesolutions.sonar.grappa.GrappaChannel;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.Nonnull;

/**
 * A supplier of {@link ParseRunnerListener}s for a {@link GrappaChannel}
 */
public interface ListenerSupplier
{
    /**
     * Create a new listener
     *
     * @param codeReader the code reader
     * @param lexer the associated lexer
     * @return a parse runner listener
     */
    @Nonnull
    ParseRunnerListener<Token.Builder> create(final CodeReader codeReader,
        final Lexer lexer);
}
