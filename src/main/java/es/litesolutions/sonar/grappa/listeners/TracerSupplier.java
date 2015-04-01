package es.litesolutions.sonar.grappa.listeners;

import com.github.fge.grappa.run.ParseRunnerListener;
import com.github.fge.grappa.run.trace.TracingListener;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;
import es.litesolutions.sonar.grappa.GrappaChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.channel.CodeReader;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.UUID;

/**
 * A supplier of {@link TracingListener} instances to associate with a {@link
 * GrappaChannel}
 *
 * <p>You supply the constructor with either a {@link Path}, or string,
 * representing the directory of your choice, or no arguments in which case the
 * directory will be automatically generated using {@link
 * Files#createTempDirectory(String, FileAttribute[])}.</p>
 *
 * <p>The path to the generated trace files is {@link Logger#info(String,
 * Object) logged} (at level {@code INFO}).</p>
 *
 * @see TracingListener
 */
@ParametersAreNonnullByDefault
public final class TracerSupplier
    implements ListenerSupplier
{
    private static final Logger LOGGER
        = LoggerFactory.getLogger(TracerSupplier.class);

    private final Path baseDir;

    /**
     * Default constructor: trace files are generated in a temporary directory
     */
    public TracerSupplier()
    {
        try {
            baseDir = Files.createTempDirectory("grappa-tracer");
        } catch (IOException e) {
            throw new RuntimeException("unable to create trace directory", e);
        }
    }

    /**
     * Constructor specifying the directory in which the trace files will be
     * generated
     *
     * @param baseDir the base directory
     */
    public TracerSupplier(final Path baseDir)
    {
        this.baseDir = Objects.requireNonNull(baseDir).toAbsolutePath();

        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("unable to create trace directory", e);
        }
    }

    /**
     * Same as {@link #TracerSupplier(Path)}, except the directory is specified
     * as a string
     *
     * @param baseDir the base directory
     *
     * @see Paths#get(String, String...)
     */
    public TracerSupplier(final String baseDir)
    {
        this(Paths.get(baseDir));
    }

    /**
     * Create a new {@link TracingListener} instance
     *
     * @param codeReader the code reader
     * @param lexer the lexter
     * @return a tracing listener
     *
     * @see GrappaChannel#consume(CodeReader, Lexer)
     */
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
