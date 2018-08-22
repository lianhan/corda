package net.corda.djvm.rewiring

import foo.bar.sandbox.A
import foo.bar.sandbox.B
import foo.bar.sandbox.Empty
import foo.bar.sandbox.StrictFloat
import net.corda.djvm.TestBase
import net.corda.djvm.assertions.AssertionExtensions.assertThat
import net.corda.djvm.costing.ThresholdViolationException
import net.corda.djvm.execution.ExecutionProfile
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class ClassRewriterTest : TestBase() {

    @Test
    fun `empty transformer does nothing`() = sandbox(BLANK) {
        val callable = newCallable<Empty>()
        assertThat(callable).hasNotBeenModified()
        callable.createAndInvoke()
        assertThat(runtimeCosts).areZero()
    }

    @Test
    fun `can transform class`() = sandbox(DEFAULT) {
        val callable = newCallable<A>()
        assertThat(callable).hasBeenModified()
        callable.createAndInvoke()
        assertThat(runtimeCosts).hasInvocationCost(1)
    }

    @Test
    fun `can transform another class`() = sandbox(DEFAULT) {
        val callable = newCallable<B>()
        assertThat(callable).hasBeenModified()
        assertThat(callable).isSandboxed()
        callable.createAndInvoke()
        assertThat(runtimeCosts)
                .hasInvocationCostGreaterThanOrEqualTo(1) // Includes static constructor calls for java.lang.Math, etc.
                .hasJumpCostGreaterThanOrEqualTo(30 * 2 + 1)
    }

    @Test
    fun `cannot breach threshold`() = sandbox(ExecutionProfile.DISABLE_BRANCHING, DEFAULT) {
        val callable = newCallable<B>()
        assertThat(callable).hasBeenModified()
        assertThat(callable).isSandboxed()
        assertThatExceptionOfType(ThresholdViolationException::class.java).isThrownBy {
            callable.createAndInvoke()
        }.withMessageContaining("terminated due to excessive use of looping")
        assertThat(runtimeCosts)
                .hasAllocationCost(0)
                .hasInvocationCost(1)
                .hasJumpCost(1)
    }

    @Test
    fun `can transform class into using strictfp`() = sandbox(DEFAULT) {
        val callable = newCallable<StrictFloat>()
        assertThat(callable).hasBeenModified()
        callable.createAndInvoke()
    }

}
