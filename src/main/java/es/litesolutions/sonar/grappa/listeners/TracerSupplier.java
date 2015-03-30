package es.litesolutions.sonar.grappa.listeners;

import com.github.fge.grappa.run.ParseRunnerListener;
import com.github.fge.grappa.run.trace.TracingListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

public final class TracerSupplier
    implements ListenerSupplier
{
    private static final Logger LOGGER
        = LoggerFactory.getLogger(TracerSupplier.class);

    private final Path baseDir;

    public TracerSupplier()
    {
        try {
            baseDir = Files.createTempDirectory("grappa-tracer");
        } catch (IOException e) {
            throw new RuntimeException("unable to create trace directory", e);
        }
    }

    public TracerSupplier(final Path baseDir)
    {
        this.baseDir = Objects.requireNonNull(baseDir);

        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("unable to create trace directory", e);
        }
    }

    public TracerSupplier(final String baseDir)
    {
        this(Paths.get(baseDir));
    }

    @Nonnull
    @Override
    public ParseRunnerListener<Token.Builder> create(
        final CodeReader codeReader, final Lexer lexer)
    {
        final String fileName
            = String.format("trace-%s.zip", UUID.randomUUID());
        final Path tracePath = baseDir.resolve(fileName);
        final TracingListener<Token.Builder> ret;

        try {
            ret = new TracingListener<>(tracePath, true);
        } catch (IOException e) {
            throw new RuntimeException("unable to create trace file", e);
        }

        LOGGER.info("recording trace file {} for URI {}", tracePath,
            lexer.getURI());
        return ret;
    }
}
