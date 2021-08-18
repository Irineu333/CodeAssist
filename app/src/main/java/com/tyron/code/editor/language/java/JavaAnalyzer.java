package com.tyron.code.editor.language.java;
import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.text.TextAnalyzeResult;
import io.github.rosemoe.editor.text.TextAnalyzer.AnalyzeThread.Delegate;
import io.github.rosemoe.editor.text.TextAnalyzer;
import io.github.rosemoe.editor.langs.java.JavaCodeAnalyzer;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import io.github.rosemoe.editor.struct.Span;
import io.github.rosemoe.editor.widget.EditorColorScheme;
import io.github.rosemoe.editor.langs.java.Tokens;
import io.github.rosemoe.editor.text.LineNumberCalculator;
import io.github.rosemoe.editor.langs.java.JavaTextTokenizer;
import io.github.rosemoe.editor.struct.BlockLine;
import io.github.rosemoe.editor.struct.NavigationItem;
import java.util.Stack;
import io.github.rosemoe.editor.langs.internal.TrieTree;

public class JavaAnalyzer extends JavaCodeAnalyzer {
    
    private static JavaAnalyzer INSTANCE = null;
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

    public static JavaAnalyzer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JavaAnalyzer();
        }
        return INSTANCE;
    }
    
    @Override
    public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
        StringBuilder text = content instanceof StringBuilder ? (StringBuilder) content : new StringBuilder(content);
        JavaTextTokenizer tokenizer = new JavaTextTokenizer(text);
        tokenizer.setCalculateLineColumn(false);
        Tokens token, previous = Tokens.UNKNOWN;
        int line = 0, column = 0;
        LineNumberCalculator helper = new LineNumberCalculator(text);
        
        Stack<BlockLine> stack = new Stack<>();
        List<NavigationItem> labels = new ArrayList<>();
        int maxSwitch = 1, currSwitch = 0;
        
        boolean first = true;
        while (delegate.shouldAnalyze()) {
            try {
                // directNextToken() does not skip any token
                token = tokenizer.directNextToken();
            } catch (RuntimeException e) {
                //When a spelling input is in process, this will happen because of format mismatch
                token = Tokens.CHARACTER_LITERAL;
            }
            if (token == Tokens.EOF) {
                break;
            }
            // Backup values because looking ahead in function name match will change them
            int thisIndex = tokenizer.getIndex();
            int thisLength = tokenizer.getTokenLength();
            switch (token) {
                case WHITESPACE:
                case NEWLINE:
                    if (first) {
                        colors.addNormalIfNull();
                    }
                    break;
                case IDENTIFIER:
                    //Add a identifier to auto complete
                    
                    //The previous so this will be the annotation's type name
                    if (previous == Tokens.AT) {
                        colors.addIfNeeded(line, column, EditorColorScheme.ANNOTATION);
                        break;
                    }
                    //Here we have to get next token to see if it is function
                    //We can only get the next token in stream.
                    //If more tokens required, we have to use a stack in tokenizer
                    Tokens next = tokenizer.directNextToken();
                    //The next is LPAREN,so this is function name or type name
                    if (next == Tokens.LPAREN) {
                        colors.addIfNeeded(line, column, EditorColorScheme.FUNCTION_NAME);
                        tokenizer.pushBack(tokenizer.getTokenLength());
                        break;
                    }
                    //Push back the next token
                    tokenizer.pushBack(tokenizer.getTokenLength());
                    //This is a class definition
                  
                    colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
                    break;
                case CHARACTER_LITERAL:
                case STRING:
                case FLOATING_POINT_LITERAL:
                case INTEGER_LITERAL:                    
                    colors.addIfNeeded(line, column, EditorColorScheme.LITERAL);
                    break;
                case INT:
                case LONG:
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case FLOAT:
                case DOUBLE:
                case SHORT:
                case VOID:
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                case ABSTRACT:
                case ASSERT:
                case CLASS:
                case DO:
                case FINAL:
                case FOR:
                case IF:
                case NEW:
                case PUBLIC:
                case PRIVATE:
                case PROTECTED:
                case PACKAGE:
                case RETURN:
                case STATIC:
                case SUPER:
                case SWITCH:
                case ELSE:
                case VOLATILE:
                case SYNCHRONIZED:
                case STRICTFP:
                case GOTO:
                case CONTINUE:
                case BREAK:
                case TRANSIENT:
                case TRY:
                case CATCH:
                case FINALLY:
                case WHILE:
                case CASE:
                case DEFAULT:
                case CONST:
                case ENUM:
                case EXTENDS:
                case IMPLEMENTS:
                case IMPORT:
                case INSTANCEOF:
                case INTERFACE:
                case NATIVE:
                case THIS:
                case THROW:
                case THROWS:
                case TRUE:
                case FALSE:
                case NULL:
                    colors.addIfNeeded(line, column, EditorColorScheme.KEYWORD);
                    break;
                case LBRACE: {
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        if (stack.isEmpty()) {
                            if (currSwitch > maxSwitch) {
                                maxSwitch = currSwitch;
                            }
                            currSwitch = 0;
                        }
                        currSwitch++;
                        BlockLine block = colors.obtainNewBlock();
                        block.startLine = line;
                        block.startColumn = column;
                        stack.push(block);
                        break;
                    }
                case RBRACE: {
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        if (!stack.isEmpty()) {
                            BlockLine block = stack.pop();
                            block.endLine = line;
                            block.endColumn = column;
                            if (block.startLine != block.endLine) {
                                colors.addBlockLine(block);
                            }
                        }
                        break;
                    }
                case LINE_COMMENT:
                case LONG_COMMENT:
                    colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
                    break;
                default:
                    if (token == Tokens.LBRACK || (token == Tokens.RBRACK && previous == Tokens.LBRACK)) {
                        colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
                        break;
                    }
                    colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
            }
            first = false;
            helper.update(thisLength);
            line = helper.getLine();
            column = helper.getColumn();
            if (token != Tokens.WHITESPACE && token != Tokens.NEWLINE) {
                previous = token;
            }
        }
        if (stack.isEmpty()) {
            if (currSwitch > maxSwitch) {
                maxSwitch = currSwitch;
            }
        }
        colors.determine(line);
        colors.setSuppressSwitch(maxSwitch + 10);
        colors.setNavigation(labels);
    }
    
    public void setDiagnostics(List<Diagnostic<? extends JavaFileObject>> diags) {
        diagnostics.clear();
        diagnostics.addAll(diags);
    }
    
}
