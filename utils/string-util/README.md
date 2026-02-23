# StringUtil Library

A lightweight, zero-dependency Java utility library designed for safe and efficient string manipulation. 
This library focuses on handling `null` values gracefully and providing common text-processing patterns 
missing from the standard JDK.

---

## 🚀 Features

### 1. Null-Safe Checks
Check string content without worrying about `NullPointerException`.
* **isBlank**: Returns `true` if the string is null, empty, or contains only whitespace.
* **isNotBlank**: The inverse of `isBlank`.
* **isEmpty**: Returns `true` only if the string is exactly `""`. (Returns `false` for `null`).
* **isNotEmpty**: The inverse of `isEmpty`.

### 2. Comparisons
* **equals / equalsIgnoreCase**: Performs null-safe comparisons between two strings.

### 3. Transformation & Formatting
* **reverse**: Reverses a string (returns `""` if null).
* **strip**: Trims whitespace with an option to return an empty string instead of null.
* **toLowerCase / toUpperCase**: Null-safe case conversion.
* **capitalize**: Capitalizes the first letter and lowercases the rest (e.g., `jOOQ` → `Jooq`).
* **wordCapitalize**: Capitalizes every word in a sentence and applies Unicode NFKC normalization to ensure consistent spacing.

### 4. Advanced Stripping
* **stripLeft**: Removes a specific prefix from the start of a string. Supports `repeat` mode to remove multiple occurrences.
* **stripRight**: Removes a specific suffix from the end of a string. Supports `repeat` mode.

### 5. Splitting (Left & Right)
Enhanced splitting capabilities beyond standard `String.split()`:
* **split**: Split by pattern with options to preserve empty tokens and set limits.
* **rsplit**: Split starting from the **right** side of the string. Useful for parsing file paths or specific suffixes.
* **Preserved Empty**: Methods like `splitByPreservedEmpty` ensure that trailing delimiters result in empty strings in the final list rather than being discarded.

---

## 📦 Installation

Add the following dependency to your build configuration file:

### Maven
Include this in your `pom.xml` within the `<dependencies>` block:

```xml
<dependency>
<groupId>io.github.khezyapp</groupId>
<artifactId>string-util</artifactId>
<version>1.0.0</version>
</dependency>
```

### Gradle
Add this to your `build.gradle` file within the `dependencies` block:

```gradle
implementation 'io.github.khezyapp:string-util:1.0.0'
```

---

## 📖 Usage Examples

### Word Capitalization
```java
String input = "jOOQ sPeCiFiCaTiOn";
String result = StringUtil.wordCapitalize(input);
// Result: "Jooq Specification"
```

### Right Splitting
```java
// Split from the right with a limit of 2
List<String> result = StringUtil.rsplit("a,b,c,d", ",", 2);
// Result: ["a,b,c", "d"]
```

### Prefix/Suffix Stripping
```java
String result = StringUtil.stripLeft("foofoobar", "foo", true);
// Result: "bar"
```

---

## 🛠 Technical Details
* **Unicode Support**: Uses `Normalizer.Form.NFKC` for robust handling of international characters and special spaces.
* **Testing**: Fully covered by JUnit 5 parameterized tests, ensuring reliability across edge cases (nulls, tabs, newlines).