# Contributing to FigwheelMain

First off, thank you for considering contributing to [FigwheelMain](https://github.com/bhauman/figwheel-main)! Your efforts are greatly appreciated and help make this project better for everyone.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Feature Requests](#feature-requests)
  - [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Coding Guidelines](#coding-guidelines)
- [Testing](#testing)
- [Style Guides](#style-guides)
- [Additional Notes](#additional-notes)

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure a welcoming and respectful environment for all contributors.

## How Can I Contribute?

There are many ways to contribute to FigwheelMain, whether it's improving documentation, reporting issues, suggesting new features, or contributing code. Here's how you can get started:

### Reporting Bugs

If you find a bug in FigwheelMain, please open an issue with the following information:

- **A clear and descriptive title**
- **A detailed description** of the problem
- **Steps to reproduce the issue**
- **Expected vs. actual behavior**
- **Environment information** (e.g., OS, version of FigwheelMain, etc.)

Before creating a new issue, please search existing issues to see if someone else has already reported the problem.

### Suggesting Enhancements

Have an idea to make FigwheelMain better? We'd love to hear it! When suggesting enhancements, please:

- Provide a clear and descriptive title
- Describe the enhancement in detail
- Explain the benefit of the enhancement
- Include any relevant examples or use cases

### Feature Requests

Feature requests are also welcome. Follow the same guidelines as [suggesting enhancements](#suggesting-enhancements) to ensure your request is clear and actionable.

### Pull Requests

Contributions in the form of pull requests are highly appreciated. Here's how to proceed:

1. **Fork the repository** to your own GitHub account.
2. **Clone your fork** to your local machine:
   ```bash
   git clone https://github.com/your-username/figwheel-main.git
   ```
3. **Create a new branch** for your feature or bugfix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make your changes** following the [coding guidelines](#coding-guidelines).
5. **Commit your changes** with a clear and descriptive commit message:
   ```bash
   git commit -m "Add feature: your feature description"
   ```
6. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Open a Pull Request** on the main repository, providing a detailed description of your changes and referencing any related issues.

Please ensure your pull request adheres to the requirements outlined in this document.

## Development Setup

The folowing steps are a simple way to set up a local development environment:

1. **Clone the repositories**:
   ```bash
   git clone https://github.com/bhauman/figwheel-core.git
   git clone https://github.com/bhauman/figwheel-repl.git   
   git clone https://github.com/bhauman/figwheel-main.git
   ```
2. **Create a figwheel-main project**
   The easiest way to do this is to use https://github.com/bhauman/figwheel-main-template  

   ```bash
   lein new figwheel-main work-on-figwheel.core -- +deps --react
   ```
   or
   ```bash
   clj -Tclj-new create :template figwheel-main :name work-on-figwheel/work-on-figwheel :args '["+lein", "--react"]'
   ```
3. **Edit deps.edn files so that they are referencing local code**

   Edit `work-on-figwheel.core/deps.edn` so that it references the local figwheel-main:

   Change this entry:
   ```clojure
   com.bhauman/figwheel-main {:mvn/version "0.2.20"}
   ```
   to
   ```clojure
   com.bhauman/figwheel-main {:local/root "../figwheel-main"}
   ```
   
   Edit `figwheel-main/deps.edn` file so that references the local `figwheel-repl` and `figwheel-core`

   Change these entries
   ```clojure
   ;; DEV for now its easier to use cider with top level deps
   ;; com.bhauman/figwheel-core {:local/root "../figwheel-core"}
   ;; com.bhauman/figwheel-repl {:local/root "../figwheel-repl"}
   com.bhauman/figwheel-repl {:mvn/version "0.2.21-SNAPSHOT"}
   com.bhauman/figwheel-core {:mvn/version "0.2.21-SNAPSHOT"}   
   ```
   by uncommenting the DEV entries with `:local/root` in them and commenting out the `:mvn/version` entries like so:
   ```clojure
   ;; DEV for now its easier to use cider with top level deps
   com.bhauman/figwheel-core {:local/root "../figwheel-core"}
   com.bhauman/figwheel-repl {:local/root "../figwheel-repl"}
   ;; com.bhauman/figwheel-repl {:mvn/version "0.2.21-SNAPSHOT"}
   ;; com.bhauman/figwheel-core {:mvn/version "0.2.21-SNAPSHOT"}   
   ```

4. **Run the generated figwheel-main project**

   Check to see that everything is setup correctly by running your
   local figwheel-main project.

   ```bash
   cd work-on-figwheel.core
   clojure -M:fig:build
   ```
   
   The project should be up and building.

   You can now stop the build and make your changes and restart it to see the effects.

   Or you can setup an editor repl into this process and hot reload changes into it.

## Coding Guidelines

To maintain consistency and quality across the project, please adhere to the following coding guidelines:

- **Language**: Primarily Clojure and ClojureScript.
- **Formatting**: Follow [cljfmt](https://github.com/weavejester/cljfmt) styles.
- **Naming Conventions**:
  - Use `kebab-case` for functions and variables.
  - Use `CamelCase` for namespaces, types, and protocols.
- **Code Structure**:
  - Keep functions small and focused.
  - Avoid deep nesting; refactor into smaller functions if necessary.

## Style Guides

Adhering to style guides improves readability and maintainability. Please follow these guidelines:

- **Indentation**: Use 2 spaces per indentation level.
- **Line Length**: Keep lines under 80 characters where possible.
- **Whitespace**: Use blank lines to separate logical blocks of code.

Consider using tools like [cljfmt](https://github.com/weavejester/cljfmt) and for automated formatting and linting.

## Additional Notes

- **Stay Updated**: Regularly sync with the main repository to stay updated on the latest changes.
  ```bash
  git fetch upstream
  git checkout main
  git merge upstream/main
  ```
- **Ask Questions**: If you're unsure about something, feel free to [open an issue](https://github.com/bhauman/figwheel-main/issues) or join the community discussions.
- **Respect Licensing**: Ensure that your contributions comply with the project's licensing terms. FigwheelMain is licensed under the [Eclipse Public License](https://github.com/bhauman/figwheel-main/blob/main/LICENSE).

Thank you again for your interest in contributing to FigwheelMain! 

# License

This project is licensed under the [Eclipse Public License](https://github.com/bhauman/figwheel-main/blob/main/LICENSE).

# Acknowledgements

Special thanks to all the [contributors](https://github.com/bhauman/figwheel-main/graphs/contributors) who have made this project possible.

# Contact

For any queries or support, please reach out via [issues](https://github.com/bhauman/figwheel-main/issues) or join our [community channels](#).

---

*If you have any suggestions for improving this document, feel free to open a pull request or issue.*
