/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetEnumEntry;
import org.jetbrains.jet.lang.psi.stubs.PsiJetEnumEntryStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetEnumEntryStubImpl;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

public class JetEnumEntryElementType extends JetStubElementType<PsiJetEnumEntryStub, JetEnumEntry> {
    public JetEnumEntryElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetEnumEntry.class, PsiJetEnumEntryStub.class);
    }

    @NotNull
    @Override
    public JetEnumEntry createPsi(@NotNull PsiJetEnumEntryStub stub) {
        return new JetEnumEntry(stub);
    }

    @NotNull
    @Override
    public JetEnumEntry createPsiFromAst(@NotNull ASTNode node) {
        return new JetEnumEntry(node);
    }

    @Override
    public PsiJetEnumEntryStub createStub(@NotNull JetEnumEntry psi, StubElement parentStub) {
        FqName fqName = ResolveSessionUtils.safeFqNameForLazyResolve(psi);
        return new PsiJetEnumEntryStubImpl(parentStub, StringRef.fromString(fqName != null ? fqName.asString() : null),
                                           StringRef.fromString(psi.getName()));
    }

    @Override
    public void serialize(@NotNull PsiJetEnumEntryStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());
    }

    @NotNull
    @Override
    public PsiJetEnumEntryStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();
        return new PsiJetEnumEntryStubImpl(parentStub, qualifiedName, name);
    }
}
