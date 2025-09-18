# Changelog

All notable changes to this project will be documented in this file.

## [1.0.3] - 19/09/2025

### Changed
- Updated Android Gradle Plugin to version 8.13.0
- Updated Kotlin to version 2.2.20
- Updated Gradle to version 9.1.0

## [1.0.2] - 05/08/2025

### Changed
- Updated Android Gradle Plugin to version 8.12.0
- Updated Gradle to version 9.0.0

## [1.0.1] - 15/07/2025

### Changed
- Updated Gradle wrapper to version 8.14.3
- Updated Android Gradle Plugin to version 8.11.1

## [1.0.0] - 01/07/2025

### Added

- Initial release of Http Client library
- Core features:
    - Supported HTTP Methods: GET, POST, PUT, DELETE, HEAD, OPTIONS, and PATCH.
    - Custom SSL Support: Add custom X.509 certificates for secure connections.
    - Request Interceptors: Modify requests (e.g., add headers) before they are sent.
    - Configurable Timeouts: Set read and connect timeouts for requests.
    - Redirect Handling: Optionally follow HTTP redirects.
    - Type-Safe API: Dedicated request classes for each HTTP method.
    - Error Handling: Comprehensive exception handling for network and HTTP errors.
- Documentation:
    - README with usage instructions
    - VERSIONING policy
    - CHANGELOG template
