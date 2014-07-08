/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativebinaries;

import org.gradle.api.file.FileCollection;

import java.io.File;

/**
 * A static library binary built by Gradle for a native library.
 */
public interface ProjectStaticLibraryBinary extends ProjectNativeBinary {
    /**
     * The static library file.
     */
    File getStaticLibraryFile();

    /**
     * The static library binary file.
     */
    void setStaticLibraryFile(File staticLibraryFile);

    /**
     * Add some additional files required at link time.
     */
    void additionalLinkFiles(FileCollection files);
}
