/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.buildinit.plugins.internal

import org.gradle.api.internal.file.BaseDirFileResolver
import org.gradle.api.internal.file.TestFiles
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

import static org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl.KOTLIN
import static org.gradle.util.TextUtil.toPlatformLineSeparators

class BuildScriptBuilderKotlinTest extends Specification {

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()

    def fileResolver = new BaseDirFileResolver(tmpDir.testDirectory, TestFiles.patternSetFactory)
    def builder = new BuildScriptBuilder(KOTLIN, fileResolver, "build")

    def outputFile = tmpDir.file("build.gradle.kts")

    def "generates basic kotlin build script"() {
        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */
""")
    }

    def "no spaces at the end of blank comment lines"() {
        when:
        builder.fileComment("\nnot-a-blank\n\nnot-a-blank");
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 *
 * not-a-blank
 *
 * not-a-blank
 */
""")
    }

    def "can add kotlin build script comment"() {
        when:
        builder.fileComment("""This is a sample
see more at gradle.org""")
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a sample
 * see more at gradle.org
 */
""")
    }

    def "can add plugins"() {
        when:
        builder.plugin("Add support for the Java language", "java")
        builder.plugin("Add support for Java libraries", "java-library")
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    // Add support for the Java language
    java

    // Add support for Java libraries
    `java-library`
}
""")
    }

    def "can add repositories"() {
        given:
        builder.repositories().mavenLocal("Use maven local")
        builder.repositories().maven("Use another repo as well", "https://somewhere")
        builder.repositories().maven(null, "https://somewhere/2")
        builder.repositories().jcenter("Use JCenter")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

repositories {
    // Use maven local
    mavenLocal()

    // Use another repo as well
    maven {
        url = "https://somewhere"
    }

    maven {
        url = "https://somewhere/2"
    }

    // Use JCenter
    jcenter()
}
""")
    }

    def "can add compile dependencies"() {
        when:
        builder.implementationDependency("Use slf4j", "org.slf4j:slf4j-api:2.7", "org.slf4j:slf4j-simple:2.7")
        builder.implementationDependency(null, "a:b:1.2", "a:c:4.5")
        builder.implementationDependency(null, "a:d:4.5")
        builder.implementationDependency("Use Scala to compile", "org.scala-lang:scala-library:2.10")
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // Use slf4j
    implementation("org.slf4j:slf4j-api:2.7")
    implementation("org.slf4j:slf4j-simple:2.7")

    implementation("a:b:1.2")
    implementation("a:c:4.5")
    implementation("a:d:4.5")

    // Use Scala to compile
    implementation("org.scala-lang:scala-library:2.10")
}
""")
    }

    def "can add test compile and runtime dependencies"() {
        when:
        builder.testImplementationDependency("use some test kit", "org:test:1.2", "org:test-utils:1.2")
        builder.testRuntimeOnlyDependency("needs some libraries at runtime", "org:test-runtime:1.2")
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // use some test kit
    testImplementation("org:test:1.2")
    testImplementation("org:test-utils:1.2")

    // needs some libraries at runtime
    testRuntimeOnly("org:test-runtime:1.2")
}
""")
    }

    def "can add project dependencies"() {
        given:
        builder.dependencies().projectDependency("implementation", "use some lib", ":abc")
        builder.dependencies().projectDependency("testImplementation", null, ":p1")
        builder.dependencies().projectDependency("implementation", null, ":p2")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

dependencies {
    // use some lib
    implementation(project(":abc"))

    implementation(project(":p2"))
    testImplementation(project(":p1"))
}
""")
    }

    def "can define tasks in allprojects block"() {
        given:
        def body = builder.allprojects().taskRegistration("Compile stuff", "compile", "JavaCompile")
        body.propertyAssignment("Set a property", "foo.bar", "bazar")
        body.methodInvocation("Call a method", "thing", "value")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

allprojects {
    // Compile stuff
    val compile by tasks.creating(JavaCompile::class) {
        // Set a property
        foo.bar = "bazar"

        // Call a method
        thing("value")
    }
}
""")
    }

    def "can add repositories in allprojects block"() {
        given:
        def allprojects = builder.allprojects()
        allprojects.repositories().mavenLocal("Use maven local")
        allprojects.repositories().maven("Use another repo as well", "https://somewhere")
        allprojects.repositories().maven(null, "https://somewhere/2")
        allprojects.repositories().jcenter("Use JCenter")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

allprojects {
    repositories {
        // Use maven local
        mavenLocal()

        // Use another repo as well
        maven {
            url = "https://somewhere"
        }

        maven {
            url = "https://somewhere/2"
        }

        // Use JCenter
        jcenter()
    }
}
""")
    }

    def "can add dependencies in allprojects block"() {
        given:
        def allprojects = builder.allprojects()
        allprojects.dependencies().projectDependency("implementation", "use some lib", ":abc")
        allprojects.dependencies().projectDependency("testImplementation", null, ":p1")
        allprojects.dependencies().dependency("implementation", null, "a:b:1.2")
        allprojects.dependencies().dependency("testImplementation", null, "a:b:1.2")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

allprojects {
    dependencies {
        // use some lib
        implementation(project(":abc"))

        implementation("a:b:1.2")
        testImplementation(project(":p1"))
        testImplementation("a:b:1.2")
    }
}
""")
    }

    def "can add allprojects block"() {
        given:
        def block = builder.allprojects()
        block.plugin("Apply java plugin", "java")
        block.plugin("Apply application plugin", "application")
        block.repositories().mavenLocal(null)
        block.propertyAssignment("Set a property", "foo.bar", "bazar")
        def nested = block.block("A block", "nested")
        nested.propertyAssignment(null, "foo.bar", "bazar")
        block.taskPropertyAssignment("Configure a thing", "SomeType", "foo.bar", "bazar")
        block.taskPropertyAssignment(null, "SomeType", "thing", 45)
        block.taskPropertyAssignment(null, "AType", "foo.bar", "bazar")
        block.dependencies().projectDependency("api", "Project dep", ":pr1")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

allprojects {
    // Apply java plugin
    plugins.apply("java")

    // Apply application plugin
    plugins.apply("application")

    repositories {
        mavenLocal()
    }

    dependencies {
        // Project dep
        api(project(":pr1"))
    }

    // Set a property
    foo.bar = "bazar"

    // A block
    nested {
        foo.bar = "bazar"
    }

    tasks.withType(SomeType) {
        // Configure a thing
        foo.bar = "bazar"

        thing = 45
    }

    tasks.withType(AType) {
        foo.bar = "bazar"
    }
}
""")
    }

    def "can add subprojects block"() {
        given:
        def block = builder.subprojects()
        block.plugin("Apply java plugin", "java")
        block.plugin("Apply application plugin", "application")
        block.repositories().mavenLocal(null)
        block.propertyAssignment("Set a property", "foo.bar", "bazar")
        def nested = block.block("A block", "nested")
        nested.propertyAssignment(null, "foo.bar", "bazar")
        block.taskPropertyAssignment("Configure a thing", "SomeType", "foo.bar", "bazar")
        block.taskPropertyAssignment(null, "SomeType", "thing", 45)
        block.taskPropertyAssignment(null, "AType", "foo.bar", "bazar")
        block.dependencies().projectDependency("api", "Project dep", ":pr1")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

subprojects {
    // Apply java plugin
    plugins.apply("java")

    // Apply application plugin
    plugins.apply("application")

    repositories {
        mavenLocal()
    }

    dependencies {
        // Project dep
        api(project(":pr1"))
    }

    // Set a property
    foo.bar = "bazar"

    // A block
    nested {
        foo.bar = "bazar"
    }

    tasks.withType(SomeType) {
        // Configure a thing
        foo.bar = "bazar"

        thing = 45
    }

    tasks.withType(AType) {
        foo.bar = "bazar"
    }
}
""")
    }

    def "can add property assignments"() {
        given:
        builder
            .propertyAssignment("Set a property", "foo.bar", "bazar")
            .propertyAssignment(null, "cathedral", 42)
            .propertyAssignment("Use a map", "cathedral", [a: 12, b: "value"])
            .propertyAssignment("Use a method call", "cathedral", builder.methodInvocationExpression("thing", 123, false))

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Set a property
foo.bar = "bazar"

cathedral = 42

// Use a map
cathedral = mapOf("a" to 12, "b" to "value")

// Use a method call
cathedral = thing(123, false)
""")
    }

    def "can add method invocations"() {
        given:
        builder
            .methodInvocation("No args", "foo.bar")
            .methodInvocation(null, "cathedral", 42)
            .methodInvocation("Use a map", "cathedral", [a: 12, b: "value"])
            .methodInvocation("Use a method call", "cathedral", builder.methodInvocationExpression("thing", [a: 12], 123, false))

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// No args
foo.bar()

cathedral(42)

// Use a map
cathedral(mapOf("a" to 12, "b" to "value"))

// Use a method call
cathedral(thing(mapOf("a" to 12), 123, false))
""")
    }

    def "can add code blocks"() {
        given:
        def block1 = builder.block("Add some thing", "foo.bar")
        block1.propertyAssignment(null, "foo.bar", "bazar")
        def block2 = builder.block("Do it again", "foo.bar")
        block2.propertyAssignment(null, "foo.bar", "bazar")
        builder.block(null, "other")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo.bar {
    foo.bar = "bazar"
}

// Do it again
foo.bar {
    foo.bar = "bazar"
}

other {
}
""")
    }

    def "can add elements to top level containers and reference the element later"() {
        given:
        def e1 = builder.containerElement("Add some thing", "foo.bar", "e1")
        def e2 = builder.containerElement(null, "foo.bar", "e2")
        builder.propertyAssignment("Set some thing", "prop", e1)
        builder.propertyAssignment(null, "prop2", builder.propertyExpression(e2, "outputDir"))

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo.bar.create("e1") {
}

foo.bar.create("e2") {
}

// Set some thing
prop = foo.bar.get("e1")

prop2 = foo.bar.get("e2").outputDir
""")
    }

    def "can add container elements to nested containers and reference the element later"() {
        given:
        builder.block("Add some thing", "foo") { b ->
            def element1 = b.containerElement("Element 1", "bar", "one") { e ->
                e.propertyAssignment(null, "value", "bazar")
                e.containerElement(null, "nested", "oneNested") {}
            }
            b.containerElement("Element 2", "bar", "two") { e ->
            }
            b.propertyAssignment("Use value", "prop", element1)
        }

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Add some thing
foo {
    // Element 1
    bar.create("one") {
        value = "bazar"

        nested.create("oneNested") {
        }
    }

    // Element 2
    bar.create("two") {
    }

    // Use value
    prop = bar.get("one")
}
""")
    }

    def "can add further configuration"() {
        given:
        builder
            .taskPropertyAssignment(null, "test", "Test", "maxParallelForks", 23)
            .propertyAssignment("Set a property", "foo.bar", "bazar")
            .methodInvocation("Call a method", "foo.bar", "bazar", 12, builder.methodInvocationExpression("child", "a", 45))
            .conventionPropertyAssignment("Convention configuration A", "application", "mainClassName", "com.example.Main")
            .conventionPropertyAssignment("Convention configuration B", "application", "applicationName", "My Application")
            .conventionPropertyAssignment("B convention", "b", "bp", 0)
            .conventionPropertyAssignment("C convention", "c", "cp", 42)
            .taskMethodInvocation("Use TestNG", "test", "Test", "useTestNG")
            .propertyAssignment(null, "cathedral", 42)
            .methodInvocation(null, "cathedral")
            .taskPropertyAssignment("Disable tests", "test", "Test", "enabled", false)
            .taskPropertyAssignment("Encoding", "Test", "encoding", "UTF-8")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

// Set a property
foo.bar = "bazar"

// Call a method
foo.bar("bazar", 12, child("a", 45))

cathedral = 42
cathedral()

application {
    // Convention configuration A
    mainClassName = "com.example.Main"

    // Convention configuration B
    applicationName = "My Application"
}

b {
    // B convention
    bp = 0
}

c {
    // C convention
    cp = 42
}

tasks.withType(Test) {
    // Encoding
    encoding = "UTF-8"
}

val test by tasks.getting(Test::class) {
    maxParallelForks = 23

    // Use TestNG
    useTestNG()

    // Disable tests
    isEnabled = false
}
""")
    }

    def "statements can have multi-line comment"() {
        given:
        builder.repositories().jcenter("""
Use jcenter

Alternatively:
- Could use Maven central instead
- Or a local mirror

""")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

repositories {
    // Use jcenter
    // 
    // Alternatively:
    // - Could use Maven central instead
    // - Or a local mirror
    jcenter()
}
""")
    }

    def "vertical whitespace is included around statements with comments and and around blocks"() {
        given:
        builder.propertyAssignment(null, "foo", "bar")
        builder.methodInvocation(null, "foo", "bar")
        builder.propertyAssignment("has comment", "foo", "bar")
        builder.propertyAssignment(null, "foo", 123)
        builder.propertyAssignment(null, "foo", false)
        builder.conventionPropertyAssignment(null, "application", "main", true)
        def b1 = builder.block(null, "block1")
        b1.methodInvocation("comment", "method1")
        b1.methodInvocation("comment", "method2")
        b1.methodInvocation(null, "method3")
        b1.methodInvocation(null, "method4")
        def b2 = builder.block("another block", "block2")
        b2.methodInvocation(null, "method1")
        b2.propertyAssignment(null, "foo", "bar")
        builder.propertyAssignment(null, "foo", "second last")
        builder.propertyAssignment(null, "foo", "last")

        when:
        builder.create().generate()

        then:
        assertOutputFile("""/*
 * This file was generated by the Gradle 'init' task.
 */

foo = "bar"
foo("bar")

// has comment
foo = "bar"

foo = 123
isFoo = false

block1 {
    // comment
    method1()

    // comment
    method2()

    method3()
    method4()
}

// another block
block2 {
    method1()
    foo = "bar"
}

foo = "second last"
foo = "last"

application {
    isMain = true
}
""")
    }

    void assertOutputFile(String contents) {
        assert outputFile.file
        assert outputFile.text == toPlatformLineSeparators(contents)
    }
}
