/*
 * SonarSource Language Recognizer
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.sslr.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.api.PreprocessorAction;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sonar.sslr.api.GenericTokenType.EOF;

public final class Lexer
{

    private final Charset charset;
    private final CodeReaderConfiguration configuration;
    private final ChannelDispatcher<Lexer> channelDispatcher;
    private final Preprocessor[] preprocessors;
    private final List<Trivia> trivia = new LinkedList<>();
    private URI uri;
    private List<Token> tokens = new ArrayList<>();

    private Lexer(final Builder builder)
    {
        charset = builder.charset;
        preprocessors = builder.preprocessors.toArray(
            new Preprocessor[builder.preprocessors.size()]);
        configuration = builder.configuration;
        channelDispatcher = builder.getChannelDispatcher();

        try {
            uri = new URI("tests://unittest");
        } catch (URISyntaxException e) {
            // Can't happen
            throw new IllegalStateException(e);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public List<Token> lex(final File file)
    {
        checkNotNull(file, "file cannot be null");
        checkArgument(file.isFile(), "file \"%s\" must be a file",
            file.getAbsolutePath());

        try {
            return lex(file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new LexerException(
                "Unable to lex file: " + file.getAbsolutePath(), e);
        }
    }

    public List<Token> lex(final URL url)
    {
        checkNotNull(url, "url cannot be null");

        InputStreamReader reader = null;
        try {
            uri = url.toURI();

            reader = new InputStreamReader(url.openStream(), charset);
            return lex(reader);
        } catch (Exception e) {
            throw new LexerException("Unable to lex url: " + getURI(), e);
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    /**
     * Do not use this method, it is intended for internal unit testing only
     *
     * @param sourceCode
     * @return
     */
    @VisibleForTesting
    public List<Token> lex(final String sourceCode)
    {
        checkNotNull(sourceCode, "sourceCode cannot be null");

        try {
            return lex(new StringReader(sourceCode));
        } catch (Exception e) {
            throw new LexerException(
                "Unable to lex string source code \"" + sourceCode + "\"", e);
        }
    }

    private List<Token> lex(final Reader reader)
    {
        tokens = Lists.newArrayList();

        initPreprocessors();
        final CodeReader code = new CodeReader(reader, configuration);
        try {
            channelDispatcher.consume(code, this);

            addToken(Token.builder().setType(EOF).setValueAndOriginalValue(
                "EOF").setURI(uri).setLine(code.getLinePosition()).setColumn(
                code.getColumnPosition()).build());

            preprocess();

            return getTokens();
        } catch (Exception e) {
            throw new LexerException(
                "Unable to lex source code at line : " + code.getLinePosition()
                    + " and column : " + code.getColumnPosition()
                    + " in file : " + uri, e);
        }
    }

    private void preprocess()
    {
        for (final Preprocessor preprocessor : preprocessors) {
            preprocess(preprocessor);
        }
    }

    private void preprocess(final Preprocessor preprocessor)
    {
        final List<Token> remainingTokens = Collections.unmodifiableList(
            new ArrayList<>(tokens));
        tokens.clear();

        int i = 0;
        while (i < remainingTokens.size()) {
            final PreprocessorAction action = preprocessor.process(
                remainingTokens.subList(i, remainingTokens.size()));
            checkNotNull(action,
                "A preprocessor cannot return a null PreprocessorAction");

            addTrivia(action.getTriviaToInject());

            for (int j = 0; j < action.getNumberOfConsumedTokens(); j++) {
                final Token removedToken = remainingTokens.get(i);
                i++;
                addTrivia(removedToken.getTrivia());
            }

            for (final Token tokenToInject : action.getTokensToInject()) {
                addToken(tokenToInject);
            }

            if (action.getNumberOfConsumedTokens() == 0) {
                final Token removedToken = remainingTokens.get(i);
                i++;
                addTrivia(removedToken.getTrivia());
                addToken(removedToken);
            }
        }
    }

    private void initPreprocessors()
    {
        for (final Preprocessor preprocessor : preprocessors) {
            preprocessor.init();
        }
    }

    public void addTrivia(final Trivia... trivia)
    {
        addTrivia(Arrays.asList(trivia));
    }

    public void addTrivia(final List<Trivia> trivia)
    {
        checkNotNull(trivia, "trivia cannot be null");

        this.trivia.addAll(trivia);
    }

    public void addToken(final Token... tokens)
    {
        checkArgument(tokens.length > 0, "at least one token must be given");

        final Token firstToken = tokens[0];
        final Token firstTokenWithTrivia;

        // Performance optimization: no need to rebuild token, if there is no
        // trivia
        if (trivia.isEmpty() && !firstToken.hasTrivia()) {
            firstTokenWithTrivia = firstToken;
        } else {
            firstTokenWithTrivia = Token.builder(firstToken).setTrivia(trivia)
                .build();
            trivia.clear();
        }

        this.tokens.add(firstTokenWithTrivia);
        if (tokens.length > 1) {
            this.tokens.addAll(Arrays.asList(tokens).subList(1, tokens.length));
        }
    }

    public List<Token> getTokens()
    {
        return Collections.unmodifiableList(tokens);
    }

    public URI getURI()
    {
        return uri;
    }

    public static final class Builder
    {

        private final List<Preprocessor> preprocessors
            = new ArrayList<>();
        private final CodeReaderConfiguration configuration
            = new CodeReaderConfiguration();
        private final List<Channel<Lexer>> channels
            = new ArrayList<>();
        private Charset charset = Charset.defaultCharset();
        private boolean failIfNoChannelToConsumeOneCharacter = false;

        private Builder()
        {
        }

        public Lexer build()
        {
            return new Lexer(this);
        }

        public Builder withCharset(final Charset charset)
        {
            this.charset = charset;
            return this;
        }

        public Builder withPreprocessor(final Preprocessor preprocessor)
        {
            preprocessors.add(preprocessor);
            return this;
        }

        public Builder withChannel(final Channel<Lexer> channel)
        {
            channels.add(channel);
            return this;
        }

        public Builder withFailIfNoChannelToConsumeOneCharacter(
            final boolean failIfNoChannelToConsumeOneCharacter)
        {
            this.failIfNoChannelToConsumeOneCharacter
                = failIfNoChannelToConsumeOneCharacter;
            return this;
        }

        private ChannelDispatcher<Lexer> getChannelDispatcher()
        {
            final ChannelDispatcher.Builder builder = ChannelDispatcher.builder()
                .addChannels(channels.toArray(new Channel[channels.size()]));

            if (failIfNoChannelToConsumeOneCharacter) {
                builder.failIfNoChannelToConsumeOneCharacter();
            }

            return builder.build();
        }

    }
}
