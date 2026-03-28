package org.hunmr.util;

public class Chars {

    public static boolean isSymbol(Character character) {
        return character != null && Chars.isSymbol(character.charValue());
    }

    public static boolean isSymbol(char c) {
        return c == '_' || Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c);
    }
}
