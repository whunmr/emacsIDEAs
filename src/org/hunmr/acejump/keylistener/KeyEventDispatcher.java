package org.hunmr.acejump.keylistener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyEventDispatcher<T> implements KeyListener {
    private T _paramObject;
    private KeyEventListener<T> _keyEventListener;

    public KeyEventDispatcher(T paramObject, KeyEventListener<T> keyEventListener) {
        this._paramObject = paramObject;
        this._keyEventListener = keyEventListener;
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        _keyEventListener.keyTyped(keyEvent, this, _paramObject);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        _keyEventListener.keyPressed(keyEvent, this, _paramObject);
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        _keyEventListener.keyReleased(keyEvent, this, _paramObject);
    }
}