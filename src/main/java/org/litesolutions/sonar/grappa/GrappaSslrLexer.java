package org.litesolutions.sonar.grappa;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.LexerException;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.ChannelDispatcher;
import org.sonar.sslr.channel.CodeReader;
import org.sonar.sslr.channel.CodeReaderConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sonar.sslr.api.GenericTokenType.EOF;

public class GrappaSslrLexer {

    private final Charset charset;
    private final CodeReaderConfiguration configuration;
    private final ChannelDispatcher<GrappaSslrLexer> channelDispatcher;

    private URI uri;
    private final List<Trivia> trivia = new LinkedList<>();
    private List<Token> tokens = new ArrayList<>();

    public GrappaSslrLexer(GrappaSslrLexer.Builder builder) {
        this.charset = builder.charset;
        this.configuration = builder.configuration;
        this.channelDispatcher = builder.getChannelDispatcher();

        try {
            this.uri = new URI("tests://unittest");
        } catch (URISyntaxException e) {
            // Can't happen
            throw new IllegalStateException(e);
        }

    }

    public List<Token> lex(File file) {
        checkNotNull(file, "file cannot be null");
        checkArgument(file.isFile(), "file \"%s\" must be a file", file.getAbsolutePath());

        try {
            return lex(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new LexerException("Unable to lex file: " + file.getAbsolutePath(), e);
        }
    }

    public List<Token> lex(URL url) {
        checkNotNull(url, "url cannot be null");

        try {
            this.uri = url.toURI();
            try (InputStreamReader reader = new InputStreamReader(url.openStream(), charset)) {
                return lex(reader);
            }
        } catch (RecognitionException e) {
            throw e;
        } catch (Exception e) {
            throw new LexerException("Unable to lex url: " + getURI(), e);
        }
    }

    /**
     * Do not use this method, it is intended for internal unit testing only
     *
     * @param sourceCode sourceCode
     * @return list tokens
     */
    @VisibleForTesting
    public List<Token> lex(String sourceCode) {
        checkNotNull(sourceCode, "sourceCode cannot be null");

        return lex(new StringReader(sourceCode));
    }

    private List<Token> lex(Reader reader) {
        tokens = Lists.newArrayList();

        CodeReader code = new CodeReader(reader, configuration);
        try {
            channelDispatcher.consume(code, this);

            addToken(Token.builder()
                    .setType(EOF)
                    .setValueAndOriginalValue("EOF")
                    .setURI(uri)
                    .setLine(code.getLinePosition())
                    .setColumn(code.getColumnPosition())
                    .build());

            return getTokens();
        } catch (Exception e) {
            throw new RecognitionException(code.getLinePosition(), "Unable to lex source code at line : " + code.getLinePosition() + " and column : "
                    + code.getColumnPosition() + " in file : " + uri);
        }
    }

    public void addTrivia(Trivia... trivia) {
        addTrivia(Arrays.asList(trivia));
    }

    public void addTrivia(List<Trivia> trivia) {
        checkNotNull(trivia, "trivia cannot be null");

        this.trivia.addAll(trivia);
    }

    public void addToken(Token... tokens) {
        checkArgument(tokens.length > 0, "at least one token must be given");

        Token firstToken = tokens[0];
        Token firstTokenWithTrivia;

        // Performance optimization: no need to rebuild token, if there is no trivia
        if (trivia.isEmpty() && !firstToken.hasTrivia()) {
            firstTokenWithTrivia = firstToken;
        } else {
            firstTokenWithTrivia = Token.builder(firstToken).setTrivia(trivia).build();
            trivia.clear();
        }

        this.tokens.add(firstTokenWithTrivia);
        if (tokens.length > 1) {
            this.tokens.addAll(Arrays.asList(tokens).subList(1, tokens.length));
        }
    }

    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokens);
    }

    public URI getURI() {
        return uri;
    }

    public static GrappaSslrLexer.Builder builder() {
        return new GrappaSslrLexer.Builder();
    }

    public static final class Builder {

        private Charset charset = Charset.defaultCharset();
        private final CodeReaderConfiguration configuration = new CodeReaderConfiguration();
        private final List<GrappaChannel> channels = new ArrayList<>();
        private boolean failIfNoChannelToConsumeOneCharacter = false;

        private Builder() {
            super();
        }

        public GrappaSslrLexer build() {
            return new GrappaSslrLexer(this);
        }

        public GrappaSslrLexer.Builder withCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public GrappaSslrLexer.Builder withChannel(GrappaChannel channel) {
            channels.add(channel);
            return this;
        }

        public GrappaSslrLexer.Builder withFailIfNoChannelToConsumeOneCharacter(boolean failIfNoChannelToConsumeOneCharacter) {
            this.failIfNoChannelToConsumeOneCharacter = failIfNoChannelToConsumeOneCharacter;
            return this;
        }

        private ChannelDispatcher<GrappaSslrLexer> getChannelDispatcher() {
            ChannelDispatcher.Builder builder = ChannelDispatcher.builder()
                    .addChannels(channels.toArray(new Channel[0]));

            if (failIfNoChannelToConsumeOneCharacter) {
                builder.failIfNoChannelToConsumeOneCharacter();
            }

            return builder.build();
        }

    }
}
