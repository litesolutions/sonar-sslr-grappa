## Read me first

This package has the same license as Sonar's SSLR package itself: LGPLv3.

Please see the file LICENSE for more details.

## What this is

This package is a simple framework for writing a language plugin for SonarQube.
At its core, it uses parsers written with [grappa](https://github.com/fge/grappa).

Requires Java 7+.

The current version is **0.2.0**:

* groupId: `es.litesolutions`;
* artifactId: `sonar-sslr-grappa`;
* version: `0.2.0`.

## About SSLR

### Principle

SSLR means SonarSource Language Recognizer. This is the framework used by
SonarQube to match and tokenize languages and build an AST (Abstract Syntax
Tree).

An AST in Sonar is a sequence of `AstNode` instances; those same `AstNode`s are
what your language checks will subscribe to and perform checks upon the `Token`
associated with this node (or its parent(s), sibling(s), child(ren) etc).

The Sonar API provides two mechanisms with which you can produce an AST.

### Using a `LexerlessGrammarBuilder`

Using this method, an SSLR grammar takes upon itself to guide both the token/AST
node production _and_ parsing of the input text. Using this method, terminals
(that is, grammar rules which do not depend on any other rules) are sequences of
text in the input.

While very flexible, writing such a grammar is very involved; more often than
not, for languages even moderately complex, this means implementing quite a few
helper classes to properly guide the parsing process.

Two examples of Sonar language plugins written using this technique are the Java
and JavaScript language plugins.

### Using a `LexerfulGrammarBuilder`

Using this method,  your grammar rules are purely declarative in that their
terminals are `AstNode` instances (more often than not instances of
`TokenType`), not input text. And matching the input text to produce tokens is
delegated to another mechanism, and this other mechanism is channels.

While this mechanism looks easier to work with, channels have their own
challenges as well. Here is how channel dispatching works:

* A dispatcher invokes the channels. Those channels run one by one, and the
  first one which succeeds wins.
* A channel can match any amount of text, or none at all, and it may produce any
  amount of tokens, including none.
* This process goes on repeatedly until no text of the input is left to match.

The consequences are as follows:

* you can end up with a lot of channels;
* you must account for the order in which your channels are declared;
* which means that the code of your channel itself may contain a lot of logic in
  order to avoid this channel to match at any given point in your parsed input,
  etc.

One example of a Sonar language plugin using this technique is the Python
language plugin.

## What this package does instead

### The grammar is fully declarative...

That is, you use a `LexerfulGrammarBuilder` and your terminals are (usually)
`TokenType`s...

### But you only have one channel

... And this channel is a [grappa](https://github.com/fge/grappa) parser.

Should you wonder what a grappa parser looks like, [here is a JSON
parser](https://github.com/fge/grappa-examples/blob/master/src/main/java/com/github/fge/grappa/examples/json/JsonParser.java).

## Advantages

### Separation of concerns

You write your grammar so that you are only concerned about how your AST should
look like; you need not be concerned about what text is matched by those tokens.

If anything, the elements of your language plugins which could care about the
actual content of the tokens are your checks -- not the grammar.

### Ease of debugging

Grappa provides the following tools to help you:

* a tracer, with which you can record your parsing run;
* a [debugger](https://github.com/fge/grappa-debugger), with which you can
  analyze your parsing run.

The API allows you to register a tracer which will generate trace files (those
are zip files) in the directory of your choice.

### Versatility

Grappa is a full fledged PEG parser; and it has mechanisms to help you get
things done which go beyond PEG as well. For instance, the famous "a^n b^n c^n"
grammar can be matched with this single rule:

```
// Supposes Java 7+
public Rule anbncn()
{
    final Var<Integer> count = new Var<>();
    return sequence(
        oneOrMore('a'), count.set(match().length()),
        oneOrMore('b'), match().length() == count.get(),
        oneOrMore('c'), match().length() == count.get()
    );
}
```

## How this works

### The parser

In order to write a parser, you will need to extend `SonarParserBase`; this
(abstract) parser implementation extends `ListeningParser<Token.Builder>`
(which, by the way, means you can register any listener to your parser
implementation provided it has the necessary annotations; see
[`EventBus`](http://docs.guava-libraries.googlecode.com/git-history/release/javadoc/com/google/common/eventbus/EventBus.html)).

What you push is not a token directly but an instance of `Token.Builder`; all you have to do is use
the method `pushToken(someTokenType)`, with `someTokenType` being an instance of
`TokenType`.

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

### The (SSLR) parser factory

This package also offers a factory which allows you to generate SSLR parsers out
of two elements:

* your (grappa!) parser class,
* your grammar class.

From such a factory, you can obtain not only rules for parsing a full source
file, but also just extracts from it; this makes it very convenient for testing
only part of your parser and/or grammar.

## Technical notes...

This package uses a specialized version of the grappa-tracer-backport.

This is needed because Sonar plugins require an old version of Guava (10.0.1)
whereas grappa depends on Guava 18.0.

Among other things, this means that if you with to include listeners of yours in
the parser (which relies on the `@Subscribe` annotation for the appropriate
methods) you need to include this:

```
// Note the initial "r"!
import r.com.google.common.eventbus.Subscribe;
```

instead of:

```
import com.google.common.eventbus.Subscribe;
```

because the latter is not guaranteed to succeed!

