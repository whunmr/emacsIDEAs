package org.hunmr.common.predictor;

import org.hunmr.util.Chars;

public class SymbolPredictor extends Predictor<Character> {

    @Override
    public boolean is(Character character) {
        return Chars.isSymbol(character);
    }
}
