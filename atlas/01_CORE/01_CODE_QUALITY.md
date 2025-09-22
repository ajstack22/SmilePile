# 01: Code Quality Standards

These standards define the rules for writing clean, consistent, and maintainable code. They are enforced during peer review and by automated tooling.

## 1. Structure & Formatting

-   **Style Guide**: The project follows a specific style guide (e.g., Prettier). All code must be formatted before submission. Automated checks will prevent merging unformatted code.
-   **Import Order**: Imports must be ordered consistently in groups:
    1.  React & React Native imports.
    2.  Third-party library imports.
    3.  Internal context or store imports.
    4.  Local component and utility imports.
-   **Component Structure**: Components should follow a consistent internal structure:
    1.  State declarations (`useState`).
    2.  Store hooks (`useStore`).
    3.  Other hooks (`useEffect`, `useCallback`).
    4.  Callback functions.
    5.  The final `return` statement for rendering.

## 2. Naming Conventions

-   **Clarity and Consistency**: Use clear, descriptive names for variables, functions, and components.
-   **Project-Specific Naming**: Adhere to any project-specific naming conventions for data fields (e.g., `activity.text` must be used for the primary text of an activity).
-   **Fallbacks**: When accessing data that may have inconsistent naming, always include fallbacks to ensure stability (e.g., `const text = activity.text || activity.name;`).

## 3. Error Handling

-   **No Generic Errors**: Do not use `throw new Error('Something failed')`. Errors must be structured and provide context.
-   **Structured Errors**: Use custom error classes or objects that include a user-friendly message, an error code, and a flag indicating if the error is recoverable.
-   **Handle All Rejections**: All promises or async operations must have a `.catch()` block or be wrapped in a `try...catch` block.

## 4. Logging

-   **No Production Logs**: `console.log`, `console.warn`, and `console.error` statements are for development only.
-   **Conditional Logging**: All logging must be wrapped in a condition that prevents it from running in a production environment (e.g., `if (process.env.NODE_ENV === 'development')`).
-   **Automated Enforcement**: The deployment script will fail if it detects unwrapped logging statements in the codebase.

## 5. Platform-Specific Code

-   **No Platform-Specific Files**: Do not use `.ios.js`, `.android.js`, or `.web.js` files. This practice leads to code duplication and maintenance issues.
-   **Use `Platform.select()`**: All platform-specific differences must be handled inline within a single component file using the `Platform.select()` API from React Native.

## 6. State Management

-   **Use Store Methods**: Do not update state management stores directly (e.g., `useAppStore.setState(...)`).
-   **Dedicated Actions**: All state changes must be performed by calling dedicated, documented methods or actions within the store itself (e.g., `useUserStore.getState().setUsers(...)`). This ensures that state mutations are predictable and centralized.
