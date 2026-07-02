package org.m9mx.cactus.glowberry.util.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * A self-contained recursive-descent math expression evaluator.
 *
 * Supported syntax:
 *   - Binary operators: + - * / % ^ (standard precedence; ^ is right-associative)
 *   - Text operators:   pow / power (same as ^), root / sqrt (unary prefix, square root)
 *   - Parentheses:      ( )
 *   - Numbers:          integers and decimals, with unary +/-
 *   - Constants:        stack (64), shulker (1728), double (3456), pi, e
 *   - Variable:         ans (result of the last successful calculation)
 *
 * The evaluator is stateless aside from the {@code ans} value passed into {@link #evaluate(String, double)}.
 */
public final class ExpressionEvaluator {

    public static final double STACK = 64.0;
    public static final double SHULKER = 1728.0;
    public static final double DOUBLE_CHEST = 3456.0;

    private ExpressionEvaluator() {
    }

    /** Thrown when an expression cannot be parsed or evaluated. */
    public static class EvaluationException extends RuntimeException {
        public EvaluationException(String message) {
            super(message);
        }
    }

    // ----- Tokenizer -----

    private enum TokenType {
        NUMBER, IDENTIFIER, PLUS, MINUS, STAR, SLASH, PERCENT, CARET, LPAREN, RPAREN, EOF
    }

    private static final class Token {
        final TokenType type;
        final double number;
        final String text;

        Token(TokenType type, double number, String text) {
            this.type = type;
            this.number = number;
            this.text = text;
        }
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = input.length();
        while (i < n) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            switch (c) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, 0, "+"));
                    i++;
                    continue;
                case '-':
                    tokens.add(new Token(TokenType.MINUS, 0, "-"));
                    i++;
                    continue;
                case '*':
                    tokens.add(new Token(TokenType.STAR, 0, "*"));
                    i++;
                    continue;
                case '/':
                    tokens.add(new Token(TokenType.SLASH, 0, "/"));
                    i++;
                    continue;
                case '%':
                    tokens.add(new Token(TokenType.PERCENT, 0, "%"));
                    i++;
                    continue;
                case '^':
                    tokens.add(new Token(TokenType.CARET, 0, "^"));
                    i++;
                    continue;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, 0, "("));
                    i++;
                    continue;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, 0, ")"));
                    i++;
                    continue;
                default:
                    break;
            }

            if (Character.isDigit(c) || c == '.') {
                int start = i;
                boolean seenDot = false;
                while (i < n) {
                    char d = input.charAt(i);
                    if (Character.isDigit(d)) {
                        i++;
                    } else if (d == '.' && !seenDot) {
                        seenDot = true;
                        i++;
                    } else {
                        break;
                    }
                }
                String numStr = input.substring(start, i);
                try {
                    tokens.add(new Token(TokenType.NUMBER, Double.parseDouble(numStr), numStr));
                } catch (NumberFormatException e) {
                    throw new EvaluationException("Invalid number: '" + numStr + "'");
                }
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    i++;
                }
                tokens.add(new Token(TokenType.IDENTIFIER, 0, input.substring(start, i)));
                continue;
            }

            throw new EvaluationException("Unexpected character: '" + c + "'");
        }
        tokens.add(new Token(TokenType.EOF, 0, ""));
        return tokens;
    }

    // ----- Parser (recursive descent) -----

    private static final class Parser {
        private final List<Token> tokens;
        private final double ans;
        private int pos;

        Parser(List<Token> tokens, double ans) {
            this.tokens = tokens;
            this.ans = ans;
        }

        private Token peek() {
            return tokens.get(pos);
        }

        private Token next() {
            return tokens.get(pos++);
        }

        private boolean match(TokenType type) {
            if (peek().type == type) {
                pos++;
                return true;
            }
            return false;
        }

        double parse() {
            double value = parseExpression();
            if (peek().type != TokenType.EOF) {
                throw new EvaluationException("Unexpected token: '" + peek().text + "'");
            }
            return value;
        }

        // expression := term (('+' | '-') term)*
        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                if (match(TokenType.PLUS)) {
                    value += parseTerm();
                } else if (match(TokenType.MINUS)) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        // term := power (('*' | '/' | '%') power)*
        private double parseTerm() {
            double value = parsePower();
            while (true) {
                if (match(TokenType.STAR)) {
                    value *= parsePower();
                } else if (match(TokenType.SLASH)) {
                    double divisor = parsePower();
                    if (divisor == 0.0) {
                        throw new EvaluationException("Division by zero");
                    }
                    value /= divisor;
                } else if (match(TokenType.PERCENT)) {
                    double divisor = parsePower();
                    if (divisor == 0.0) {
                        throw new EvaluationException("Modulo by zero");
                    }
                    value %= divisor;
                } else {
                    return value;
                }
            }
        }

        // power := unary (('^' | 'pow' | 'power') power)?   (right-associative)
        private double parsePower() {
            double base = parseUnary();
            if (match(TokenType.CARET)) {
                return Math.pow(base, parsePower());
            }
            if (peek().type == TokenType.IDENTIFIER
                    && (peek().text.equalsIgnoreCase("pow") || peek().text.equalsIgnoreCase("power"))) {
                next();
                return Math.pow(base, parsePower());
            }
            return base;
        }

        // unary := ('-' | '+') unary | ('root' | 'sqrt') unary | primary
        private double parseUnary() {
            if (match(TokenType.MINUS)) {
                return -parseUnary();
            }
            if (match(TokenType.PLUS)) {
                return parseUnary();
            }
            if (peek().type == TokenType.IDENTIFIER
                    && (peek().text.equalsIgnoreCase("root") || peek().text.equalsIgnoreCase("sqrt"))) {
                next();
                double operand = parseUnary();
                if (operand < 0.0) {
                    throw new EvaluationException("Cannot take the square root of a negative number");
                }
                return Math.sqrt(operand);
            }
            return parsePrimary();
        }

        // primary := NUMBER | IDENTIFIER(constant/ans) | '(' expression ')'
        private double parsePrimary() {
            Token token = peek();
            switch (token.type) {
                case NUMBER:
                    next();
                    return token.number;
                case LPAREN: {
                    next();
                    double value = parseExpression();
                    if (!match(TokenType.RPAREN)) {
                        throw new EvaluationException("Missing closing parenthesis");
                    }
                    return value;
                }
                case IDENTIFIER:
                    next();
                    return resolveIdentifier(token.text);
                default:
                    throw new EvaluationException("Expected a value but found '"
                            + (token.text.isEmpty() ? "end of expression" : token.text) + "'");
            }
        }

        private double resolveIdentifier(String name) {
            return resolveVariable(name, ans);
        }
    }

    /** Whether the given identifier (case-insensitive) is a recognized variable/constant. */
    private static boolean isVariable(String name) {
        switch (name.toLowerCase()) {
            case "stack":
            case "shulker":
            case "double":
            case "ans":
            case "pi":
            case "e":
                return true;
            default:
                return false;
        }
    }

    /** Resolves a variable/constant name to its value, using {@code ans} for the memory variable. */
    private static double resolveVariable(String name, double ans) {
        switch (name.toLowerCase()) {
            case "stack":
                return STACK;
            case "shulker":
                return SHULKER;
            case "double":
                return DOUBLE_CHEST;
            case "ans":
                return ans;
            case "pi":
                return Math.PI;
            case "e":
                return Math.E;
            default:
                throw new EvaluationException("Unknown variable: '" + name + "'");
        }
    }

    /**
     * Evaluates the given expression string.
     *
     * @param expression the math expression
     * @param ans        the value to substitute for the {@code ans} variable
     * @return the numeric result
     * @throws EvaluationException if the expression is invalid
     */
    public static double evaluate(String expression, double ans) {
        return evaluateDetailed(expression, ans).value;
    }

    /** The result of a detailed evaluation: the numeric value plus a color-coded display string. */
    public static final class EvalResult {
        /** The computed numeric result. */
        public final double value;
        /**
         * A color-coded rendering of the input with variables substituted inline,
         * e.g. {@code §bans§7(128) §7+ §bstack§7(64)}. Does not include the answer.
         */
        public final String display;

        EvalResult(double value, String display) {
            this.value = value;
            this.display = display;
        }
    }

    /**
     * Evaluates the expression and also builds a color-coded display string that shows the
     * substituted values of any variables (e.g. {@code ans}, {@code stack}) inline.
     *
     * @param expression the math expression
     * @param ans        the value to substitute for the {@code ans} variable
     * @return an {@link EvalResult} with the value and display string
     * @throws EvaluationException if the expression is invalid
     */
    public static EvalResult evaluateDetailed(String expression, double ans) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new EvaluationException("Empty expression");
        }
        List<Token> tokens = tokenize(expression);
        double value = new Parser(tokens, ans).parse();
        return new EvalResult(value, buildDisplay(tokens, ans));
    }

    /**
     * Builds a color-coded string from the token list: gray (§7) for operators and brackets,
     * white (§f) for numbers, and variables shown as {@code §b<name>§7(<value>)}.
     */
    private static String buildDisplay(List<Token> tokens, double ans) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            if (token.type == TokenType.EOF) {
                break;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            switch (token.type) {
                case NUMBER:
                    sb.append("§f").append(token.text);
                    break;
                case PLUS:
                case MINUS:
                case STAR:
                case SLASH:
                case PERCENT:
                case CARET:
                case LPAREN:
                case RPAREN:
                    sb.append("§7").append(token.text);
                    break;
                case IDENTIFIER:
                    if (isVariable(token.text)) {
                        double v = resolveVariable(token.text, ans);
                        sb.append("§b").append(token.text)
                                .append("§7(§f").append(CalcUtil.formatNumber(v)).append("§7)");
                    } else {
                        // Function keyword such as pow / power / root / sqrt.
                        sb.append("§d").append(token.text);
                    }
                    break;
                default:
                    sb.append("§f").append(token.text);
                    break;
            }
        }
        return sb.toString();
    }
}
