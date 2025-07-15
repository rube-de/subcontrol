# Learnings from Fixing SubControl Test Suite

This document captures key learnings from debugging and fixing test failures in the SubControl Android project.

## 1. Kotlin Object Singleton Mocking

**Issue**: Tests were failing because `EncryptionManager` is a Kotlin `object` (singleton), not a class.

**Learning**: When mocking Kotlin objects with MockK, you need to use `mockkObject()` instead of regular `mockk()`:

```kotlin
// Wrong - for classes
val encryptionManager = mockk<EncryptionManager>()

// Correct - for objects
mockkObject(EncryptionManager)
every { EncryptionManager.encrypt(any()) } returns byteArrayOf()
```

## 2. Coroutine Test Dispatcher Issues

**Issue**: Tests were failing with "Detected use of different schedulers" error.

**Learning**: When testing coroutines, ensure all components use the same test dispatcher:

```kotlin
@Test
fun `test with coroutines`() = runTest {
    // Pass the test scheduler's dispatcher to the repository
    repository = SubscriptionRepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
    // ... test code
}
```

## 3. Flow Exception Handling

**Issue**: Repository catches exceptions and emits empty lists, but tests expected errors to propagate.

**Learning**: When testing Flows that handle exceptions internally:
- Check the actual implementation to understand error handling behavior
- Mock Flows that throw exceptions during collection, not during setup:

```kotlin
// Wrong - throws immediately during mock setup
coEvery { dataStore.data } throws Exception("Error")

// Correct - throws during Flow collection
coEvery { dataStore.data } returns flow { throw Exception("Error") }
```

## 4. BigDecimal Comparison

**Issue**: BigDecimal comparison failed due to scale differences (0 vs 0.00).

**Learning**: Use `compareTo()` for BigDecimal equality checks to ignore scale:

```kotlin
// Wrong - scale-sensitive
assertEquals(BigDecimal.ZERO, result)

// Correct - scale-insensitive
assertTrue(result.compareTo(BigDecimal.ZERO) == 0)
```

## 5. Proto DataStore Testing

**Issue**: Tests used non-existent `FakeAppData` class.

**Learning**: Use actual Proto-generated classes for testing:

```kotlin
private fun createAppData(subscriptions: List<Subscription>): AppData {
    val protoSubscriptions = subscriptions.map { it.toProto() }
    return AppData.newBuilder()
        .setSubscriptionList(
            SubscriptionList.newBuilder()
                .addAllSubscriptions(protoSubscriptions)
                .build()
        )
        .build()
}
```

## 6. Import Conflicts

**Issue**: Tests had wrong imports (kotlin.test instead of JUnit).

**Learning**: Be careful with auto-imports. Android projects typically use JUnit:

```kotlin
// Wrong
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Correct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
```

## 7. Gradle DSL Format Differences

**Issue**: Version parser expected Groovy format but the project uses Kotlin DSL.

**Learning**: Support both Gradle formats when parsing build files:

```kotlin
// Support both formats:
// Groovy: versionName "1.0.0"
// Kotlin: versionName = "1.0.0"
val versionNameRegex = Regex("versionName\\s*=?\\s*\"([^\"]+)\"")
```

## 8. Test Data Validation

**Issue**: Changelog tests failed because commit hashes were too short.

**Learning**: Validate test data matches implementation expectations:
- Commit hashes need at least 7 characters
- Check substring operations won't exceed string length
- Ensure test data is realistic

## 9. Dependency Management

**Issue**: Missing material-icons-extended dependency caused compilation errors.

**Learning**: When fixing UI-related compilation errors:
- Check if the icon/component is from an extended library
- Add dependencies to the appropriate bundle in `libs.versions.toml`
- Ensure the dependency is included in the module's build.gradle

## 10. Flow Testing with Turbine

**Issue**: Understanding how to properly test Flow emissions and errors.

**Learning**: Turbine provides a clean API for Flow testing:

```kotlin
repository.getAllSubscriptions().test {
    val result = awaitItem()  // Get first emission
    assertEquals(expected, result)
    awaitComplete()  // Ensure Flow completes
}
```

## 11. Method Name Refactoring

**Issue**: Code was using old method names that had been refactored.

**Learning**: When refactoring method names:
- Use IDE's refactoring tools to catch all usages
- Run tests immediately after refactoring
- Check for string-based references that IDEs might miss

## 12. Test Organization

**Learning**: Good test organization practices observed:
- Test structure mirrors main source structure
- Each test class focuses on a single class/component
- Test names clearly describe what they're testing
- Use of `@Before` for common setup
- Proper use of mocking for dependencies

## Key Takeaways

1. **Always read the actual implementation** before writing or fixing tests
2. **Use proper test utilities** for the framework (MockK for Kotlin, Turbine for Flows)
3. **Understand the threading model** when testing coroutines
4. **Match test data to implementation requirements** (string lengths, formats, etc.)
5. **Be careful with auto-imports** - verify they're from the correct library
6. **Test both success and failure paths** including exception handling
7. **Keep tests focused and isolated** - one concept per test
8. **Use meaningful test names** that describe the scenario and expected outcome

## Tools and Libraries Used

- **MockK**: Kotlin mocking library with special support for objects, coroutines
- **Turbine**: Flow testing library for clean assertions
- **JUnit 4**: Test framework (not kotlin.test)
- **Coroutines Test**: Provides TestDispatcher and runTest
- **Protocol Buffers**: For data serialization testing

## Common Patterns

### Repository Testing Pattern
```kotlin
@Test
fun `repository operation succeeds`() = runTest {
    // Given
    repository = RepositoryImpl(dataStore, UnconfinedTestDispatcher(testScheduler))
    coEvery { dataStore.data } returns flowOf(testData)
    
    // When
    val result = repository.getData()
    
    // Then
    result.test {
        assertEquals(expected, awaitItem())
        awaitComplete()
    }
}
```

### ViewModel Testing Pattern
```kotlin
@Test
fun `viewmodel updates UI state`() = runTest {
    // Given
    every { mockRepository.getData() } returns flowOf(testData)
    
    // When
    viewModel = TestViewModel(mockRepository)
    
    // Then
    viewModel.uiState.test {
        val state = awaitItem()
        assertEquals(expected, state.data)
        assertFalse(state.isLoading)
    }
}
```