/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.listener;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

/**
 * A DocumentListener for views that work with state based view models.
 * This is preferred over KeyListener for text components as it avoids
 * interference with the natural text editing process.
 */
public class StatefulTextDocumentListener implements DocumentListener {

    /**
     * Consumer to feed text changes to
     */
    @Nullable
    private Consumer<String> textConsumer;

    /**
     * The text component this listener is attached to
     */
    @Nonnull
    private final JTextComponent textComponent;

    /**
     * Consumer to call when text change starts (optional)
     */
    @Nullable
    private final Runnable onChangeStart;

    /**
     * Consumer to call when text change ends (optional)
     */
    @Nullable
    private final Runnable onChangeEnd;

    /**
     * Flag to prevent recursive updates
     */
    private boolean isUpdating = false;

    /**
     * @param textComponent The text component to monitor
     * @param textConsumer Consumer to receive text changes
     */
    public StatefulTextDocumentListener(@Nonnull JTextComponent textComponent,
        @Nonnull Consumer<String> textConsumer) {
        this.textComponent = textComponent;
        this.textConsumer = textConsumer;
        this.onChangeStart = null;
        this.onChangeEnd = null;
    }

    /**
     * @param textComponent The text component to monitor
     * @param textConsumer Consumer to receive text changes
     * @param onChangeStart Callback for when change starts
     * @param onChangeEnd Callback for when change ends
     */
    public StatefulTextDocumentListener(@Nonnull JTextComponent textComponent,
        @Nonnull Consumer<String> textConsumer,
        @Nullable Runnable onChangeStart,
        @Nullable Runnable onChangeEnd) {
        this.textComponent = textComponent;
        this.textConsumer = textConsumer;
        this.onChangeStart = onChangeStart;
        this.onChangeEnd = onChangeEnd;
    }

    /**
     * Set the consumer for text changes
     * @param textConsumer Consumer to receive text changes
     */
    public void setTextConsumer(@Nullable Consumer<String> textConsumer) {
        this.textConsumer = textConsumer;
    }

    /**
     * Safely update the text component without triggering this listener
     * @param text The new text to set
     */
    public void updateText(@Nullable String text) {
        if (isUpdating) {
            return;
        }

        isUpdating = true;
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    textComponent.setText(text);
                } finally {
                    isUpdating = false;
                }
            });
        } catch (Exception e) {
            isUpdating = false;
            throw e;
        }
    }

    private void handleTextChange() {
        if (isUpdating || textConsumer == null) {
            return;
        }

        if (onChangeStart != null) {
            onChangeStart.run();
        }

        String currentText = textComponent.getText();
        textConsumer.accept(currentText);

        if (onChangeEnd != null) {
            SwingUtilities.invokeLater(() -> {
                if (onChangeEnd != null) {
                    onChangeEnd.run();
                }
            });
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        handleTextChange();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        handleTextChange();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // This is typically only called for styled documents
        handleTextChange();
    }
}