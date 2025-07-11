# OpenAPI Pattern Auditor

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/Version-1.4.1-blue.svg)](https://github.com/your-username/graphix-studio) [![Built with: HTML, JS, TailwindCSS](https://img.shields.io/badge/Built%20with-HTML%2C%20JS%2C%20TailwindCSS-brightgreen.svg)](#technology-stack)

Audits regex patterns in OpenAPI Specification files to ensure cross-engine compatibility and identify quality issues.
![Graphix Studio Screenshot](https://placehold.co/800x450/DBEAFE/3B82F6?text=OpenAPI+Pattern+Auditor)
## Key Features

* **Multi-Engine Validation**: Checks regex pattern syntax against multiple popular engines:
    * Java (java.util.regex)
    * JavaScript (via GraalVM)
    * Go (via Google's RE2J)

* **Quality & Security Checks**: Identifies common issues in your patterns:
    * **Overly Permissive**: Warns against broad patterns like .* that can be insecure.
    * **Missing Anchors**: Detects patterns that lack start (^) and end ($) anchors, which could allow unintended partial matches.
    * **Potential ReDoS**: Flags patterns with nested quantifiers that may be vulnerable to Regular Expression Denial of Service attacks.
* **Comprehensive Scanning**: Traverses the entire OpenAPI document to find regex patterns, including:
    * Component Schemas
    * Path & Operation Parameters
    * Request Bodies
    * API Responses
* **User-Friendly Interface**: A simple web UI to upload your OpenAPI file (JSON or YAML) and view the detailed results.

## How It Works

1.  **Upload**: The user uploads an OpenAPI 3.x specification file through the web interface.
2.  **Parse**: The backend, built with Spring Boot, uses the swagger-parser library to parse the uploaded file.
3.  **Traverse & Extract**: The OasValidationService recursively navigates the OpenAPI model, extracting every pattern field it finds.
4.  **Validate**: For each extracted regex pattern, the service performs two types of validation based on the user's selection:
    * **Syntax Validation**: It runs the pattern against the selected validator components (JavaRegexValidator, JavaScriptRegexValidator, GoRe2jRegexValidator). Each validator uses the native engine or a compatible library to compile the pattern.
    * **Quality Validation**: The PatternQualityValidator inspects the pattern for common quality and security flaws.
5.  **Report**: The results are aggregated and sent back to the user interface, which displays a summary and a detailed table of every check performed.

## Technologies Used

* **Backend**: Spring Boot 3.3, Java 17
* **Frontend**: Thymeleaf, Tailwind CSS
* **OpenAPI Parsing**: io.swagger.parser.v3
* **Regex Engines**:
    * Java: java.util.regex.Pattern
    * JavaScript: org.graalvm.js
    * Go (RE2 Compatible): com.google.re2j
* **Build**: Maven

## How to Run Locally

1.  **Prerequisites**:

    * JDK 17 or later
    * Apache Maven

2.  **Clone the repository**:


bash
git clone https://github.com/abdulwaheed18/openapi-pattern-auditor.git
cd openapi-pattern-auditor


3.  **Build and Run the application**:


bash
mvn spring-boot:run


4.  **Access the application**:
    Open your web browser and navigate to http://localhost:8080.

## Screenshots

*(A screenshot of the upload form would go here, showing the file drop zone and the checkboxes for engine and quality validation.)*

*(A screenshot of the results view would go here, showing the statistics cards and the detailed table with valid, error, and warning statuses.)*

## Author

* **Name**: Abdul Waheed
* **Title**: Senior Software Engineer | API Design Enthusiast
* **GitHub**: [https://github.com/abdulwaheed18](https://github.com/abdulwaheed18)
* **LinkedIn**: [https://www.linkedin.com/in/waheedabdul](https://www.linkedin.com/in/waheedabdul)

-----




