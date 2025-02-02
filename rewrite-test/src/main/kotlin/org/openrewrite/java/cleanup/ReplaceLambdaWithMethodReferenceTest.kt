/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.cleanup

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.tree.J

interface ReplaceLambdaWithMethodReferenceTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = ReplaceLambdaWithMethodReference()

    @Test
    fun containsMultipleStatements() = assertUnchanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                List<Integer> even(List<Integer> l) {
                    return l.stream().map(n -> {
                        if (n % 2 == 0) return n;
                        return n * 2;
                    }).collect(Collectors.toList());
                }
            }
        """
    )

    @Test
    fun instanceOf() = assertChanged(
        dependsOn = arrayOf("""
            package org.test;
            public class CheckType {
            }
        """),
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> method(List<Object> input) {
                    return input.stream().filter(n -> n instanceof CheckType).collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> method(List<Object> input) {
                    return input.stream().filter(CheckType.class::isInstance).collect(Collectors.toList());
                }
            }
        """,
        afterConditions = { cu ->
            val value = (((((((cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.Return)
                .expression as J.MethodInvocation)
                .select as J.MethodInvocation)
                .arguments[0] as J.MemberReference)
                .containing as J.FieldAccess)
                .target as J.Identifier).type!!.toString()
            assertThat(value).isEqualTo("org.test.CheckType")
        }
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun systemOutPrint() = assertChanged(
        before = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(x -> System.out.println(x));
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(System.out::println);
                }
            }
        """
    )

    @Suppress("Convert2MethodRef", "CodeBlock2Expr")
    @Test
    fun systemOutPrintInBlock() = assertChanged(
        before = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(x -> { System.out.println(x); });
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                void method(List<Integer> input) {
                    return input.forEach(System.out::println);
                }
            }
        """
    )

    @Suppress("RedundantCast")
    @Test
    fun castType() = assertChanged(
        dependsOn = arrayOf("""
            package org.test;
            public class CheckType {
            }
        """),
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(CheckType.class::isInstance)
                        .map(o -> (CheckType) o)
                        .collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.stream.Collectors;

            import org.test.CheckType;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(CheckType.class::isInstance)
                        .map(CheckType.class::cast)
                        .collect(Collectors.toList());
                }
            }
        """,
        afterConditions = { cu ->
            val value = (((((((cu.classes[0].body.statements[0] as J.MethodDeclaration)
                .body!!.statements[0] as J.Return)
                .expression as J.MethodInvocation)
                .select as J.MethodInvocation)
                .arguments[0] as J.MemberReference)
                .containing as J.FieldAccess)
                .target as J.Identifier).type!!.toString()
            assertThat(value).isEqualTo("org.test.CheckType")
        }
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun notEqualToNull() = assertChanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(o -> o != null)
                        .collect(Collectors.toList());
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.Objects;
            import java.util.stream.Collectors;

            class Test {
                List<Object> filter(List<Object> l) {
                    return l.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                }
            }
        """
    )

    @Suppress("Convert2MethodRef")
    @Test
    fun isEqualToNull() = assertChanged(
        before = """
            import java.util.List;
            import java.util.stream.Collectors;

            class Test {
                boolean containsNull(List<Object> l) {
                    return l.stream()
                        .anyMatch(o -> o == null);
                }
            }
        """,
        after = """
            import java.util.List;
            import java.util.Objects;
            import java.util.stream.Collectors;

            class Test {
                boolean containsNull(List<Object> l) {
                    return l.stream()
                        .anyMatch(Objects::isNull);
                }
            }
        """
    )
}
