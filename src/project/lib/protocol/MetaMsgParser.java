package project.lib.protocol;

import java.util.HashMap;
import java.util.stream.Stream;

import project.lib.protocol.MetaMessage.Body;
import project.lib.protocol.scaffolding.collections.HList;
import project.lib.protocol.scaffolding.parser.Parsed;
import project.lib.protocol.scaffolding.parser.Parser;
import project.lib.protocol.scaffolding.parser.Parsers;

public class MetaMsgParser implements MetaMessageParser {
    private static final Parser<Atom> atom = Parsers.regex("[^;&]+").map(Atom::of);
    private static final Parser<String> id = Parsers.regex("[_a-zA-Z][_a-zA-Z0-9]*");
    private static final Parser<String> at = Parsers.regex("@");
    private static final Parser<String> colon = Parsers.regex(":");
    private static final Parser<String> semicolon = Parsers.regex(";");
    private static final Parser<String> equal = Parsers.regex("=");
    private static final Parser<String> ampasand = Parsers.regex("&");
    private static final Parser<AtomRule> atomRule = id.join(equal).join(atom).map(AtomRule::of)
            .inspect(x -> System.out.println("atomRule:" + x.toString()));;
    private static final Parser<RuleSet> ruleSet = createRuleSet();
    private static final Parser<Mapping> mapping = ruleSet.map(Mapping::of);
    private static final Parser<RecRule> recRule = id.join(colon).join(ruleSet)
            .join(semicolon).map(RecRule::of);
    private static final Parser<Body> body = mapping.map(x -> (Body) x).or(atom.map(x -> (Body) x));
    private static final Parser<MetaMessage> metaMessage = id.join(at).join(body).map(MetaMsgParser::createMetaMessage);

    private static Parser<RuleSet> createRuleSet() {
        final Parser<RecRule> recRule = MetaMsgParser::recRuleFn;
        final var atomOrRec = atomRule.map(x -> (Rule) x).or(recRule.map(x -> (Rule) x));
        final var separated = atomOrRec.separated(ampasand);
        return separated.map(RuleSet::of);
    }

    private static Parsed<RecRule> recRuleFn(CharSequence input) {
        return recRule.parse(input);
    }

    private static MetaMessage createMetaMessage(HList<HList<String, String>, MetaMessage.Body> list) {
        final var body = list.head;
        final var id = list.rest.rest;

        return new MetaMessage() {
            {
                this._body = body;
                this._id = id;
            }

            private final MetaMessage.Body _body;
            private final String _id;

            @Override
            public MetaMessage.Body body() {
                return this._body;
            }

            @Override
            public String identity() {
                return this._id;
            }

            @Override
            public String toString() {
                return this.identity() + "@" + this.body().toString();
            }
        };
    }

    @Override
    public MetaMessage parse(CharSequence sequence) {
        final var result = metaMessage.parse(sequence);
        if (result == null) {
            return null;
        }
        return result.value;
    }
}

class RuleSet {
    public static RuleSet of(Stream<Rule> rules) {
        return new RuleSet(rules.toArray(Rule[]::new));
    }

    public final Rule[] rules;

    private RuleSet(Rule[] rules) {
        this.rules = rules;
    }
}

abstract sealed class Rule permits RecRule, AtomRule {
    public final String id;

    protected Rule(String id) {
        this.id = id;
    }
}

final class RecRule extends Rule {
    public static RecRule of(HList<HList<HList<String, String>, RuleSet>, String> list) {
        final var ruleSet = list.rest.head;
        final var id = list.rest.rest.rest;
        return new RecRule(id, ruleSet);
    }

    public final RuleSet ruleSet;

    private RecRule(String id, RuleSet ruleSet) {
        super(id);
        this.ruleSet = ruleSet;
    }
}

final class AtomRule extends Rule {
    public static AtomRule of(HList<HList<String, String>, Atom> list) {
        final var atom = list.head;
        final var id = list.rest.rest;
        return new AtomRule(id, atom);
    }

    public final Atom atom;

    private AtomRule(String id, Atom atom) {
        super(id);
        this.atom = atom;
    }
}

class Mapping implements MetaMessage.Body.Mapping {
    public static class Builder {
        private final HashMap<String, Body> map;

        Builder() {
            this.map = new HashMap<>();
        }

        public Builder add(String key, Body value) {
            this.map.put(key, value);
            return this;
        }

        public project.lib.protocol.Mapping build() {
            return new project.lib.protocol.Mapping(map);
        }
    }

    public static project.lib.protocol.Mapping of(RuleSet ruleSet) {
        final var builder = builder();
        for (final var rule : ruleSet.rules) {
            if (rule.getClass() == RecRule.class) {
                final var r = (RecRule) rule;
                builder.add(r.id, project.lib.protocol.Mapping.of(r.ruleSet));
            } else {
                final var r = (AtomRule) rule;
                builder.add(r.id, r.atom);
            }
        }
        return builder.build();
    }

    public static project.lib.protocol.Mapping.Builder builder() {
        return new Builder();
    }

    private final HashMap<String, Body> map;

    private Mapping(HashMap<String, Body> map) {
        this.map = map;
    }

    @Override
    public Body get(String key) {
        return map.get(key);
    }

    @Override
    public String toString() {
        final var builder = new java.lang.StringBuilder();
        for (final var entry : this.map.entrySet()) {
            final var key = entry.getKey();
            final var val = entry.getValue();
            final var vstr = val.toString();

            if (val instanceof MetaMessage.Body.Atom) {
                builder.append(key + "=" + vstr);
            } else {
                builder.append(key + ":" + vstr);
            }
        }
        return builder.toString();
    }
}

class Atom implements MetaMessage.Body.Atom {
    public static project.lib.protocol.Atom of(String text) {
        return new project.lib.protocol.Atom(text);
    }

    final String text;

    private Atom(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}