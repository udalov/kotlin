/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.checkers;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.AbstractDiagnosticWithParametersRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckerTestUtil {
    public static final Comparator<Diagnostic> DIAGNOSTIC_COMPARATOR = new Comparator<Diagnostic>() {
        @Override
        public int compare(@NotNull Diagnostic o1, @NotNull Diagnostic o2) {
            List<TextRange> ranges1 = o1.getTextRanges();
            List<TextRange> ranges2 = o2.getTextRanges();
            int minNumberOfRanges = ranges1.size() < ranges2.size() ? ranges1.size() : ranges2.size();
            for (int i = 0; i < minNumberOfRanges; i++) {
                TextRange range1 = ranges1.get(i);
                TextRange range2 = ranges2.get(i);
                int startOffset1 = range1.getStartOffset();
                int startOffset2 = range2.getStartOffset();
                if (startOffset1 != startOffset2) {
                    // Start early -- go first
                    return startOffset1 - range2.getStartOffset();
                }
                int endOffset1 = range1.getEndOffset();
                int endOffset2 = range2.getEndOffset();
                if (endOffset1 != endOffset2) {
                    // start at the same offset, the one who end later is the outer, i.e. goes first
                    return endOffset2 - endOffset1;
                }
            }
            return ranges1.size() - ranges2.size();
        }
    };

    private static final String IGNORE_DIAGNOSTIC_PARAMETER = "IGNORE";
    private static final String DIAGNOSTIC_PARAMETER = "[^\\)\\(;]+";
    private static final String INDIVIDUAL_DIAGNOSTIC = "(\\w+)(\\(" + DIAGNOSTIC_PARAMETER + "(;\\s*" + DIAGNOSTIC_PARAMETER + ")*\\))?";
    private static final Pattern RANGE_START_OR_END_PATTERN = Pattern.compile("(<!"+
                                                                              INDIVIDUAL_DIAGNOSTIC +"(,\\s*"+
                                                                              INDIVIDUAL_DIAGNOSTIC +")*!>)|(<!>)");
    private static final Pattern INDIVIDUAL_DIAGNOSTIC_PATTERN = Pattern.compile(INDIVIDUAL_DIAGNOSTIC);
    private static final Pattern INDIVIDUAL_PARAMETER_PATTERN = Pattern.compile(DIAGNOSTIC_PARAMETER);

    @NotNull
    public static List<Diagnostic> getDiagnosticsIncludingSyntaxErrors(
            @NotNull BindingContext bindingContext,
            @NotNull final PsiElement root,
            boolean markDynamicCalls
    ) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.addAll(Collections2.filter(bindingContext.getDiagnostics().all(),
                                               new Predicate<Diagnostic>() {
                                                   @Override
                                                   public boolean apply(Diagnostic diagnostic) {
                                                       return  PsiTreeUtil.isAncestor(root, diagnostic.getPsiElement(), false);
                                                   }
                                               }));
        for (PsiErrorElement errorElement : AnalyzingUtils.getSyntaxErrorRanges(root)) {
            diagnostics.add(new SyntaxErrorDiagnostic(errorElement));
        }
        List<Diagnostic> debugAnnotations = getDebugInfoDiagnostics(root, bindingContext, markDynamicCalls);
        diagnostics.addAll(debugAnnotations);
        return diagnostics;
    }

    @NotNull
    public static List<Diagnostic> getDebugInfoDiagnostics(
            @NotNull PsiElement root,
            @NotNull BindingContext bindingContext,
            final boolean markDynamicCalls
    ) {
        final List<Diagnostic> debugAnnotations = Lists.newArrayList();
        DebugInfoUtil.markDebugAnnotations(root, bindingContext, new DebugInfoUtil.DebugInfoReporter() {
            @Override
            public void reportElementWithErrorType(@NotNull JetReferenceExpression expression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE);
            }

            @Override
            public void reportMissingUnresolved(@NotNull JetReferenceExpression expression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.MISSING_UNRESOLVED);
            }

            @Override
            public void reportUnresolvedWithTarget(@NotNull JetReferenceExpression expression, @NotNull String target) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET);
            }

            @Override
            public void reportDynamicCall(@NotNull JetElement element) {
                if (markDynamicCalls) {
                    newDiagnostic(element, DebugInfoDiagnosticFactory.DYNAMIC);
                }
            }

            private void newDiagnostic(JetElement element, DebugInfoDiagnosticFactory factory) {
                debugAnnotations.add(new DebugInfoDiagnostic(element, factory));
            }
        });
        // this code is used in tests and in internal action 'copy current file as diagnostic test'
        //noinspection TestOnlyProblems
        for (JetExpression expression : bindingContext.getSliceContents(BindingContext.SMARTCAST).keySet()) {
            if (PsiTreeUtil.isAncestor(root, expression, false)) {
                debugAnnotations.add(new DebugInfoDiagnostic(expression, DebugInfoDiagnosticFactory.SMARTCAST));
            }
        }
        return debugAnnotations;
    }

    public interface DiagnosticDiffCallbacks {
        void missingDiagnostic(TextDiagnostic diagnostic, int expectedStart, int expectedEnd);
        void wrongParametersDiagnostic(TextDiagnostic expectedDiagnostic, TextDiagnostic actualDiagnostic, int start, int end);
        void unexpectedDiagnostic(TextDiagnostic diagnostic, int actualStart, int actualEnd);
    }

    public static void diagnosticsDiff(
            Map<Diagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic,
            List<DiagnosedRange> expected,
            Collection<Diagnostic> actual,
            DiagnosticDiffCallbacks callbacks
    ) {
        assertSameFile(actual);

        Iterator<DiagnosedRange> expectedDiagnostics = expected.iterator();
        List<DiagnosticDescriptor> sortedDiagnosticDescriptors = getSortedDiagnosticDescriptors(actual);
        Iterator<DiagnosticDescriptor> actualDiagnostics = sortedDiagnosticDescriptors.iterator();

        DiagnosedRange currentExpected = safeAdvance(expectedDiagnostics);
        DiagnosticDescriptor currentActual = safeAdvance(actualDiagnostics);
        while (currentExpected != null || currentActual != null) {
            if (currentExpected != null) {
                if (currentActual == null) {
                    missingDiagnostics(callbacks, currentExpected);
                    currentExpected = safeAdvance(expectedDiagnostics);
                }
                else {
                    int expectedStart = currentExpected.getStart();
                    int actualStart = currentActual.getStart();
                    int expectedEnd = currentExpected.getEnd();
                    int actualEnd = currentActual.getEnd();
                    if (expectedStart < actualStart) {
                        missingDiagnostics(callbacks, currentExpected);
                        currentExpected = safeAdvance(expectedDiagnostics);
                    }
                    else if (expectedStart > actualStart) {
                        unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else if (expectedEnd > actualEnd) {
                        assert expectedStart == actualStart;
                        missingDiagnostics(callbacks, currentExpected);
                        currentExpected = safeAdvance(expectedDiagnostics);
                    }
                    else if (expectedEnd < actualEnd) {
                        assert expectedStart == actualStart;
                        unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else {
                        compareDiagnostics(callbacks, currentExpected, currentActual, diagnosticToExpectedDiagnostic);
                        currentExpected = safeAdvance(expectedDiagnostics);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                }
            }
            else {
                //noinspection ConstantConditions
                assert (currentActual != null);

                unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                currentActual = safeAdvance(actualDiagnostics);
            }
        }
    }

    private static void compareDiagnostics(
            @NotNull DiagnosticDiffCallbacks callbacks,
            @NotNull DiagnosedRange currentExpected,
            @NotNull DiagnosticDescriptor currentActual,
            @NotNull Map<Diagnostic, TextDiagnostic> diagnosticToInput
    ) {
        int expectedStart = currentExpected.getStart();
        int expectedEnd = currentExpected.getEnd();

        int actualStart = currentActual.getStart();
        int actualEnd = currentActual.getEnd();
        assert expectedStart == actualStart && expectedEnd == actualEnd;

        Map<Diagnostic, TextDiagnostic> actualDiagnostics = currentActual.getTextDiagnosticsMap();
        List<TextDiagnostic> expectedDiagnostics = currentExpected.getDiagnostics();

        for (TextDiagnostic expectedDiagnostic : expectedDiagnostics) {
            boolean diagnosticFound = false;
            for (Diagnostic actualDiagnostic : actualDiagnostics.keySet()) {
                TextDiagnostic actualTextDiagnostic = actualDiagnostics.get(actualDiagnostic);
                if (expectedDiagnostic.getName().equals(actualTextDiagnostic.getName())) {
                    if (!compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                        callbacks.wrongParametersDiagnostic(expectedDiagnostic, actualTextDiagnostic, expectedStart, expectedEnd);
                    }

                    actualDiagnostics.remove(actualDiagnostic);
                    diagnosticToInput.put(actualDiagnostic, expectedDiagnostic);
                    diagnosticFound = true;
                    break;
                }
            }
            if (!diagnosticFound) callbacks.missingDiagnostic(expectedDiagnostic, expectedStart, expectedEnd);
        }

        for (TextDiagnostic unexpectedDiagnostic : actualDiagnostics.values()) {
            callbacks.unexpectedDiagnostic(unexpectedDiagnostic, actualStart, actualEnd);
        }
    }

    private static boolean compareTextDiagnostic(@NotNull TextDiagnostic expected, @NotNull TextDiagnostic actual) {
        if (!expected.getName().equals(actual.getName())) return false;

        if (expected.getParameters() == null) return true;
        if (actual.getParameters() == null || expected.getParameters().size() != actual.getParameters().size()) return false;

        for (int index = 0; index < expected.getParameters().size(); index++) {
            String expectedParameter = expected.getParameters().get(index);
            String actualParameter = actual.getParameters().get(index);
            if (!expectedParameter.equals(IGNORE_DIAGNOSTIC_PARAMETER) && !expectedParameter.equals(actualParameter)) {
                return false;
            }
        }
        return true;
    }

    private static void assertSameFile(Collection<Diagnostic> actual) {
        if (actual.isEmpty()) return;
        PsiFile file = actual.iterator().next().getPsiElement().getContainingFile();
        for (Diagnostic diagnostic : actual) {
            assert diagnostic.getPsiFile().equals(file)
                    : "All diagnostics should come from the same file: " + diagnostic.getPsiFile() + ", " + file;
        }
    }

    private static void unexpectedDiagnostics(List<Diagnostic> actual, DiagnosticDiffCallbacks callbacks) {
        for (Diagnostic diagnostic : actual) {
            List<TextRange> textRanges = diagnostic.getTextRanges();
            for (TextRange textRange : textRanges) {
                callbacks.unexpectedDiagnostic(TextDiagnostic.asTextDiagnostic(diagnostic), textRange.getStartOffset(), textRange.getEndOffset());
            }
        }
    }

    private static void missingDiagnostics(DiagnosticDiffCallbacks callbacks, DiagnosedRange currentExpected) {
        for (TextDiagnostic diagnostic : currentExpected.getDiagnostics()) {
            callbacks.missingDiagnostic(diagnostic, currentExpected.getStart(), currentExpected.getEnd());
        }
    }

    private static <T> T safeAdvance(Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static String parseDiagnosedRanges(String text, List<DiagnosedRange> result) {
        Matcher matcher = RANGE_START_OR_END_PATTERN.matcher(text);

        Stack<DiagnosedRange> opened = new Stack<DiagnosedRange>();

        int offsetCompensation = 0;

        while (matcher.find()) {
            int effectiveOffset = matcher.start() - offsetCompensation;
            String matchedText = matcher.group();
            if ("<!>".equals(matchedText)) {
                opened.pop().setEnd(effectiveOffset);
            }
            else {
                Matcher diagnosticTypeMatcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(matchedText);
                DiagnosedRange range = new DiagnosedRange(effectiveOffset);
                while (diagnosticTypeMatcher.find()) {
                    range.addDiagnostic(diagnosticTypeMatcher.group());
                }
                opened.push(range);
                result.add(range);
            }
            offsetCompensation += matchedText.length();
        }

        assert opened.isEmpty() : "Stack is not empty";

        matcher.reset();
        return matcher.replaceAll("");
    }
    
    public static StringBuffer addDiagnosticMarkersToText(@NotNull PsiFile psiFile, @NotNull Collection<Diagnostic> diagnostics) {
        return addDiagnosticMarkersToText(psiFile, diagnostics, Collections.<Diagnostic, TextDiagnostic>emptyMap(), new Function<PsiFile, String>() {
            @Override
            public String fun(PsiFile file) {
                return file.getText();
            }
        });
    }

    public static StringBuffer addDiagnosticMarkersToText(
            @NotNull final PsiFile psiFile,
            @NotNull Collection<Diagnostic> diagnostics,
            @NotNull Map<Diagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic,
            @NotNull Function<PsiFile, String> getFileText
    ) {
        String text = getFileText.fun(psiFile);
        StringBuffer result = new StringBuffer();
        diagnostics = Collections2.filter(diagnostics, new Predicate<Diagnostic>() {
            @Override
            public boolean apply(Diagnostic diagnostic) {
                return psiFile.equals(diagnostic.getPsiFile());
            }
        });
        if (!diagnostics.isEmpty()) {
            List<DiagnosticDescriptor> diagnosticDescriptors = getSortedDiagnosticDescriptors(diagnostics);

            Stack<DiagnosticDescriptor> opened = new Stack<DiagnosticDescriptor>();
            ListIterator<DiagnosticDescriptor> iterator = diagnosticDescriptors.listIterator();
            DiagnosticDescriptor currentDescriptor = iterator.next();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                while (!opened.isEmpty() && i == opened.peek().end) {
                    closeDiagnosticString(result);
                    opened.pop();
                }
                while (currentDescriptor != null && i == currentDescriptor.start) {
                    openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic);
                    if (currentDescriptor.getEnd() == i) {
                        closeDiagnosticString(result);
                    }
                    else {
                        opened.push(currentDescriptor);
                    }
                    if (iterator.hasNext()) {
                        currentDescriptor = iterator.next();
                    }
                    else {
                        currentDescriptor = null;
                    }
                }
                result.append(c);
            }
            
            if (currentDescriptor != null) {
                assert currentDescriptor.start == text.length();
                assert currentDescriptor.end == text.length();
                openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic);
                opened.push(currentDescriptor);
            }
            
            while (!opened.isEmpty() && text.length() == opened.peek().end) {
                closeDiagnosticString(result);
                opened.pop();
            }

            assert opened.isEmpty() : "Stack is not empty: " + opened;

        }
        else {
            result.append(text);
        }
        return result;
    }

    private static void openDiagnosticsString(
            StringBuffer result,
            DiagnosticDescriptor currentDescriptor,
            Map<Diagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic
    ) {
        result.append("<!");
        for (Iterator<Diagnostic> iterator = currentDescriptor.diagnostics.iterator(); iterator.hasNext(); ) {
            Diagnostic diagnostic = iterator.next();
            if (diagnosticToExpectedDiagnostic.containsKey(diagnostic)) {
                TextDiagnostic expectedDiagnostic = diagnosticToExpectedDiagnostic.get(diagnostic);
                TextDiagnostic actualTextDiagnostic = TextDiagnostic.asTextDiagnostic(diagnostic);
                if (compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                    result.append(expectedDiagnostic.asString());
                } else {
                    result.append(actualTextDiagnostic.asString());
                }
            } else {
                result.append(diagnostic.getFactory().getName());
            }
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        result.append("!>");
    }

    private static void closeDiagnosticString(StringBuffer result) {
        result.append("<!>");
    }

    public static class AbstractDiagnosticForTests implements Diagnostic {
        private final PsiElement element;
        private final DiagnosticFactory<?> factory;

        public AbstractDiagnosticForTests(@NotNull PsiElement element, @NotNull DiagnosticFactory<?> factory) {
            this.element = element;
            this.factory = factory;
        }

        @NotNull
        @Override
        public DiagnosticFactory<?> getFactory() {
            return factory;
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return Severity.ERROR;
        }

        @NotNull
        @Override
        public PsiElement getPsiElement() {
            return element;
        }

        @NotNull
        @Override
        public List<TextRange> getTextRanges() {
            return Collections.singletonList(element.getTextRange());
        }

        @NotNull
        @Override
        public PsiFile getPsiFile() {
            return element.getContainingFile();
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    public static class SyntaxErrorDiagnosticFactory extends DiagnosticFactory<SyntaxErrorDiagnostic> {
        public static final SyntaxErrorDiagnosticFactory INSTANCE = new SyntaxErrorDiagnosticFactory();

        private SyntaxErrorDiagnosticFactory() {
            super(Severity.ERROR);
        }

        @NotNull
        @Override
        public String getName() {
            return "SYNTAX";
        }
    }

    public static class SyntaxErrorDiagnostic extends AbstractDiagnosticForTests {
        public SyntaxErrorDiagnostic(@NotNull PsiErrorElement errorElement) {
            super(errorElement, SyntaxErrorDiagnosticFactory.INSTANCE);
        }
    }

    public static class DebugInfoDiagnosticFactory extends DiagnosticFactory<DebugInfoDiagnostic> {
        public static final DebugInfoDiagnosticFactory SMARTCAST = new DebugInfoDiagnosticFactory("SMARTCAST");
        public static final DebugInfoDiagnosticFactory ELEMENT_WITH_ERROR_TYPE = new DebugInfoDiagnosticFactory("ELEMENT_WITH_ERROR_TYPE");
        public static final DebugInfoDiagnosticFactory UNRESOLVED_WITH_TARGET = new DebugInfoDiagnosticFactory("UNRESOLVED_WITH_TARGET");
        public static final DebugInfoDiagnosticFactory MISSING_UNRESOLVED = new DebugInfoDiagnosticFactory("MISSING_UNRESOLVED");
        public static final DebugInfoDiagnosticFactory DYNAMIC = new DebugInfoDiagnosticFactory("DYNAMIC");

        private final String name;
        private DebugInfoDiagnosticFactory(String name, Severity severity) {
            super(severity);
            this.name = name;
        }

        private DebugInfoDiagnosticFactory(String name) {
            this(name, Severity.ERROR);
        }

        @NotNull
        @Override
        public String getName() {
            return "DEBUG_INFO_" + name;
        }
    }

    public static class DebugInfoDiagnostic extends AbstractDiagnosticForTests {
        public DebugInfoDiagnostic(@NotNull JetElement element, @NotNull DebugInfoDiagnosticFactory factory) {
            super(element, factory);
        }
    }

    @NotNull
    private static List<DiagnosticDescriptor> getSortedDiagnosticDescriptors(@NotNull Collection<Diagnostic> diagnostics) {
        LinkedListMultimap<TextRange, Diagnostic> diagnosticsGroupedByRanges = LinkedListMultimap.create();
        for (Diagnostic diagnostic : diagnostics) {
            if (!diagnostic.isValid()) continue;
            for (TextRange textRange : diagnostic.getTextRanges()) {
                diagnosticsGroupedByRanges.put(textRange, diagnostic);
            }
        }
        List<DiagnosticDescriptor> diagnosticDescriptors = Lists.newArrayList();
        for (TextRange range : diagnosticsGroupedByRanges.keySet()) {
            diagnosticDescriptors.add(
                    new DiagnosticDescriptor(range.getStartOffset(), range.getEndOffset(), diagnosticsGroupedByRanges.get(range)));
        }
        Collections.sort(diagnosticDescriptors, new Comparator<DiagnosticDescriptor>() {
            @Override
            public int compare(@NotNull DiagnosticDescriptor d1, @NotNull DiagnosticDescriptor d2) {
                // Start early -- go first; start at the same offset, the one who end later is the outer, i.e. goes first
                return (d1.start != d2.start) ? d1.start - d2.start : d2.end - d1.end;
            }
        });
        return diagnosticDescriptors;
    }

    private static class DiagnosticDescriptor {
        private final int start;
        private final int end;
        private final List<Diagnostic> diagnostics;

        DiagnosticDescriptor(int start, int end, List<Diagnostic> diagnostics) {
            this.start = start;
            this.end = end;
            this.diagnostics = diagnostics;
        }

        public Map<Diagnostic, TextDiagnostic> getTextDiagnosticsMap() {
            Map<Diagnostic, TextDiagnostic> diagnosticMap = new IdentityHashMap<Diagnostic, TextDiagnostic>();
            for (Diagnostic diagnostic : diagnostics) {
                diagnosticMap.put(diagnostic, TextDiagnostic.asTextDiagnostic(diagnostic));
            }
            return diagnosticMap;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public TextRange getTextRange() {
            return new TextRange(start, end);
        }
    }

    public static class TextDiagnostic {
        @NotNull
        private static TextDiagnostic parseDiagnostic(String text) {
            Matcher matcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(text);
            if (!matcher.find())
                throw new IllegalArgumentException("Could not parse diagnostic: " + text);
            String name = matcher.group(1);

            String parameters = matcher.group(2);
            if (parameters == null) {
                return new TextDiagnostic(name, null);
            }

            List<String> parsedParameters = new SmartList<String>();
            Matcher parametersMatcher = INDIVIDUAL_PARAMETER_PATTERN.matcher(parameters);
            while (parametersMatcher.find())
                parsedParameters.add(parametersMatcher.group().trim());
            return new TextDiagnostic(name, parsedParameters);
        }

        @NotNull
        public static TextDiagnostic asTextDiagnostic(@NotNull Diagnostic diagnostic) {
            DiagnosticRenderer renderer = getRenderer(diagnostic);
            String diagnosticName = diagnostic.getFactory().getName();
            if (renderer instanceof AbstractDiagnosticWithParametersRenderer) {
                //noinspection unchecked
                Object[] renderParameters = ((AbstractDiagnosticWithParametersRenderer) renderer).renderParameters(diagnostic);
                List<String> parameters = ContainerUtil.map(renderParameters, new Function<Object, String>() {
                    @Override
                    public String fun(Object o) {
                        return o != null ? o.toString() : "null";
                    }
                });
                return new TextDiagnostic(diagnosticName, parameters);
            }
            return new TextDiagnostic(diagnosticName, null);
        }

        @Nullable
        private static DiagnosticRenderer getRenderer(@NotNull Diagnostic diagnostic) {
            for (DiagnosticFactoryToRendererMap map : DefaultErrorMessages.MAPS) {
                DiagnosticRenderer renderer = map.get(diagnostic.getFactory());
                if (renderer != null)
                    return renderer;
            }
            return null;
        }


        @NotNull
        private final String name;
        @Nullable
        private final List<String> parameters;

        public TextDiagnostic(@NotNull String name, @Nullable List<String> parameters) {
            this.name = name;
            this.parameters = parameters;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @Nullable
        public List<String> getParameters() {
            return parameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TextDiagnostic that = (TextDiagnostic) o;

            if (!name.equals(that.name)) return false;
            if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
            return result;
        }

        @NotNull
        public String asString() {
            if (parameters == null)
                return name;
            return name + '(' + StringUtil.join(parameters, "; ") + ')';
        }
    }

    public static class DiagnosedRange {
        private final int start;
        private int end;
        private final List<TextDiagnostic> diagnostics = ContainerUtil.newSmartList();
        private PsiFile file;

        protected DiagnosedRange(int start) {
            this.start = start;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public List<TextDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public void setEnd(int end) {
            this.end = end;
        }
        
        public void addDiagnostic(String diagnostic) {
            diagnostics.add(TextDiagnostic.parseDiagnostic(diagnostic));
        }

        public void setFile(@NotNull PsiFile file) {
            this.file = file;
        }

        @NotNull
        public PsiFile getFile() {
            return file;
        }
    }
}
