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

package jet.objc;

import java.io.File;
import java.io.InputStream;

import static kotlin.io.IoPackage.readBytes;
import static kotlin.io.IoPackage.writeBytes;

@SuppressWarnings("UnusedDeclaration")
public class Native {
    private Native() {}

    public static final String KOTLIN_NATIVE_LIBRARY_PATH = "/libKotlinNative.dylib";

    static {
        try {
            InputStream resource = Native.class.getResourceAsStream(KOTLIN_NATIVE_LIBRARY_PATH);
            try {
                byte[] bytes = readBytes(resource, resource.available());
                File dylib = File.createTempFile("libKotlinNative", ".dylib");
                writeBytes(dylib, bytes);
                System.load(dylib.getAbsolutePath());
            }
            finally {
                resource.close();
            }
        }
        catch (Throwable e) {
            throw new IllegalStateException("KotlinNative library couldn't be loaded from " + KOTLIN_NATIVE_LIBRARY_PATH, e);
        }
    }

    public static native void dlopen(String path);


    public static native long malloc(long bytes);

    public static native void free(long pointer);

    public static native long getWord(long pointer);

    public static native void setWord(long pointer, long value);


    public static native long objc_getClass(String name);

    public static native Object objc_msgSend(String returnType, ObjCObject receiver, String selectorName, Object... args);
}
