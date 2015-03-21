## Read me first

This package has the same license as Sonar's SSLR package itself: LGPLv3.

Please see the file LICENSE for more details.

## What this is

This package is a sample implementation of an SSLR `Channel` using a parser written with
[grappa](https://github.com/fge/grappa).

Requires Java 7+.

## Rationale

SSLR means SonarSource Language Recognizer. This is the mechanism used by SonarQube to match and
tokenize languages. It relies on two main elements:

* `Channel`s, to parse the language and produce tokens;
* `GrammarRuleKey`s to create nodes for the language's AST (Abstract Syntax Tree).

The existing channels provided by SonarQube require that you create [many, many, many of
them](https://github.com/SonarCommunity/sonar-python/blob/master/python-squid/src/main/java/org/sonar/python/lexer/PythonLexer.java)
to parse a single language.

What is more, since the channel dispatcher will try all channels (IN THE ORDER IN WHICH YOU DECLARE
THEM!) you have to make sure that not only the channels are declared in the correct order, but also
that [your channel includes a lot of boilerplate code](https://github.com/SonarCommunity/sonar-python/blob/master/python-squid/src/main/java/org/sonar/python/lexer/IndentationChannel.java)...

Whereas with this package, it is as simple as:

* writing a parser for your language,
* pushing the tokens you need (see below),
* and writing such a simple lexer as:

```java
final MyLanguageParser parser = Parboiled.createParser(MyLanguageParser.class);

final Channel<Lexer> channel = new GrappaChannel(parser.theRule());

final Lexer lexer = Lexer.builder()
    .withFailIfNoChannelToConsumeOneCharacter(true)
    .withChannel(channel)
    .build();
```

That's it! 

Should you wonder what a grappa parser looks like, [here is a parser which parses any, and all,
JSON, as defined by RFC
7159](https://github.com/fge/grappa-examples/blob/master/src/main/java/com/github/fge/grappa/examples/json/JsonParser.java).

There are many advantages to proceeding this way:

### Grappa can do much more than any other Channel

It's a full fledged parser after all! Here is how, for instance, you would match text between
parens, WITH included, matching parens:

```java

// Even matches no text at all
public Rule embeddedParens()
{
    return join(zeroOrMore(noneOf("()"))
        .using(sequence('(', embeddedParens(), ')')
        .min(0);
}
```

### Much easier to debug

Suppose you have a complex set of `GrammarRuleKey`s; you have a root rule to set in order for a
Sonar `Parser` to be legal.

Now, you may want to test only _part_ of your grammar.

Well, with this package, and provided you write your grammar rule implementation accordingly, it's
very easy:

```java

        /*
         * Initialize your language parser and your rule
         */

        final MyParser parser
            = Parboiled.createParser(MyParser.class);
        final Rule rule = parser.someRule();
        final Channel<Lexer> channel = new GrappaChannel(rule);

        /*
         * Initialize the lexer; only one channel!
         */

        final Lexer lexer = Lexer.builder()
            .withFailIfNoChannelToConsumeOneCharacter(true)
            .withChannel(channel)
            .build();

        /*
         * Initialize your rules; here we take the example of a grammar which
         * has no root rule, but which we inject into a builder.
         *
         * Match the GrammarRuleKey with the grappa parser rule.
         */

        final LexerfulGrammarBuilder builder = LexerfulGrammarBuilder.create();
        MyGrammar.injectInto(builder);
        builder.setRootRule(MyGrammar.MY_RULE_KEY);

        /*
         * Finally, create the Grammar object, the Parser object, initialize
         * the toolkit and run it
         */

        final Grammar grammar = builder.build();

        final Parser<Grammar> grammarParser = Parser.builder(grammar)
            .withLexer(lexer).build();

        final Toolkit toolkit = new Toolkit("test",
            new DummyConfigurationModel(grammarParser));

        toolkit.run();
```

You then get a toolkit window to test _that_ part of your grammar, and only _that_ part.

Not only that, but you can also modify the channel easily so as to use the [grappa
debugger](https://github.com/fge/grappa-debugger) to test your parser itself, that is, your channel!
Using it you know exactly what parser rules matched, along with some statistics you may also find
useful.

## How this works

In order to write a parser, you will need to extend `SonarParserBase`; this (abstract) parser
implementation extends `EventBusParser<Token.Builder>` (which, by the way, means you can register
any listener to your parser implementation provided it has the necessary annotations; see
[`EventBus`](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/eventbus/EventBus.html)).

What you push is not a token directly but an instance of `Token.Builder`; all you have to do is use
the method `pushToken(someTokenType)`, with `someTokenType` being an instance of `TokenType`.

This method will grab the indices of the text matched by the _immediately preceding rule_ and
produce a `Token.Builder` on the parser's stack (which is unwound when the parsing finishes). For
instance:

```java
public Rule lotsOfAs()
{
    return sequence(oneOrMore('a'), pushToken(MyTokens.LOTS_OF_AS));
}
```

When the channel finishes the parsing, it will then append all the tokens to the Lexer instance
associated with the channel.

**Note that this is REALLY the text matched by the previous rule!** That is, this for instance:

```java
public Rule probablyNotWhatYouMeant()
{
    return sequence("foo", "bar", pushToken(MyTokens.FOOBAR)); // Oops...
}
```

will associate the token in the lexer with "bar", not "foobar"! You'd need to write this instead:

```java
public Rule thatIsBetter()
{
    return sequence(
        sequence("foo", "bar"),
        pushToken(MyTokens.FOOBAR)
    );
}
```

## Technical notes...

Unfortunately, Sonar uses an antiquated version of Guava (10.0.1!!); grappa, on the other hand,
depends on Guava 18.0.

This means that right now two files from Sonar itself had to be included in the source of this
package so that the code can actually run. This problem is not yet solved.

