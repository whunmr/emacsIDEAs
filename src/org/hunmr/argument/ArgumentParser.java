package org.hunmr.argument;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArgumentParser {
    private static final Set<String> NON_CALL_KEYWORDS = new HashSet<String>(Arrays.asList(
            "if", "for", "while", "switch", "catch", "return", "sizeof", "case", "else", "do"
    ));

    private ArgumentParser() {
    }

    public static ParsedArguments parse(String text) {
        String safeText = text == null ? "" : text;
        ArrayList<ArgumentCandidate> arguments = new ArrayList<ArgumentCandidate>();
        ArrayList<ArgumentList> lists = new ArrayList<ArgumentList>();
        Deque<Frame> stack = new ArrayDeque<Frame>();
        Token lastToken = Token.none();

        for (int i = 0; i < safeText.length(); ) {
            char c = safeText.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (startsLineComment(safeText, i)) {
                i = skipLineComment(safeText, i + 2);
                continue;
            }

            if (startsBlockComment(safeText, i)) {
                i = skipBlockComment(safeText, i + 2);
                continue;
            }

            if (c == '"' || c == '\'' || c == '`') {
                i = skipQuoted(safeText, i);
                lastToken = Token.punctuation(c);
                continue;
            }

            if (isIdentifierStart(c)) {
                int start = i;
                i++;
                while (i < safeText.length() && isIdentifierPart(safeText.charAt(i))) {
                    i++;
                }
                lastToken = Token.identifier(safeText.substring(start, i));
                continue;
            }

            if (Character.isDigit(c)) {
                i = skipNumber(safeText, i + 1);
                lastToken = Token.punctuation('#');
                continue;
            }

            switch (c) {
                case '(':
                    stack.push(new Frame('(', i, looksLikeArgumentList(lastToken)));
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case '[':
                case '{':
                    stack.push(new Frame(c, i, false));
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case '<':
                    if (looksLikeAngleBlock(safeText, i, lastToken)) {
                        stack.push(new Frame(c, i, false));
                    }
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case ',':
                    if (!stack.isEmpty() && stack.peek().isArgumentList()) {
                        stack.peek().finishArgument(i, safeText);
                    }
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case ')':
                    closeFrame(stack, '(', i, safeText, arguments, lists);
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case ']':
                    closeFrame(stack, '[', i, safeText, arguments, lists);
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case '}':
                    closeFrame(stack, '{', i, safeText, arguments, lists);
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                case '>':
                    closeFrame(stack, '<', i, safeText, arguments, lists);
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
                default:
                    lastToken = Token.punctuation(c);
                    i++;
                    break;
            }
        }

        return new ParsedArguments(safeText, arguments, lists);
    }

    private static void closeFrame(Deque<Frame> stack,
                                   char expectedOpenChar,
                                   int closeOffset,
                                   String text,
                                   List<ArgumentCandidate> arguments,
                                   List<ArgumentList> lists) {
        if (expectedOpenChar == '<') {
            if (stack.isEmpty() || stack.peek()._openChar != '<') {
                return;
            }

            stack.pop();
            return;
        }

        while (!stack.isEmpty() && stack.peek()._openChar == '<') {
            stack.pop();
        }

        if (stack.isEmpty() || stack.peek()._openChar != expectedOpenChar) {
            return;
        }

        Frame frame = stack.pop();
        if (!frame.isArgumentList()) {
            return;
        }

        frame.finishArgument(closeOffset, text);
        ArgumentList list = frame.toArgumentList(text, closeOffset);
        if (list != null) {
            lists.add(list);
            arguments.addAll(list.getArguments());
        }
    }

    private static boolean looksLikeArgumentList(Token lastToken) {
        if (lastToken == null) {
            return false;
        }

        if (lastToken._type == TokenType.IDENTIFIER) {
            return !NON_CALL_KEYWORDS.contains(lastToken._text);
        }

        if (lastToken._type == TokenType.PUNCTUATION) {
            char c = lastToken._symbol;
            return c == ')' || c == ']' || c == '>' || c == '"' || c == '\'' || c == '`';
        }

        return false;
    }

    private static boolean looksLikeAngleBlock(String text, int offset, Token lastToken) {
        if (lastToken == null || text == null || offset < 0 || offset >= text.length()) {
            return false;
        }

        if (offset == 0 || Character.isWhitespace(text.charAt(offset - 1))) {
            return false;
        }

        if (offset + 1 >= text.length() || Character.isWhitespace(text.charAt(offset + 1))) {
            return false;
        }

        if (!lastToken.canOpenAngle()) {
            return false;
        }

        int nextMeaningful = findNextMeaningfulIndex(text, offset + 1);
        if (nextMeaningful < 0 || !canStartAngleContent(text.charAt(nextMeaningful))) {
            return false;
        }

        int closingOffset = findMatchingAngle(text, offset);
        if (closingOffset < 0) {
            return false;
        }

        int nextAfterClose = findNextMeaningfulIndex(text, closingOffset + 1);
        if (nextAfterClose < 0) {
            return true;
        }

        return canFollowAngle(text.charAt(nextAfterClose));
    }

    private static boolean canStartAngleContent(char c) {
        return isIdentifierStart(c) || c == '?' || c == '[';
    }

    private static boolean canNestAngle(String text, int offset, Token lastToken) {
        return lastToken != null
                && lastToken.canOpenAngle()
                && offset > 0
                && offset + 1 < text.length()
                && !Character.isWhitespace(text.charAt(offset - 1))
                && !Character.isWhitespace(text.charAt(offset + 1))
                && canStartAngleContent(text.charAt(offset + 1));
    }

    private static boolean canFollowAngle(char c) {
        return isIdentifierStart(c) || Character.isDigit(c) || "().,;:[]{}".indexOf(c) >= 0;
    }

    private static int findMatchingAngle(String text, int offset) {
        int depth = 1;
        Token lastToken = Token.none();

        for (int i = offset + 1; i < text.length(); ) {
            char c = text.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i + 2);
                continue;
            }

            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i + 2);
                continue;
            }

            if (c == '"' || c == '\'' || c == '`') {
                i = skipQuoted(text, i);
                lastToken = Token.punctuation(c);
                continue;
            }

            if (isIdentifierStart(c)) {
                int start = i;
                i++;
                while (i < text.length() && isIdentifierPart(text.charAt(i))) {
                    i++;
                }
                lastToken = Token.identifier(text.substring(start, i));
                continue;
            }

            if (c == '<' && canNestAngle(text, i, lastToken)) {
                depth++;
                lastToken = Token.punctuation(c);
                i++;
                continue;
            }

            if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i;
                }
                lastToken = Token.punctuation(c);
                i++;
                continue;
            }

            lastToken = Token.punctuation(c);
            i++;
        }

        return -1;
    }

    private static int findNextMeaningfulIndex(String text, int offset) {
        for (int i = Math.max(0, offset); i < text.length(); ) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (startsLineComment(text, i)) {
                i = skipLineComment(text, i + 2);
                continue;
            }

            if (startsBlockComment(text, i)) {
                i = skipBlockComment(text, i + 2);
                continue;
            }

            return i;
        }

        return -1;
    }

    private static boolean startsLineComment(String text, int offset) {
        return offset + 1 < text.length() && text.charAt(offset) == '/' && text.charAt(offset + 1) == '/';
    }

    private static boolean startsBlockComment(String text, int offset) {
        return offset + 1 < text.length() && text.charAt(offset) == '/' && text.charAt(offset + 1) == '*';
    }

    private static int skipLineComment(String text, int offset) {
        int index = offset;
        while (index < text.length() && text.charAt(index) != '\n') {
            index++;
        }
        return index;
    }

    private static int skipBlockComment(String text, int offset) {
        int index = offset;
        while (index + 1 < text.length()) {
            if (text.charAt(index) == '*' && text.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return text.length();
    }

    private static int skipQuoted(String text, int offset) {
        char quote = text.charAt(offset);
        int index = offset + 1;

        while (index < text.length()) {
            char c = text.charAt(index);
            if (quote != '`' && c == '\\') {
                index += 2;
                continue;
            }

            if (c == quote) {
                return index + 1;
            }

            index++;
        }

        return text.length();
    }

    private static int skipNumber(String text, int offset) {
        int index = offset;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                break;
            }
            index++;
        }
        return index;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private enum TokenType {
        NONE,
        IDENTIFIER,
        PUNCTUATION
    }

    private static final class Token {
        private final TokenType _type;
        private final String _text;
        private final char _symbol;

        private Token(TokenType type, String text, char symbol) {
            _type = type;
            _text = text;
            _symbol = symbol;
        }

        public static Token none() {
            return new Token(TokenType.NONE, "", '\0');
        }

        public static Token identifier(String text) {
            return new Token(TokenType.IDENTIFIER, text, '\0');
        }

        public static Token punctuation(char c) {
            return new Token(TokenType.PUNCTUATION, "", c);
        }

        public boolean canOpenAngle() {
            if (_type == TokenType.IDENTIFIER) {
                return true;
            }

            if (_type == TokenType.PUNCTUATION) {
                return _symbol == '>' || _symbol == ')' || _symbol == ']';
            }

            return false;
        }
    }

    private static final class Frame {
        private final char _openChar;
        private final int _openOffset;
        private final boolean _argumentList;
        private final ArrayList<TextSpan> _segments = new ArrayList<TextSpan>();
        private int _currentSegmentStart;

        private Frame(char openChar, int openOffset, boolean argumentList) {
            _openChar = openChar;
            _openOffset = openOffset;
            _argumentList = argumentList;
            _currentSegmentStart = openOffset + 1;
        }

        public boolean isArgumentList() {
            return _argumentList && _openChar == '(';
        }

        public void finishArgument(int endOffsetExclusive, String text) {
            if (!isArgumentList()) {
                return;
            }

            int trimmedStart = trimLeadingWhitespace(text, _currentSegmentStart, endOffsetExclusive);
            int trimmedEnd = trimTrailingWhitespace(text, trimmedStart, endOffsetExclusive);
            if (trimmedStart < trimmedEnd) {
                _segments.add(new TextSpan(trimmedStart, trimmedEnd));
            }
            _currentSegmentStart = endOffsetExclusive + 1;
        }

        public ArgumentList toArgumentList(String text, int closeOffset) {
            ArrayList<ArgumentCandidate> candidates = new ArrayList<ArgumentCandidate>();
            for (int i = 0; i < _segments.size(); i++) {
                TextSpan span = _segments.get(i);
                int anchorOffset = findAnchor(text, span.getStartOffset(), span.getEndOffset());
                candidates.add(new ArgumentCandidate(span, anchorOffset, _openOffset, closeOffset, i));
            }
            return new ArgumentList(_openOffset, closeOffset, candidates);
        }

        private static int trimLeadingWhitespace(String text, int start, int end) {
            int index = Math.max(0, start);
            while (index < end && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            return index;
        }

        private static int trimTrailingWhitespace(String text, int start, int end) {
            int index = Math.max(start, end);
            while (index > start && Character.isWhitespace(text.charAt(index - 1))) {
                index--;
            }
            return index;
        }

        private static int findAnchor(String text, int start, int end) {
            int index = start;
            while (index < end) {
                while (index < end && Character.isWhitespace(text.charAt(index))) {
                    index++;
                }

                if (index + 1 < end && text.charAt(index) == '/' && text.charAt(index + 1) == '/') {
                    index = skipLineComment(text, index + 2);
                    continue;
                }

                if (index + 1 < end && text.charAt(index) == '/' && text.charAt(index + 1) == '*') {
                    index = skipBlockComment(text, index + 2);
                    continue;
                }

                break;
            }

            return index < end ? index : start;
        }
    }
}
