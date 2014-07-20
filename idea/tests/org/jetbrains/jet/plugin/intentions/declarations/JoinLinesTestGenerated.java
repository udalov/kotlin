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

package org.jetbrains.jet.plugin.intentions.declarations;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.regex.Pattern;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import org.jetbrains.jet.plugin.intentions.declarations.AbstractJoinLinesTest;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/joinLines")
public class JoinLinesTestGenerated extends AbstractJoinLinesTest {
    public void testAllFilesPresentInJoinLines() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.TestsPackage", new File("idea/testData/joinLines"), Pattern.compile("^(.+)\\.kt$"), true);
    }
    
    @TestMetadata("longInit.kt")
    public void testLongInit() throws Exception {
        doTest("idea/testData/joinLines/longInit.kt");
    }
    
    @TestMetadata("longInit2.kt")
    public void testLongInit2() throws Exception {
        doTest("idea/testData/joinLines/longInit2.kt");
    }
    
    @TestMetadata("simpleInit.kt")
    public void testSimpleInit() throws Exception {
        doTest("idea/testData/joinLines/simpleInit.kt");
    }
    
    @TestMetadata("simpleInit2.kt")
    public void testSimpleInit2() throws Exception {
        doTest("idea/testData/joinLines/simpleInit2.kt");
    }
    
    @TestMetadata("simpleInitWithBackticks.kt")
    public void testSimpleInitWithBackticks() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithBackticks.kt");
    }
    
    @TestMetadata("simpleInitWithBackticks2.kt")
    public void testSimpleInitWithBackticks2() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithBackticks2.kt");
    }
    
    @TestMetadata("simpleInitWithBackticks3.kt")
    public void testSimpleInitWithBackticks3() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithBackticks3.kt");
    }
    
    @TestMetadata("simpleInitWithComments.kt")
    public void testSimpleInitWithComments() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithComments.kt");
    }
    
    @TestMetadata("simpleInitWithComments2.kt")
    public void testSimpleInitWithComments2() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithComments2.kt");
    }
    
    @TestMetadata("simpleInitWithSemicolons.kt")
    public void testSimpleInitWithSemicolons() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithSemicolons.kt");
    }
    
    @TestMetadata("simpleInitWithSemicolons2.kt")
    public void testSimpleInitWithSemicolons2() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithSemicolons2.kt");
    }
    
    @TestMetadata("simpleInitWithSemicolons3.kt")
    public void testSimpleInitWithSemicolons3() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithSemicolons3.kt");
    }
    
    @TestMetadata("simpleInitWithType.kt")
    public void testSimpleInitWithType() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithType.kt");
    }
    
    @TestMetadata("simpleInitWithType2.kt")
    public void testSimpleInitWithType2() throws Exception {
        doTest("idea/testData/joinLines/simpleInitWithType2.kt");
    }
    
}
