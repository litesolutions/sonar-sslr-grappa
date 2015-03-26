## Read me first

This package has the same license as Sonar's SSLR package itself: LGPLv3.

Please see the file LICENSE for more details.

## What this is

This package is a sample implementation of an SSLR `Channel` using a parser written with
[grappa](https://github.com/fge/grappa).

Requires Java 7+.

The current version is **0.1.0**:

* groupId: `es.litesolutions`;
* artifactId: `sonar-sslr-grappa`;
* version: `0.1.0`.

## Rationale

### About SSLR

SSLR means SonarSource Language Recognizer. This is the mechanism used by SonarQube to match and
tokenize languages. It relies on three elements:

* `TokenType`s, which are your individual tokens;
* `Channel`s, to parse the language and produce these tokens;
* `GrammarRuleKey`s to aggregate tokens into logical units and ultimately
  produce an AST (Abstract Syntax Tree).

For instance, provided that you have tokens `NUMBER` and `OPERATOR`, and rule
keys `EXPRESSION` and `EXPRESSIONS`, a (simplified!) sample grammar could read:

```java
build.rule(EXPRESSION).is(NUMBER, OPERATOR, NUMBER);
builder.rule(EXPRESSIONS).is(builder.oneOrMore(EXPRESSION));
```

### The trick...

Now, the above grammar relies only on tokens, regardless of whatever those
tokens are (after all, those `NUMBER`s could very well be Roman numerals for all
the grammar cares). The channels (**note the final 's'**) are there to parse
your input and produce those tokens...

But writing those channels can be challenging.

Here is a quick rundown of how the tokenizing process goes:

* A dispatcher invokes the channels. Those channels run one by one, and the
  first one which succeeds wins.
* A channel can match any amount of text, or none at all, and it may produce any
* amount of tokens, including none.
* This process goes on repeatedly until no text of the input is left to match.

However, there are a few pitfalls:

* you can end up with a lot of channels;
* you must account for the order in which your channels are declared;
* which means the code of your channel itself may contain a lot of logic in
  order to avoid this channel to match at any given point in your parsed input,
  etc.

### What this package does instead

With this package, there is only one channel; and this channel is a parser
written using [grappa](https://github.com/fge/grappa). Therefore, the lexer is
reduced to this:

```java
final MyLanguageParser parser = Parboiled.createParser(MyLanguageParser.class);

final Channel<Lexer> channel = new GrappaChannel(parser.theRule());

final Lexer lexer = Lexer.builder()
    .withFailIfNoChannelToConsumeOneCharacter(true)
    .withChannel(channel)
    .build();
```

This channel provides convenience methods to match tokens by value and a few
other things.

Should you wonder what a grappa parser looks like, [here is a JSON
parser](https://github.com/fge/grappa-examples/blob/master/src/main/java/com/github/fge/grappa/examples/json/JsonParser.java).

## Advantages

### Easier to write complex parsing rules

Here is how, for instance, you would match text between parens, WITH included,
matching parens:

```java

// Even matches no text at all
public Rule embeddedParens()
{
    return join(zeroOrMore(noneOf("()"))
        .using(sequence('(', embeddedParens(), ')')
        .min(0);
}
```

### Easy to debug

Suppose you have a complex set of `GrammarRuleKey`s; you have a root rule to set in order for a
Sonar `Parser` to be legal.

Now, you may want to test only _part_ of your grammar.

With this package, and provided you write your grammar rule implementation accordingly, it's
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

Not only that, but you can also modify the channel easily so as to use the
[grappa debugger](https://github.com/fge/grappa-debugger) to test your parser
itself, therefore, your channel! Using it you know exactly what parser rules
matched, in what order, where they were invoked from etc.

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

This package uses a specialized version of the grappa-tracer-backport.

This is needed because Sonar plugins require an old version of Guava (10.0.1)
whereas grappa depends on Guava 18.0.

Among other things, this means that if you with to include listeners of yours in
the parser (which relies on the `@Subscribe` annotation for the appropriate
methods) you need to include this:

```
import sonarhack.com.google.common.eventbus.Subscribe;
```

instead of:

```
import com.google.common.eventbus.Subscribe;
```

because the latter is not guaranteed to succeed!

