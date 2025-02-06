package org.litesolutions.sonar.grappa;

import com.sonar.sslr.api.AstNode;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.matcher.RuleDefinition;
import org.sonar.sslr.internal.matchers.LexerfulAstCreator;
import org.sonar.sslr.internal.vm.CompilableGrammarRule;
import org.sonar.sslr.internal.vm.CompiledGrammar;
import org.sonar.sslr.internal.vm.Machine;
import org.sonar.sslr.internal.vm.MutableGrammarCompiler;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

public class GrappaSslrParser<G extends Grammar> extends Parser<G> {

    private RuleDefinition rootRule;
    private final GrappaSslrLexer lexer;
    private final G grammar;

    /**
     * @since 1.16
     * @param grammar grammar
     */
    protected GrappaSslrParser(G grammar) {
        super(grammar);
        this.grammar = grammar;
        lexer = null;
    }

    private GrappaSslrParser(GrappaSslrParser.Builder<G> builder) {
        super(builder.grammar);
        this.lexer = builder.lexer;
        this.grammar = builder.grammar;
        this.rootRule = (RuleDefinition) this.grammar.getRootRule();
    }

    public AstNode parse(@Nonnull File file) {
        lexer.lex(file);
        return parse(lexer.getTokens());
    }

    public AstNode parse(@Nonnull String source) {
        lexer.lex(source);
        return parse(lexer.getTokens());
    }

    public AstNode parse(@Nonnull List<Token> tokens) {
        CompiledGrammar g = MutableGrammarCompiler.compile(rootRule);
        return LexerfulAstCreator.create(Machine.parse(tokens, g), tokens);
    }

    public G getGrammar() {
        return grammar;
    }

    public RuleDefinition getRootRule() {
        return rootRule;
    }

    public void setRootRule(@Nonnull Rule rootRule) {
        this.rootRule = (RuleDefinition) rootRule;
    }

    public static <G extends Grammar> GrappaSslrParser.Builder<G> grappaBuilder(G grammar) {
        return new GrappaSslrParser.Builder<>(grammar);
    }

    public static <G extends Grammar> GrappaSslrParser.Builder<G> grappaBuilder(GrappaSslrParser<G> parser) {
        return new GrappaSslrParser.Builder<>(parser);
    }

    public static final class Builder<G extends Grammar> {

        private GrappaSslrParser<G> baseParser;
        private GrappaSslrLexer lexer;
        private final G grammar;

        private Builder(G grammar) {
            this.grammar = grammar;
        }

        private Builder(GrappaSslrParser<G> parser) {
            this.baseParser = parser;
            this.lexer = parser.lexer;
            this.grammar = parser.grammar;
        }

        public GrappaSslrParser<G> build() {
            return new GrappaSslrParser<>(this);
        }

        public GrappaSslrParser.Builder<G> withLexer(GrappaSslrLexer lexer) {
            this.lexer = lexer;
            return this;
        }

    }

}
