package es.litesolutions.sonar.grappa.listeners;

import com.github.fge.grappa.run.ParseRunnerListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.Nonnull;

public interface ListenerSupplier
{
    @Nonnull
    ParseRunnerListener<Token.Builder> create(final CodeReader codeReader,
        final Lexer lexer);
}
